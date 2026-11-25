package space.nuvio.nativelegacy

import java.nio.ByteBuffer
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes

// Original 64x64 black test streams generated with libx265, not mock SPS
// values. ffprobe independently confirmed Main10 BT.2020 PQ / HLG and
// Main 8-bit BT.709 SDR. Annex-B VPS/SPS/PPS and the PQ IRAP are retained.
private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private val pq = hex("0000000140010c01ffff02200000030090000003000003001e9598090000000142010102200000030090000003000003001ea0208104d96566924caf016a12201208000003000800000300c040000000014401c172b42240")
private val hlg = hex("0000000140010c01ffff02200000030090000003000003001e9598090000000142010102200000030090000003000003001ea0208104d96566924caf016a12241208000003000800000300c040000000014401c172b42240")
private val sdr = hex("0000000140010c01ffff01600000030090000003000003001e9598090000000142010101600000030090000003000003001ea020810596566924caf016a020202080000003008000000c04000000014401c172b42240")
private val irap = hex("000000012801af1d80f7015bfff24efeccf0")

fun main(args: Array<String>) {
    fun inspect(data: ByteArray, codecs: String = "dvhe.08.06", width: Int = 64) =
        HdrBaseLayerP5B.inspect(codecs, width, 64, listOf(data))
    val p = HdrBaseLayerP5B
    check(inspect(pq)?.profile?.generalProfileIdc == 2)
    check(inspect(pq, "dvhe.07.06") != null)
    check(inspect(pq, "dvhe.05.06") == null)
    check(inspect(hlg) == null)
    check(inspect(sdr) == null)
    check(inspect(pq, width = 128) == null)
    check(inspect(byteArrayOf()) == null)
    check(inspect(pq.copyOf(10)) == null)
    check(inspect(pq.copyOf(pq.size - 11)) == null) // no PPS
    check(inspect(pq.copyOf().apply { this[6] = 0 }) == null) // no internal/available BL
    check(inspect(pq.copyOf().apply {
        // Change the layer id of the actual SPS header to one.
        val pos = indices.first { it + 5 < size && this[it] == 0.toByte() &&
            this[it + 1] == 0.toByte() && this[it + 2] == 0.toByte() &&
            this[it + 3] == 1.toByte() && this[it + 4] == 0x42.toByte() }
        this[pos + 5] = 9
    }) == null)
    val input = ByteBuffer.allocateDirect(irap.size + 8)
    input.position(4); input.put(irap)
    val before = input.position()
    check(p.hasBaseRandomAccess(input, 4, irap.size))
    check(input.position() == before)
    check(!p.hasBaseRandomAccess(input, -1, irap.size))
    check(!p.hasBaseRandomAccess(input, 4, input.capacity()))
    check(!p.hasBaseRandomAccess(ByteBuffer.wrap(irap.copyOf(5)), 0, 5))
    check(!p.hasBaseRandomAccess(ByteBuffer.wrap(irap.copyOf().apply { this[5] = 9 }), 0, irap.size))
    check(!p.hasBaseRandomAccess(ByteBuffer.wrap(irap.copyOf().apply { this[4] = 0x7c }), 0, irap.size))
    check(!p.hasBaseRandomAccess(ByteBuffer.wrap(irap.copyOf().apply { this[6] = 0x2f }), 0, irap.size))
    val el = hex("0000012809af01")
    val rpu = hex("000000017c010101")
    val mixed = pq + el + irap + rpu
    val adapted = ByteBuffer.allocateDirect(mixed.size + 8)
    adapted.position(3); adapted.put(mixed)
    val length = p.keepBaseUnits(adapted, 3, mixed.size)
    val bytes = ByteArray(length)
    adapted.position(3); adapted.get(bytes)
    check(bytes.contentEquals(pq + irap))
    check(p.keepBaseUnits(ByteBuffer.wrap(el), 0, el.size) == 0)
    if (args.isNotEmpty()) java.io.File(args.single()).writeBytes(bytes)
    val proof = HdrCodecFallbackP5B.Proof()
    check(!proof.ready())
    proof.sample = true
    check(!proof.ready())
    proof.hdrOutput = true
    check(proof.ready())
    proof.valid = false
    check(!proof.ready())
    // The actual Media3 Format (not a simulated renderer) is the identity
    // guard for one runtime decoder recovery.
    val original = Format.Builder().setId("selected").setSampleMimeType(MimeTypes.VIDEO_DOLBY_VISION)
        .setCodecs("dvhe.08.06").setWidth(64).setHeight(64).setFrameRate(24f)
        .setAverageBitrate(2000000).setPeakBitrate(3000000).setInitializationData(listOf(pq)).build()
    val recovery = HdrCodecFallbackP5B.RuntimeRecovery("failed.hw", original)
    val platform = HdrCodecFallbackP5B.RuntimeRecovery.after("codec1", original, true)
    check(platform.permits("codec1", original) && "codec1" in platform.platformOnly)
    val next = platform.next("codec1", original, false)!!
    check(!next.permits("codec1", original))
    check(next.next("codec1", original, false) == null)
    check(next.next("codec2", original.buildUpon().setWidth(32).build(), false) == null)
    val last = next.next("codec2", original, false)!!.next("codec3", original, false)!!.next("codec4", original, false)!!
    check(last.next("codec5", original, false) == null)
    check(!recovery.completed)
    check(recovery.sameRepresentation(original))
    check(recovery.permits("compatible.sw", original.buildUpon().build()))
    check(!recovery.permits("failed.hw", original))
    check(!recovery.permits("compatible.sw", original.buildUpon().setWidth(32).build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setHeight(32).build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setFrameRate(30f).build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setAverageBitrate(1000000).build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setPeakBitrate(1000000).build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setId("other").build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setCodecs("dvhe.05.06").build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setSampleMimeType(MimeTypes.VIDEO_H265).build()))
    check(!recovery.permits("compatible.sw", original.buildUpon().setInitializationData(listOf(hlg)).build()))
    check(!recovery.handoff(null, original, "compatible.sw"))
    check(!recovery.completed)
    // Actual format objects: unchanged metadata can reuse a decoder;
    // changed DV or software-decoded formats must repeat the capability gate.
    val sdrFormat = original.buildUpon().setSampleMimeType(MimeTypes.VIDEO_H265).setCodecs("hvc1.2.4.L93").build()
    val changedSdr = sdrFormat.buildUpon().setWidth(128).build()
    val changedDv = original.buildUpon().setCodecs("dvhe.05.06").build()
    check(!HdrCodecFallbackP5B.needsRevalidation(original, original.buildUpon().build(), false))
    check(HdrCodecFallbackP5B.needsRevalidation(original, changedDv, false))
    check(HdrCodecFallbackP5B.needsRevalidation(original, original.buildUpon().setFrameRate(30f).build(), false))
    check(HdrCodecFallbackP5B.needsRevalidation(original, original.buildUpon().setInitializationData(listOf(hlg)).build(), false))
    check(HdrCodecFallbackP5B.needsRevalidation(original, sdrFormat, false))
    check(HdrCodecFallbackP5B.needsRevalidation(sdrFormat, original, false))
    check(!HdrCodecFallbackP5B.needsRevalidation(sdrFormat, changedSdr, false))
    check(HdrCodecFallbackP5B.needsRevalidation(sdrFormat, changedSdr, true))
    check(!HdrCodecFallbackP5B.needsRevalidation(sdrFormat, sdrFormat.buildUpon().build(), true))
    println("P5-B actual HEVC CSD/IRAP, codec proof, runtime and reuse guards: PASS including enhancement/RPU removal")
}
