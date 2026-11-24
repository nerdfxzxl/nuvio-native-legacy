package space.nuvio.nativelegacy

import android.media.MediaFormat
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.CodecSpecificDataUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.CryptoInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import java.io.IOException
import java.nio.ByteBuffer

/** Keep the real Media3 codec factory and its fallback order. Only a Dolby
 * Vision -> HEVC alternate receives additional positive-evidence checks. */
@androidx.annotation.OptIn(UnstableApi::class)
internal object HdrCodecFallbackP5B {
    class Proof {
        @Volatile var sample = false
        @Volatile var hdrOutput = false
        @Volatile var valid = true
        fun ready() = valid && sample && hdrOutput
    }

    data class VideoConfig(val info: MediaCodecInfo, val source: Format, val base: Proof?) {
        fun matches(format: Format, name: String?): Boolean =
            name == info.name && source.sampleMimeType == format.sampleMimeType &&
                source.codecs == format.codecs && source.width == format.width &&
                source.height == format.height && source.drmInitData == format.drmInitData &&
                source.initializationDataEquals(format)
    }

    class Factory(private val delegate: MediaCodecAdapter.Factory,
                  private val displayHdr10: () -> Boolean,
                  private val displayDv: () -> Boolean,
                  private val report: (VideoConfig?) -> Unit) : MediaCodecAdapter.Factory {
        override fun createAdapter(configuration: MediaCodecAdapter.Configuration): MediaCodecAdapter {
            val f = configuration.format
            if (!MimeTypes.isVideo(f.sampleMimeType)) return delegate.createAdapter(configuration)
            report(null)
            val info = configuration.codecInfo
            val alternate = f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION &&
                info.mimeType != MimeTypes.VIDEO_DOLBY_VISION
            val proof = if (alternate) {
                validateAlternate(configuration)
                Proof()
            } else {
                // Reject incompatible native DV before initialization. Media3
                // can then try its remaining compatible same-source candidates,
                // rather than accepting an invalid native DV frame first.
                if (f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION) validateNativeDolby(info, f)
                if (info.softwareOnly && !info.isFormatSupported(f))
                    throw IOException("Software decoder cannot preserve the actual video format")
                null
            }
            val codec = delegate.createAdapter(configuration)
            report(VideoConfig(info, f, proof))
            return if (proof != null) ObservedBase(codec, proof) else codec
        }

        private fun validateNativeDolby(info: MediaCodecInfo, f: Format) {
            val profile = CodecSpecificDataUtil.getCodecProfileAndLevel(f)
                ?: throw IOException("Unrecognized Dolby Vision profile/level")
            val caps = info.capabilities ?: throw IOException("Unknown Dolby Vision decoder capabilities")
            val vc = caps.videoCapabilities ?: throw IOException("Unknown Dolby Vision video capabilities")
            val size = f.width > 0 && f.height > 0 &&
                if (f.frameRate.isFinite() && f.frameRate > 0f)
                    vc.areSizeAndRateSupported(f.width, f.height, f.frameRate.toDouble())
                else vc.isSizeSupported(f.width, f.height)
            if (!HdrPolicyP5.compatibleDolby(displayDv(),
                    caps.profileLevels.any { it.profile == profile.first && it.level >= profile.second }, size) ||
                !info.isFormatSupported(f))
                throw IOException("Native Dolby Vision decoder/output cannot preserve this format")
        }

        private fun validateAlternate(c: MediaCodecAdapter.Configuration) {
            val f = c.format
            val info = c.codecInfo
            // Clear, same-stream HEVC only. DRM, HLG, SDR, ambiguous or other
            // alternative MIME paths must not silently become HDR10.
            if (info.mimeType != MimeTypes.VIDEO_H265 || c.crypto != null ||
                info.secure || f.drmInitData != null || !displayHdr10())
                throw IOException("No verified clear HDR10 output path")
            val evidence = HdrBaseLayerP5B.inspect(f.codecs, f.width, f.height, f.initializationData)
                ?: throw IOException("No independent HEVC HDR10 base-layer configuration")
            val color = f.colorInfo
            fun consistent(actual: Int?, expected: Int) =
                actual == null || actual == Format.NO_VALUE || actual == expected
            if (!consistent(color?.colorSpace, C.COLOR_SPACE_BT2020) ||
                !consistent(color?.colorTransfer, C.COLOR_TRANSFER_ST2084) ||
                !consistent(color?.colorRange, C.COLOR_RANGE_LIMITED))
                throw IOException("Conflicting base-layer color metadata")
            val pt = evidence.profile
            val codecs = CodecSpecificDataUtil.buildHevcCodecString(pt.generalProfileSpace,
                pt.generalTierFlag, pt.generalProfileIdc, pt.generalProfileCompatibilityFlags,
                pt.constraintBytes, pt.generalLevelIdc)
            val baseColor = (color?.buildUpon() ?: ColorInfo.Builder())
                .setColorSpace(C.COLOR_SPACE_BT2020).setColorTransfer(C.COLOR_TRANSFER_ST2084)
                .setColorRange(C.COLOR_RANGE_LIMITED).build()
            val baseFormat = f.buildUpon().setSampleMimeType(MimeTypes.VIDEO_H265)
                .setCodecs(codecs).setColorInfo(baseColor).build()
            val profile = CodecSpecificDataUtil.getCodecProfileAndLevel(baseFormat)
                ?: throw IOException("Unrecognized HEVC profile/level")
            val caps = info.capabilities ?: throw IOException("Unknown HEVC codec capabilities")
            val vc = caps.videoCapabilities ?: throw IOException("Unknown HEVC video capabilities")
            val size = if (f.frameRate.isFinite() && f.frameRate > 0f)
                vc.areSizeAndRateSupported(f.width, f.height, f.frameRate.toDouble())
            else vc.isSizeSupported(f.width, f.height)
            if (!size || caps.profileLevels.none { it.profile == profile.first && it.level >= profile.second } ||
                !info.isFormatSupported(baseFormat))
                throw IOException("Alternate HEVC decoder cannot preserve the source format")
            // MediaCodecVideoRenderer otherwise writes a DV profile even for
            // its HEVC alternate. Use the independently parsed HEVC profile;
            // preserve the actual CSD, size, rate, source and all other data.
            c.mediaFormat.setInteger(MediaFormat.KEY_PROFILE, profile.first)
            c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
            c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084)
            c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
        }
    }

    // Observe the real codec; do not copy, transcode or replace its samples.
    // Keep ordinary audio/video adapters untouched and delegate every other
    // operation, including asynchronous codec behavior and surface ownership.
    private class ObservedBase(private val codec: MediaCodecAdapter, private val proof: Proof) :
        MediaCodecAdapter by codec {
        private var inputIndex = -1
        private var input: ByteBuffer? = null

        override fun getInputBuffer(index: Int): ByteBuffer? {
            val b = codec.getInputBuffer(index)
            if (!proof.sample) { inputIndex = index; input = b }
            return b
        }

        override fun queueInputBuffer(index: Int, offset: Int, size: Int,
                                      presentationTimeUs: Long, flags: Int) {
            val candidate = !proof.sample && inputIndex == index &&
                input?.let { HdrBaseLayerP5B.hasBaseRandomAccess(it, offset, size) } == true
            input = null; inputIndex = -1
            codec.queueInputBuffer(index, offset, size, presentationTimeUs, flags)
            if (candidate) proof.sample = true
        }

        override fun queueSecureInputBuffer(index: Int, offset: Int, info: CryptoInfo,
                                            presentationTimeUs: Long, flags: Int) {
            proof.valid = false
            input = null; inputIndex = -1
            codec.queueSecureInputBuffer(index, offset, info, presentationTimeUs, flags)
        }

        override fun getOutputFormat(): MediaFormat {
            val f = codec.outputFormat
            proof.hdrOutput = try {
                f.getInteger(MediaFormat.KEY_COLOR_STANDARD) == MediaFormat.COLOR_STANDARD_BT2020 &&
                    f.getInteger(MediaFormat.KEY_COLOR_TRANSFER) == MediaFormat.COLOR_TRANSFER_ST2084 &&
                    f.getInteger(MediaFormat.KEY_COLOR_RANGE) == MediaFormat.COLOR_RANGE_LIMITED
            } catch (_: Exception) { false }
            return f
        }

        override fun flush() {
            proof.sample = false
            input = null; inputIndex = -1
            codec.flush()
        }

        override fun release() {
            proof.valid = false
            input = null; inputIndex = -1
            codec.release()
        }

        override fun setOnFrameRenderedListener(listener: MediaCodecAdapter.OnFrameRenderedListener,
                                                handler: Handler) {
            codec.setOnFrameRenderedListener({ _, pts, time ->
                listener.onFrameRendered(this, pts, time)
            }, handler)
        }
    }
}
