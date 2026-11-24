package space.nuvio.nativelegacy

import java.nio.ByteBuffer

// Original 64x64 black test streams generated with libx265, not mock SPS
// values. ffprobe independently confirmed Main10 BT.2020 PQ / HLG and
// Main 8-bit BT.709 SDR. Annex-B VPS/SPS/PPS and the PQ IRAP are retained.
private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private val pq = hex("0000000140010c01ffff02200000030090000003000003001e9598090000000142010102200000030090000003000003001ea0208104d96566924caf016a12201208000003000800000300c040000000014401c172b42240")
private val hlg = hex("0000000140010c01ffff02200000030090000003000003001e9598090000000142010102200000030090000003000003001ea0208104d96566924caf016a12241208000003000800000300c040000000014401c172b42240")
private val sdr = hex("0000000140010c01ffff01600000030090000003000003001e9598090000000142010101600000030090000003000003001ea020810596566924caf016a020202080000003008000000c04000000014401c172b42240")
private val irap = hex("000000012801af1d80f7015bfff24efeccf0")

fun main() {
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
    val proof = HdrCodecFallbackP5B.Proof()
    check(!proof.ready())
    proof.sample = true
    check(!proof.ready())
    proof.hdrOutput = true
    check(proof.ready())
    proof.valid = false
    check(!proof.ready())
    println("P5-B actual HEVC CSD/IRAP and codec proof: PASS (23 assertions)")
}
