package space.nuvio.nativelegacy

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
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
import java.net.ServerSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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

    @Test fun localHttpRangeServerFeedsActualMedia3DataSource() {
        // java.net.ServerSocket is available on the Android/JVM test classpath;
        // com.sun.net.httpserver.HttpServer is not exposed by AGP's bootclasspath.
        val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 10_000
        val served = AtomicInteger()
        val worker = Executors.newSingleThreadExecutor()
        val task = worker.submit {
            try {
                server.accept().use { client ->
                    client.soTimeout = 10_000
                    val input = client.getInputStream().bufferedReader()
                    var line = input.readLine()
                    var range: String? = null
                    while (line != null && line.isNotEmpty()) {
                        if (line.startsWith("Range:", ignoreCase = true))
                            range = line.substringAfter(':').trim()
                        line = input.readLine()
                    }
                    val match = Regex("^bytes=([0-9]+)-([0-9]*)$").matchEntire(range.orEmpty())
                    val start = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val last = match?.groupValues?.get(2)?.toIntOrNull() ?: (bodies.size - 1)
                    val end = minOf(last, bodies.size - 1)
                    val count = end - start + 1
                    require(start in bodies.indices && count > 0 && match != null)
                    val header = "HTTP/1.1 206 Partial Content\r\n" +
                        "ETag: \"loopback\"\r\nAccept-Ranges: bytes\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Range: bytes " + start + "-" + end + "/" + bodies.size + "\r\n" +
                        "Content-Length: " + count + "\r\nConnection: close\r\n\r\n"
                    client.getOutputStream().use {
                        it.write(header.toByteArray(Charsets.US_ASCII))
                        it.write(bodies, start, count)
                        it.flush()
                    }
                    served.addAndGet(count)
                }
            } catch (_: SocketException) {
                // Server closed by @Test finally after the bounded request.
            }
        }
        try {
            val uri = "http://127.0.0.1:" + server.localPort + "/vod"
            val cache = create(uri, bodies.size)
            val d = cache.fabrica(DefaultHttpDataSource.Factory()).createDataSource()
            val start = 8192
            val length = 16384
            val out = ByteArrayOutputStream()
            try {
                d.open(DataSpec.Builder().setUri(Uri.parse(uri))
                    .setPosition(start.toLong()).setLength(length.toLong()).build())
                val buf = ByteArray(4096)
                while (true) {
                    val n = d.read(buf, 0, buf.size)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
            } finally { d.close() }
            task.get(10, TimeUnit.SECONDS)
            assertArrayEquals(bodies.copyOfRange(start, start + length), out.toByteArray())
            assertEquals("real HTTP range must transmit only requested bytes", length, served.get())
            cache.liberar()
        } finally {
            server.close()
            worker.shutdownNow()
        }
    }

    @Test fun evictionWhileReaderIsOpenCannotReturnWrongByteSequence() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        assertArrayEquals(bodies, read(cache, origin, primary))
        val field = CacheMidia::class.java.getDeclaredField("cache")
        field.isAccessible = true
        val simple = field.get(cache) as SimpleCache
        val span = simple.getCachedSpans(simple.keys.first()).first()
        val d = cache.fabrica(origin).createDataSource()
        val out = ByteArrayOutputStream()
        val pool = Executors.newSingleThreadExecutor()
        try {
            d.open(DataSpec.Builder().setUri(Uri.parse(primary)).setLength(bodies.size.toLong()).build())
            val chunk = ByteArray(4096)
            val first = d.read(chunk, 0, chunk.size)
            assertEquals(chunk.size, first)
            out.write(chunk, 0, first)
            // Evict the underlying span from another thread while its reader is open.
            pool.submit { simple.removeSpan(span) }.get(5, TimeUnit.SECONDS)
            while (true) {
                val n = d.read(chunk, 0, chunk.size)
                if (n < 0) break
                out.write(chunk, 0, n)
            }
            assertArrayEquals("active reader must never return mixed/wrong bytes", bodies, out.toByteArray())
        } finally {
            d.close()
            pool.shutdownNow()
            cache.liberar()
        }
    }

    @Test fun nextEpisodeProofCannotAdoptPreviousMovieCache() {
        val origin = Origin(bodies)
        val cache = create(primary, bodies.size)
        assertArrayEquals(bodies, read(cache, origin, primary))
        CacheMidia.guardar(cache)
        val (next, status) = CacheMidia.criar(folder, 256, {},
            "account-profile-headers", "different-episode-proof",
            "catalog:season-2:episode-3", rotated, bodies.size.toLong())
        assertEquals(CacheSessao.ATIVO, status)
        assertNotSame("next episode must have independent byte ownership", cache, next)
        next?.liberar()
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
