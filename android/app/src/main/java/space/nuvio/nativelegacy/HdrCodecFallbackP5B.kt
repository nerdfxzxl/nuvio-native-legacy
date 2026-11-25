package space.nuvio.nativelegacy

import android.media.MediaFormat
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.ColorInfo
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.CodecSpecificDataUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.CryptoInfo
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
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
        @Volatile var sdrOutput = false
        var requestSdr = false
        fun ready() = valid && sample && (if (requestSdr) sdrOutput else hdrOutput)
    }

    private fun sameVideoFormat(a: Format, b: Format): Boolean =
        a.sampleMimeType == b.sampleMimeType && a.codecs == b.codecs &&
            a.width == b.width && a.height == b.height &&
            a.drmInitData == b.drmInitData && a.colorInfo == b.colorInfo &&
            a.initializationDataEquals(b)

    // Stock Media3 may reuse a codec even when Dolby profile/CSD changes.
    // Require a fresh capability check for changed DV or software-decoded
    // formats; ordinary hardware SDR/HDR adaptation stays stock.
    fun needsRevalidation(old: Format, next: Format, software: Boolean): Boolean {
        val guarded = software || old.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION ||
            next.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION
        return guarded && (!sameVideoFormat(old, next) ||
            java.lang.Float.compare(old.frameRate, next.frameRate) != 0)
    }

    class OutputRejected : IOException("No verified compatible video output")

    // Extend only the actual platform video renderer. Preserve Media3's
    // format negotiation, clock, surface and extension renderer behavior.
    class GuardedRenderer(builder: MediaCodecVideoRenderer.Builder,
                          private val config: () -> VideoConfig?,
                          private val recovery: () -> RuntimeRecovery?,
                          private val displayDv: () -> Boolean,
                          private val displayHdr10: () -> Boolean,
                          private val positionReset: (Long) -> Unit) : MediaCodecVideoRenderer(builder) {
        override fun canReuseCodec(codecInfo: MediaCodecInfo, oldFormat: Format,
                                   newFormat: Format): DecoderReuseEvaluation {
            if (needsRevalidation(oldFormat, newFormat, codecInfo.softwareOnly) ||
                recovery()?.permits(codecInfo.name, newFormat) == false)
                return DecoderReuseEvaluation(codecInfo.name, oldFormat, newFormat,
                    DecoderReuseEvaluation.REUSE_RESULT_NO, DecoderReuseEvaluation.DISCARD_REASON_WORKAROUND)
            return super.canReuseCodec(codecInfo, oldFormat, newFormat)
        }

        override fun onPositionReset(positionUs: Long, joining: Boolean) {
            val at = android.os.SystemClock.elapsedRealtime()
            super.onPositionReset(positionUs, joining)
            positionReset(at)
        }

        override fun onOutputFormatChanged(format: Format, mediaFormat: MediaFormat?) {
            // The format dequeued for this actual output buffer (rather than
            // the latest adaptive selection) is the authority. A reused or
            // reconfigured decoder must never bypass positive DV/base proof.
            if (format.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION) {
                val current = config()
                val info = getCodecInfo()
                val supported = current != null && info != null && current.matches(format, info.name) &&
                    if (info.mimeType == MimeTypes.VIDEO_DOLBY_VISION) displayDv()
                    else info.mimeType == MimeTypes.VIDEO_H265 && current.base?.ready() == true &&
                        (current.base.requestSdr || displayHdr10())
                if (!supported) throw createRendererException(OutputRejected(), format,
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED)
            }
            super.onOutputFormatChanged(format, mediaFormat)
        }
    }

    data class VideoConfig(val info: MediaCodecInfo, val source: Format, val base: Proof?) {
        fun matches(format: Format, name: String?): Boolean =
            name == info.name && sameVideoFormat(source, format)
    }

    /** A single actual failed decoder per player opening. During recovery
     * the new decoder must accept the same video representation, not a lower
     * resolution, color mode or bitrate silently selected by the track selector. */
    class RuntimeRecovery(val failed: String, private val source: Format) {
        @Volatile var completed = false
            private set

        fun sameRepresentation(f: Format): Boolean =
            sameVideoFormat(source, f) && source.id == f.id &&
                source.averageBitrate == f.averageBitrate && source.peakBitrate == f.peakBitrate &&
                java.lang.Float.compare(source.frameRate, f.frameRate) == 0 &&
                source.rotationDegrees == f.rotationDegrees &&
                java.lang.Float.compare(source.pixelWidthHeightRatio, f.pixelWidthHeightRatio) == 0

        fun permits(name: String, f: Format): Boolean =
            name != failed && (completed || sameRepresentation(f))

        fun handoff(config: VideoConfig?, f: Format?, name: String?): Boolean {
            if (completed) return true
            if (config == null || f == null || name == null ||
                !config.matches(f, name) || !permits(name, f)) return false
            completed = true
            return true
        }
    }

    class Factory(private val delegate: MediaCodecAdapter.Factory,
                  private val displayHdr10: () -> Boolean,
                  private val displayDv: () -> Boolean,
                  private val runtimeRecovery: () -> RuntimeRecovery?,
                  private val report: (VideoConfig?) -> Unit) : MediaCodecAdapter.Factory {
        override fun createAdapter(configuration: MediaCodecAdapter.Configuration): MediaCodecAdapter {
            val f = configuration.format
            if (!MimeTypes.isVideo(f.sampleMimeType)) return delegate.createAdapter(configuration)
            report(null)
            val info = configuration.codecInfo
            if (runtimeRecovery()?.permits(info.name, f) == false)
                throw IOException("Runtime video fallback cannot preserve the failed representation")
            val alternate = f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION &&
                info.mimeType != MimeTypes.VIDEO_DOLBY_VISION
            val proof = if (alternate) {
                validateAlternate(configuration)
                Proof().apply { requestSdr = !displayHdr10() }
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
                info.secure || f.drmInitData != null || (!displayHdr10() && android.os.Build.VERSION.SDK_INT < 31))
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
            // Request platform codec tone mapping only for a positively proven
            // HDR10 base on a non-HDR display. Verify the actual SDR output;
            // a codec ignoring this request is not a successful downgrade.
            if (!displayHdr10()) c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER_REQUEST,
                MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
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
            proof.sdrOutput = try {
                f.getInteger(MediaFormat.KEY_COLOR_TRANSFER) == MediaFormat.COLOR_TRANSFER_SDR_VIDEO
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
