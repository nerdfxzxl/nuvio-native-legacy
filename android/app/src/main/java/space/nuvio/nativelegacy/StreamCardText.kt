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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// One bounded background worker handles expensive paragraph shaping and
// offscreen bitmap rasterization; no Bitmap/Compose painting on the SDL GL
// thread. Bounded queue prevents long scrolls from hoarding old card jobs.
private object StreamCardRenderWorker {
    val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue<Runnable>(24),
        ThreadFactory { task -> Thread(task, "nuvio-card-paint").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )
}

// Called by the SDL render thread. The same Compose paragraphs supply
// both row height and pixels. Native code continues to own clipping and focus.
internal class StreamCardText(private val context: Context) {
    private val resolver = createFontFamilyResolver(context)
    private val family = FontFamily(
        Font(R.font.selector_inter, FontWeight.Normal),
        Font(R.font.selector_inter, FontWeight.Medium)
    )
    private data class Key(val generation: Int, val index: Int, val width: Int)
    // Reuse the Compose text measurer instead of recreating font resolution
    // and shaping infrastructure for every freshly visible stream.
    private val density = Density(1.6f, context.resources.configuration.fontScale)
    private val measurer = TextMeasurer(resolver, density, LayoutDirection.Ltr, cacheSize = 24)
    private val cache = LruCache<Key, StreamCardParagraphs>(24)
    private val queuedLayouts = ConcurrentHashMap<Key, Future<StreamCardParagraphs?>>()

    fun layout(generation: Int, index: Int, width: Int, payload: ByteArray?): StreamCardParagraphs? {
        val key = Key(generation, index, width)
        cache.get(key)?.let { return it }
        if (payload == null || width <= 0) return null
        val values = JSONArray(payload.toString(Charsets.UTF_8))
        fun field(i: Int) = if (values.isNull(i)) null else values.getString(i)
        val title = field(0) ?: field(1) ?: field(2) ?: context.getString(R.string.stream_unknown)
        val description = (field(2) ?: field(1))?.takeIf { it.isNotBlank() && it != title }
        // The SDL clock uses 32 scene units for an official 20dp badge.
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

    // Nonblocking entry point for the native render thread. A layout request
    // with payload starts one worker job. Further polls use a null payload and
    // read only completed results. The synchronous layout() is retained for
    // unit tests and never used from the production SDL frame loop.
    fun layoutAsync(generation: Int, index: Int, width: Int, payload: ByteArray?): StreamCardParagraphs? {
        val key = Key(generation, index, width)
        cache.get(key)?.let { return it }
        queuedLayouts[key]?.let { task ->
            if (!task.isDone) return null
            queuedLayouts.remove(key, task)
            return try { task.get() } catch (_: Exception) { null }
        }
        if (payload == null || width <= 0) return null
        try {
            val copy = payload.copyOf()
            queuedLayouts[key] = StreamCardRenderWorker.executor.submit<StreamCardParagraphs?> {
                layout(generation, index, width, copy)
            }
        } catch (_: RejectedExecutionException) {
            // Render thread retries with payload after a short timeout.
        }
        return null
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
    private val queuedTiles = ConcurrentHashMap<Long, Future<IntArray>>()
    val height: Int = title.size.height + (description?.let { gap + it.size.height } ?: 0)

    // Preparing the bitmap is CPU work and must not block VSync. The first
    // call schedules painting; the next completed call returns pixels for a
    // bounded OpenGL texture upload. No Java array is retained after delivery.
    fun tileAsync(top: Int, rows: Int): IntArray? {
        if (rows <= 0 || top < 0 || top + rows > height) return null
        val key = (top.toLong() shl 32) or rows.toLong()
        queuedTiles[key]?.let { task ->
            if (!task.isDone) return null
            queuedTiles.remove(key, task)
            return try { task.get() } catch (_: Exception) { null }
        }
        try {
            queuedTiles[key] = StreamCardRenderWorker.executor.submit<IntArray> { tile(top, rows) }
        } catch (_: RejectedExecutionException) {
            // The worker is busy painting visible tiles; retry next frame.
        }
        return null
    }

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
                // Improve legibility on the exact black card background.
                it.multiParagraph.paint(composeCanvas, Color(0xffd0d0d0))
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
