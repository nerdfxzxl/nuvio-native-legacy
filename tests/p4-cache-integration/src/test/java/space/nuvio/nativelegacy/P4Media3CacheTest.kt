package space.nuvio.nativelegacy

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.SimpleCache
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/** Real production Media3 SimpleCache/CacheDataSource; fake HTTPS origin serves exact 206 byte ranges. */
@RunWith(RobolectricTestRunner::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class P4Media3CacheTest {
    private lateinit var folder: File
    private val bodies = ByteArray(128 * 1024) { (it * 31 + 7).toByte() }
    private val primary = "https://cdn.example.invalid/media/file.mkv?sig=one"
    private val rotated = "https://cdn.example.invalid/media/file.mkv?sig=two"

    @Before fun before() {
        folder = Files.createTempDirectory("p4-real-media3").toFile()
        CacheMidia.descartar("p4 fixture reset")
    }
    @After fun after() {
        CacheMidia.descartar("p4 fixture teardown")
    }

    private class Origin(var body: ByteArray, var etag: String = "\"stable\"") : DataSource.Factory {
        val requests = AtomicInteger()
        val transferred = AtomicInteger()
        @Volatile var failOpen = false
        override fun createDataSource(): DataSource = object : DataSource {
            private var bytes = ByteArray(0)
            private var cursor = 0
            private var limit = 0
            private var activeSpec: DataSpec? = null
            private var remote: Uri? = null
            private var headers = emptyMap<String, List<String>>()
            private val listeners = ArrayList<TransferListener>()

            override fun open(dataSpec: DataSpec): Long {
                if (failOpen) throw IOException("source unavailable")
                val current = body.copyOf()
                val start = dataSpec.position.toInt()
                require(start in 0 until current.size) { "invalid range position" }
                val requested = if (dataSpec.length < 0L) current.size - start
                    else minOf(dataSpec.length, (current.size - start).toLong()).toInt()
                require(requested > 0)
                requests.incrementAndGet()
                bytes = current
                cursor = start
                limit = start + requested
                activeSpec = dataSpec
                remote = dataSpec.uri
                headers = mapOf(
                    "ETag" to listOf(etag),
                    "Content-Range" to listOf("bytes " + start + "-" + (limit - 1) + "/" + current.size),
                    "Content-Length" to listOf(requested.toString()),
                    "Content-Encoding" to listOf("identity")
                )
                for (l in listeners) l.onTransferInitializing(this, dataSpec, true)
                for (l in listeners) l.onTransferStart(this, dataSpec, true)
                return requested.toLong()
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (length == 0) return 0
                if (cursor >= limit) return -1
                val n = minOf(length, limit - cursor)
                System.arraycopy(bytes, cursor, buffer, offset, n)
                cursor += n
                transferred.addAndGet(n)
                activeSpec?.let { s -> for (l in listeners) l.onBytesTransferred(this, s, true, n) }
                return n
            }
            override fun getUri(): Uri? = remote
            override fun getResponseHeaders(): Map<String, List<String>> = headers
            override fun addTransferListener(transferListener: TransferListener) {
                listeners.add(transferListener)
            }
            override fun close() {
                activeSpec?.let { s -> for (l in listeners) l.onTransferEnd(this, s, true) }
                activeSpec = null
            }
        }
    }

    private fun create(url: String, length: Int): CacheMidia {
        val (c, status) = CacheMidia.criar(folder, 256, {}, "account-profile-headers",
            "immutable-release-proof", "catalog:movie-1", url, length.toLong())
        assertEquals(CacheSessao.ATIVO, status)
        return requireNotNull(c)
    }

    private fun read(cache: CacheMidia, origin: Origin, url: String,
                     position: Int = 0, length: Int = bodies.size,
                     canceled: () -> Boolean = { false }): ByteArray {
        val source = cache.fabrica(origin, canceled).createDataSource()
        val spec = DataSpec.Builder().setUri(Uri.parse(url))
            .setPosition(position.toLong()).setLength(length.toLong()).build()
        val out = ByteArrayOutputStream()
        try {
            source.open(spec)
            val bytes = ByteArray(4096)
            while (true) {
                val n = source.read(bytes, 0, bytes.size)
                if (n < 0) break
                out.write(bytes, 0, n)
            }
        } finally { source.close() }
        return out.toByteArray()
    }

    @Test fun realCacheWritesAndSafeReads() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        assertArrayEquals(bodies, read(cache, origin, primary))
        val spent = origin.transferred.get()
        assertArrayEquals(bodies, read(cache, origin, primary))
        assertTrue("repeat read should not redownload entire file",
            origin.transferred.get() - spent < bodies.size / 8)
        cache.liberar()
    }

    @Test fun verifiedSignedUrlRotationAndValidatorConflict() {
        val origin = Origin(bodies)
        val first = create(primary, bodies.size)
        assertArrayEquals(bodies, read(first, origin, primary))
        CacheMidia.guardar(first)
        val reused = create(rotated, bodies.size)
        assertSame("retain proven SimpleCache", first, reused)
        val before = origin.transferred.get()
        assertArrayEquals(bodies, read(reused, origin, rotated))
        assertTrue("rotation should fetch only bounded proof",
            origin.transferred.get() - before < bodies.size / 8)
        CacheMidia.guardar(reused)

        origin.etag = "\"changed\""
        val changed = bodies.clone()
        changed[4000] = (changed[4000] + 1).toByte()
        origin.body = changed
        val third = create(primary, changed.size)
        assertSame(reused, third)
        assertArrayEquals("changed validator cannot serve stale cached bytes",
            changed, read(third, origin, primary))
        third.liberar()
    }

    @Test fun corruptDiskBytesAreRejectedBeforeDecoderReads() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        assertArrayEquals(bodies, read(cache, origin, primary))
        val field = CacheMidia::class.java.getDeclaredField("cache")
        field.isAccessible = true
        val simple = field.get(cache) as SimpleCache
        val span = simple.getCachedSpans(simple.keys.first()).first()
        val file = requireNotNull(span.file)
        val corrupt = file.readBytes()
        corrupt[corrupt.size / 2] = (corrupt[corrupt.size / 2] + 1).toByte()
        file.writeBytes(corrupt)
        assertArrayEquals("SHA mismatch must force fresh, correct network bytes",
            bodies, read(cache, origin, primary))
        cache.liberar()
    }

    @Test fun partialDiskSpanAndUncachedHoleKeepExactByteRange() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        val half = bodies.size / 2
        assertArrayEquals(bodies.copyOf(half), read(cache, origin, primary, 0, half))
        val networkBefore = origin.transferred.get()
        assertArrayEquals(bodies, read(cache, origin, primary))
        val extra = origin.transferred.get() - networkBefore
        assertTrue("only missing HTTP range + one-byte validator should be downloaded",
            extra >= half && extra < bodies.size)
        cache.liberar()
    }

    @Test fun changedRemoteRepresentationAfterPartialCacheNeverMixesBytes() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        val half = bodies.size / 2
        assertArrayEquals(bodies.copyOf(half), read(cache, origin, primary, 0, half))
        val changed = bodies.clone()
        changed[42] = (changed[42] + 1).toByte()
        changed[half + 42] = (changed[half + 42] + 1).toByte()
        origin.etag = "\"changed\""
        origin.body = changed
        assertArrayEquals("no old cached prefix may be mixed with changed HTTP hole",
            changed, read(cache, origin, primary))
        cache.liberar()
    }

    @Test fun accountSwitchRevokesProvenRetainedCache() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        assertArrayEquals(bodies, read(cache, origin, primary))
        CacheMidia.guardar(cache)
        CacheMidia.descartar("account switch")
        val next = create(primary, bodies.size)
        assertNotSame("old account cache must not carry over", cache, next)
        next.liberar()
    }

    @Test fun cancellationBeforeAndDuringReadFailsClosed() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        var canceled = true
        val source = cache.fabrica(origin) { canceled }.createDataSource()
        val spec = DataSpec.Builder().setUri(Uri.parse(primary))
            .setLength(bodies.size.toLong()).build()
        try {
            source.open(spec)
            fail("cancelled open must reject")
        } catch (_: InterruptedIOException) {
            assertEquals(0, origin.requests.get())
        } finally { source.close() }

        canceled = false
        val opened = cache.fabrica(origin) { canceled }.createDataSource()
        try {
            opened.open(spec)
            canceled = true
            try {
                opened.read(ByteArray(1024), 0, 1024)
                fail("cancelled loader must not consume bytes")
            } catch (_: InterruptedIOException) { }
        } finally { opened.close() }
        cache.liberar()
    }
}
