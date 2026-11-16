package space.nuvio.nativelegacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import kotlin.math.roundToInt

// Called only by the SDL render thread. The same Compose paragraphs supply
// both row height and pixels. Native code continues to own clipping and focus.
internal class StreamCardText(private val context: Context) {
    private val resolver = createFontFamilyResolver(context)
    private val family = FontFamily(
        Font(R.font.selector_inter, FontWeight.Normal),
        Font(R.font.selector_inter, FontWeight.Medium)
    )
    private data class Key(val generation: Int, val index: Int, val width: Int)
    private val cache = LruCache<Key, StreamCardParagraphs>(12)

    fun layout(generation: Int, index: Int, width: Int, payload: ByteArray?): StreamCardParagraphs? {
        val key = Key(generation, index, width)
        cache.get(key)?.let { return it }
        if (payload == null || width <= 0) return null
        val values = JSONArray(payload.toString(Charsets.UTF_8))
        fun field(i: Int) = if (values.isNull(i)) null else values.getString(i)
        val title = field(0) ?: field(1) ?: field(2) ?: context.getString(R.string.stream_unknown)
        val description = (field(2) ?: field(1))?.takeIf { it.isNotBlank() && it != title }
        // The SDL clock uses 32 scene units for an official 20dp badge.
        val density = Density(1.6f, context.resources.configuration.fontScale)
        val measurer = TextMeasurer(resolver, density, LayoutDirection.Ltr, cacheSize = 0)
        fun measure(value: String, heading: Boolean): TextLayoutResult {
            val style = TextStyle(
                fontFamily = family,
                fontWeight = if (heading) FontWeight.Medium else FontWeight.Normal,
                fontSize = if (heading) 16.sp else 12.sp,
                lineHeight = if (heading) 24.sp else 16.sp,
                letterSpacing = if (heading) 0.15.sp else 0.4.sp,
                textDirection = contentDirection(value)
            )
            return measurer.measure(AnnotatedString(value), style,
                constraints = Constraints.fixedWidth(width))
        }
        return StreamCardParagraphs(width, measure(title, true), description?.let { measure(it, false) },
            (4 * density.density).roundToInt()).also { cache.put(key, it) }
    }

    private fun contentDirection(text: String): TextDirection {
        var offset = 0
        while (offset < text.length) {
            val cp = text.codePointAt(offset)
            offset += Character.charCount(cp)
            if (cp in 0x1F1E6..0x1F1FF || cp in 0x1F300..0x1F9FF ||
                cp in 0x2600..0x27BF || cp in 0x1F000..0x1F02F ||
                cp in 0x0300..0x036F || cp == 0xFE0F || cp == 0x200D) continue
            when (Character.getDirectionality(cp)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return TextDirection.Rtl
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return TextDirection.Ltr
            }
        }
        return TextDirection.Ltr
    }
}

class StreamCardParagraphs internal constructor(
    private val width: Int,
    private val title: TextLayoutResult,
    private val description: TextLayoutResult?,
    private val gap: Int
) {
    val height: Int = title.size.height + (description?.let { gap + it.size.height } ?: 0)

    // Return straight-alpha RGBA bytes for Legacy's GL_SRC_ALPHA blend.
    // Bitmap.getPixels unpremultiplies, unlike direct AndroidBitmap pixel access.
    fun tile(top: Int, rows: Int): IntArray {
        val bitmap = Bitmap.createBitmap(width, rows, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.translate(0f, -top.toFloat())
            val composeCanvas = ComposeCanvas(canvas)
            title.multiParagraph.paint(composeCanvas, Color.White)
            description?.let {
                canvas.translate(0f, (title.size.height + gap).toFloat())
                it.multiParagraph.paint(composeCanvas, Color(0xffb3b3b3))
            }
            val pixels = IntArray(width * rows)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, rows)
            for (i in pixels.indices) {
                val p = pixels[i]
                pixels[i] = (p and 0xff00ff00.toInt()) or
                    ((p ushr 16) and 255) or ((p and 255) shl 16)
            }
            return pixels
        } finally {
            bitmap.recycle()
        }
    }
}
