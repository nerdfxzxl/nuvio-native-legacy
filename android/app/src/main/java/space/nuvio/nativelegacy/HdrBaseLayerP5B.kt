package space.nuvio.nativelegacy

import androidx.media3.common.C
import androidx.media3.common.util.CodecSpecificDataUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.NalUnitUtil
import java.nio.ByteBuffer

/** Q37: independent, conservative evidence from the actual HEVC initialization
 * data and submitted access unit, not the Dolby profile or container PQ label. */
@androidx.annotation.OptIn(UnstableApi::class)
internal object HdrBaseLayerP5B {
    private const val MAX_SCAN = 256 * 1024
    data class Evidence(val profile: NalUnitUtil.H265ProfileTierLevel)

    private fun baseType(first: Int, second: Int, type: Int): Boolean =
        first and 0x81 == 0 && second and 0xf8 == 0 &&
            second and 7 != 0 && (first shr 1) and 63 == type

    private fun baseNal(n: ByteArray, type: Int): Boolean =
        n.size >= 7 && baseType(n[4].toInt() and 255, n[5].toInt() and 255, type)

    fun inspect(codecs: String?, width: Int, height: Int,
                initializationData: List<ByteArray>): Evidence? {
        val parts = codecs?.split('.') ?: return null
        if (parts.size != 3 || parts[0] !in listOf("dvhe", "dvh1") ||
            parts[1].toIntOrNull() !in listOf(7, 8) ||
            (parts[2].toIntOrNull() ?: 0) <= 0 || width <= 0 || height <= 0) return null
        if (initializationData.isEmpty() ||
            initializationData.sumOf { it.size.toLong() } > MAX_SCAN) return null
        return try {
            val units = initializationData.flatMap {
                CodecSpecificDataUtil.splitNalUnits(it)?.asList() ?: return null
            }
            if (units.size > 64) return null
            // VPS must positively declare an internal, available base layer.
            val vps = units.filter { baseNal(it, 32) }
            if (vps.isEmpty() || vps.any { it[6].toInt() and 0x0c != 0x0c } ||
                units.none { baseNal(it, 34) }) return null
            val sps = units.filter { baseNal(it, 33) }
            if (sps.isEmpty()) return null
            val parsed = sps.map { NalUnitUtil.parseH265SpsNalUnit(it, 4, it.size, null) }
            if (parsed.any {
                    it.nalHeader.layerId != 0 || it.chromaFormatIdc != 1 ||
                        it.bitDepthLumaMinus8 != 2 || it.bitDepthChromaMinus8 != 2 ||
                        it.width != width || it.height != height ||
                        it.colorSpace != C.COLOR_SPACE_BT2020 ||
                        it.colorTransfer != C.COLOR_TRANSFER_ST2084 ||
                        it.colorRange != C.COLOR_RANGE_LIMITED ||
                        it.profileTierLevel?.generalProfileIdc != 2 ||
                        it.profileTierLevel?.generalProfileSpace != 0
                }) return null
            val highest = parsed.mapNotNull { it.profileTierLevel }
                .maxWithOrNull(compareBy<NalUnitUtil.H265ProfileTierLevel> { it.generalTierFlag }
                    .thenBy { it.generalLevelIdc }) ?: return null
            Evidence(highest)
        } catch (_: Exception) { null }
    }

    // Extractors have converted clear HEVC samples to Annex B. Keep layer-0
    // coded pictures, parameter sets and SEI (including static/dynamic HDR).
    // Strip enhancement-layer and Dolby RPU/EL units only for a verified
    // alternate. The original sample in the media source remains untouched.
    fun keepBaseUnits(buffer: ByteBuffer, offset: Int, size: Int): Int {
        if (size < 5) return size
        require(offset >= 0 && offset <= buffer.capacity() - size)
        val src = buffer.duplicate().apply { clear() }
        val dst = src.duplicate()
        val end = offset + size
        fun prefix(from: Int): Int {
            var i = from + 2
            while (i < end) {
                if (src.get(i).toInt() == 1 && src.get(i - 1).toInt() == 0 && src.get(i - 2).toInt() == 0)
                    return if (i >= from + 3 && src.get(i - 3).toInt() == 0) i - 3 else i - 2
                i += if (src.get(i).toInt() and 0xfe != 0) 3 else 1
            }
            return end
        }
        var start = prefix(offset)
        if (start != offset) return size
        var write = offset
        while (start < end) {
            val header = start + if (src.get(start + 2).toInt() == 1) 3 else 4
            if (header + 1 >= end) break
            val next = prefix(header + 2)
            val first = src.get(header).toInt() and 255
            val second = src.get(header + 1).toInt() and 255
            val type = (first ushr 1) and 63
            if (first and 1 == 0 && second and 0xf8 == 0 && type != 62 && type != 63) {
                if (write != start) {
                    val unit = src.duplicate().apply { position(start); limit(next) }
                    dst.position(write)
                    dst.put(unit)
                }
                write += next - start
            }
            start = next
        }
        return write - offset
    }

    // Extractors submit Annex-B HEVC NALs. Require a layer-zero IRAP with a
    // first slice, not merely an enhancement layer, RPU or codec-config NAL.
    // No copying/mutation of the MediaCodec input buffer; scan only bounded
    // prefix of the first usable sample. Encrypted samples are not evidence.
    fun hasBaseRandomAccess(buffer: ByteBuffer, offset: Int, size: Int): Boolean {
        if (offset < 0 || size < 6 || offset > buffer.capacity() - size) return false
        val data = buffer.duplicate().apply { clear() }
        val end = offset + minOf(size, MAX_SCAN)
        for (i in offset until end - 5) {
            if (data.get(i).toInt() != 0 || data.get(i + 1).toInt() != 0 ||
                data.get(i + 2).toInt() != 1) continue
            val first = data.get(i + 3).toInt() and 255
            val second = data.get(i + 4).toInt() and 255
            val type = (first shr 1) and 63
            if (type in 16..23 && baseType(first, second, type) &&
                data.get(i + 5).toInt() and 0x80 != 0) return true
        }
        return false
    }
}
