package space.nuvio.nativelegacy

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StreamCardTextTest {
    private val renderer = StreamCardText(RuntimeEnvironment.getApplication())
    private var index = 0

    private fun row(fields: String, width: Int = 420) =
        renderer.layout(1, index++, width, fields.toByteArray())!!

    private fun pixels(row: StreamCardParagraphs) = row.tile(0, row.height)

    @Test fun nullFallbackAndDuplicateSuppression() {
        val titleOnly = row("""["Release",null,null]""")
        assertArrayEquals(pixels(titleOnly), pixels(row("""[null,"Release",null]""")))
        assertArrayEquals(pixels(titleOnly), pixels(row("""[null,null,"Release"]""")))
        assertArrayEquals(pixels(titleOnly), pixels(row("""["Release",null,"Release"]""")))
        assertArrayEquals(pixels(titleOnly), pixels(row("""["Release","Ignored","  "]""")))
        assertFalse(pixels(row("""["","Release",""]""")).any { it ushr 24 != 0 })
    }

    @Test fun colorEmojiAndJsonSurrogates() {
        val literal = pixels(row("""["🔥",null,null]"""))
        val escaped = pixels(row("""["\uD83D\uDD25",null,null]"""))
        assertArrayEquals(literal, escaped)
        assertTrue("expected colored emoji pixels", literal.any { pixel ->
            val red = pixel and 255
            val blue = (pixel ushr 16) and 255
            pixel ushr 24 > 128 && red - blue > 40
        })
    }

    @Test fun backgroundLayoutAndTileAreNonblockingAndPixelIdentical() {
        val fields = JSONArray(listOf(
            "[TB⚡] Addon", null, "🎬 UHD Dolby Vision\\nMalayalam.2160p.mkv"
        )).toString().toByteArray(Charsets.UTF_8)
        // Different generation/index guarantees this is not a warm sync cache.
        val first = renderer.layoutAsync(94, 301, 370, fields)
        assertNull("raster work should be queued instead of running on GL", first)
        var prepared: StreamCardParagraphs? = null
        var attempts = 0
        while (prepared == null && attempts++ < 300) {
            Thread.sleep(10)
            prepared = renderer.layoutAsync(94, 301, 370, null)
        }
        val ready = requireNotNull(prepared) { "background layout never completed" }
        assertTrue(ready.height > 0)
        val pixelsExpected = ready.tile(0, ready.height)
        assertTrue(pixelsExpected.any { it ushr 24 != 0 })
        assertNull("tile painting should also start asynchronously",
            ready.tileAsync(0, ready.height))
        var actual: IntArray? = null
        attempts = 0
        while (actual == null && attempts++ < 300) {
            Thread.sleep(10)
            actual = ready.tileAsync(0, ready.height)
        }
        assertNotNull("background painting never completed", actual)
        assertArrayEquals("background painting must preserve emoji and exact pixels",
            pixelsExpected, actual)
    }

    @Test fun multilineAndTiledRenderingMatchAtBothWidths() {
        val filename = "Bullet.Train." + "2160p.".repeat(42) + "മലയാളം.mkv"
        fun payload(separator: String) = JSONArray(listOf("[TB⚡] ElfCache", "unused",
            "👩🏽‍💻  Malayalam" + separator + "📁\n" + filename)).toString()
        val wide = row(payload("\n\n"))
        assertTrue(wide.height > row(payload("\n")).height)
        val narrow = row(payload("\n\n"), 260)
        assertTrue(narrow.height > wide.height)
        val first = narrow.height / 2
        assertArrayEquals(pixels(narrow), narrow.tile(0, first) + narrow.tile(first, narrow.height - first))
        assertTrue(pixels(narrow).any { it ushr 24 != 0 })
    }
}
