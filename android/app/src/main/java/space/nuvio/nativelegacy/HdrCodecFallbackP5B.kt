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
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
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
        // HDR10 decode can be presented by the Android compositor even if
        // the codec does not implement direct SDR tone mapping. It is not
        // proof of physical HDR/SDR display output.
        fun ready() = valid && sample && (hdrOutput || (requestSdr && sdrOutput))
    }

    private fun sameVideoFormat(a: Format, b: Format): Boolean =
        a.sampleMimeType == b.sampleMimeType && a.codecs == b.codecs &&
            a.width == b.width && a.height == b.height &&
            a.drmInitData == b.drmInitData && a.colorInfo == b.colorInfo &&
            a.initializationDataEquals(b)

    internal fun baseFormat(f: Format): Format? {
        if (f.sampleMimeType != MimeTypes.VIDEO_DOLBY_VISION || f.drmInitData != null) return null
        val evidence = HdrBaseLayerP5B.inspect(f.codecs, f.width, f.height, f.initializationData)
            ?: return null
        val color = f.colorInfo
        fun consistent(actual: Int?, expected: Int) =
            actual == null || actual == Format.NO_VALUE || actual == expected
        if (!consistent(color?.colorSpace, C.COLOR_SPACE_BT2020) ||
            !consistent(color?.colorTransfer, C.COLOR_TRANSFER_ST2084) ||
            !consistent(color?.colorRange, C.COLOR_RANGE_LIMITED))
            return null
        val pt = evidence.profile
        val codecs = CodecSpecificDataUtil.buildHevcCodecString(pt.generalProfileSpace,
            pt.generalTierFlag, pt.generalProfileIdc, pt.generalProfileCompatibilityFlags,
            pt.constraintBytes, pt.generalLevelIdc)
        val baseColor = (color?.buildUpon() ?: ColorInfo.Builder())
            .setColorSpace(C.COLOR_SPACE_BT2020).setColorTransfer(C.COLOR_TRANSFER_ST2084)
            .setColorRange(C.COLOR_RANGE_LIMITED).build()
        return f.buildUpon().setSampleMimeType(MimeTypes.VIDEO_H265)
            .setCodecs(codecs).setColorInfo(baseColor).build()
    }

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
        // Media3 1.8 deliberately omits a generic HEVC alternate for profile 7.
        // Expose only the independently parsed clear base to its normal HEVC
        // capability query; keep native DV first on a capable display.
        override fun supportsFormat(selector: MediaCodecSelector, format: Format): Int {
            if (format.sampleMimeType != MimeTypes.VIDEO_DOLBY_VISION)
                return super.supportsFormat(selector, format)
            val nativeOnly = MediaCodecSelector { mime, secure, tunnel ->
                if (mime == MimeTypes.VIDEO_DOLBY_VISION) selector.getDecoderInfos(mime, secure, tunnel)
                else emptyList()
            }
            val native = if (displayDv()) super.supportsFormat(nativeOnly, format)
                else RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE)
            if (RendererCapabilities.getFormatSupport(native) == C.FORMAT_HANDLED) return native
            val base = baseFormat(format) ?: return native
            val support = super.supportsFormat(selector, base)
            // This custom base adapter does not negotiate tunneled or seamless adaptive playback.
            return if (RendererCapabilities.getFormatSupport(support) == C.FORMAT_HANDLED)
                RendererCapabilities.create(C.FORMAT_HANDLED) else native
        }

        override fun getDecoderInfos(selector: MediaCodecSelector, format: Format,
                                     requiresSecureDecoder: Boolean): List<MediaCodecInfo> {
            val stock = super.getDecoderInfos(selector, format, requiresSecureDecoder)
            if (format.sampleMimeType != MimeTypes.VIDEO_DOLBY_VISION) return stock
            val native = stock.filter { it.mimeType == MimeTypes.VIDEO_DOLBY_VISION && displayDv() }
            val base = baseFormat(format) ?: return native
            if (requiresSecureDecoder) return native
            return native + super.getDecoderInfos(selector, base, false).filter {
                it.mimeType == MimeTypes.VIDEO_H265 && it.isFormatSupported(base)
            }
        }

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
            val current = config()
            current?.outputTransfer = mediaFormat?.takeIf { it.containsKey(MediaFormat.KEY_COLOR_TRANSFER) }
                ?.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
            if (format.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION) {
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
        var requestedToneMap = false
        @Volatile var outputTransfer: Int? = null
        fun matches(format: Format, name: String?): Boolean =
            name == info.name && sameVideoFormat(source, format)
    }

    /** A single actual failed decoder per player opening. During recovery
     * the new decoder must accept the same video representation, not a lower
     * resolution, color mode or bitrate silently selected by the track selector. */
    class RuntimeRecovery(val failed: String, private val source: Format,
                          val excluded: Set<String> = setOf(failed),
                          val platformOnly: Set<String> = emptySet()) {
        fun next(name: String, format: Format, requestedToneMap: Boolean): RuntimeRecovery? {
            if (!sameRepresentation(format) || name in excluded || excluded.size >= 4) return null
            return after(name, format, requestedToneMap, excluded, platformOnly)
        }

        companion object {
            fun after(name: String, format: Format, requestedToneMap: Boolean,
                      excluded: Set<String> = emptySet(), platformOnly: Set<String> = emptySet()): RuntimeRecovery =
                if (requestedToneMap && name !in platformOnly)
                    RuntimeRecovery(name, format, excluded, platformOnly + name)
                else RuntimeRecovery(name, format, excluded + name, platformOnly)
        }

        @Volatile var completed = false
            private set

        fun sameRepresentation(f: Format): Boolean =
            sameVideoFormat(source, f) && source.id == f.id &&
                source.averageBitrate == f.averageBitrate && source.peakBitrate == f.peakBitrate &&
                java.lang.Float.compare(source.frameRate, f.frameRate) == 0 &&
                source.rotationDegrees == f.rotationDegrees &&
                java.lang.Float.compare(source.pixelWidthHeightRatio, f.pixelWidthHeightRatio) == 0

        fun permits(name: String, f: Format): Boolean =
            name !in excluded && (completed || sameRepresentation(f))

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
                  private val displayHlg: () -> Boolean,
                  private val runtimeRecovery: () -> RuntimeRecovery?,
                  private val report: (VideoConfig?) -> Unit,
                  private val trace: (String) -> Unit = {}) : MediaCodecAdapter.Factory {
        override fun createAdapter(configuration: MediaCodecAdapter.Configuration): MediaCodecAdapter {
            val f = configuration.format
            if (!MimeTypes.isVideo(f.sampleMimeType)) return delegate.createAdapter(configuration)
            val info = configuration.codecInfo
            val sourceProfile = CodecSpecificDataUtil.getCodecProfileAndLevel(f)
            trace("attempt codec=${info.name} hw=${info.hardwareAccelerated} sw=${info.softwareOnly} mime=${info.mimeType} profile=${sourceProfile?.first} level=${sourceProfile?.second} size=${f.width}x${f.height} bits=${f.colorInfo?.lumaBitdepth}")
            return try {
                configureVideo(configuration)
            } catch (e: Exception) {
                trace("reject codec=${info.name} reason=${if (e is Rejection) e.message else e.javaClass.simpleName}")
                throw e
            }
        }

        private class Rejection(reason: String) : IOException(reason)

        private fun configureVideo(configuration: MediaCodecAdapter.Configuration): MediaCodecAdapter {
            val f = configuration.format
            report(null)
            val info = configuration.codecInfo
            if (runtimeRecovery()?.permits(info.name, f) == false)
                throw Rejection("Runtime video fallback cannot preserve the failed representation")
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
                    throw Rejection("Software decoder cannot preserve the actual video format")
                null
            }
            val platformOutput = when {
                proof != null -> proof.requestSdr
                f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION -> false
                f.colorInfo?.colorTransfer == C.COLOR_TRANSFER_ST2084 -> !displayHdr10()
                f.colorInfo?.colorTransfer == C.COLOR_TRANSFER_HLG -> !displayHlg()
                else -> false
            }
            var toneMap = platformOutput && android.os.Build.VERSION.SDK_INT >= 31 &&
                runtimeRecovery()?.platformOnly?.contains(info.name) != true
            if (toneMap) {
                trace("request codec SDR transfer")
                configuration.mediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER_REQUEST, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
            }
            val codec = try { delegate.createAdapter(configuration) } catch (e: Exception) {
                // Codec-side tone mapping is optional. Retry the proven base
                // once without the request and let Android render HDR buffers.
                if (!toneMap) throw e
                trace("tone-map configuration failed; retry platform presentation")
                toneMap = false
                configuration.mediaFormat.removeKey(MediaFormat.KEY_COLOR_TRANSFER_REQUEST)
                delegate.createAdapter(configuration)
            }
            trace("configured codec=${info.name} verifiedBase=${proof != null}")
            report(VideoConfig(info, f, proof).also { it.requestedToneMap = toneMap })
            return if (proof != null) ObservedBase(codec, proof) else codec
        }

        private fun validateNativeDolby(info: MediaCodecInfo, f: Format) {
            val profile = CodecSpecificDataUtil.getCodecProfileAndLevel(f)
                ?: throw Rejection("Unrecognized Dolby Vision profile/level")
            val caps = info.capabilities ?: throw Rejection("Unknown Dolby Vision decoder capabilities")
            val vc = caps.videoCapabilities ?: throw Rejection("Unknown Dolby Vision video capabilities")
            val size = f.width > 0 && f.height > 0 &&
                if (f.frameRate.isFinite() && f.frameRate > 0f)
                    vc.areSizeAndRateSupported(f.width, f.height, f.frameRate.toDouble())
                else vc.isSizeSupported(f.width, f.height)
            if (!HdrPolicyP5.compatibleDolby(displayDv(),
                    caps.profileLevels.any { it.profile == profile.first && it.level >= profile.second }, size) ||
                !info.isFormatSupported(f))
                throw Rejection("Native Dolby Vision decoder/output cannot preserve this format")
        }

        private fun validateAlternate(c: MediaCodecAdapter.Configuration) {
            val f = c.format
            val info = c.codecInfo
            // Clear, same-stream HEVC only. DRM, HLG, SDR, ambiguous or other
            // alternative MIME paths must not silently become HDR10.
            if (info.mimeType != MimeTypes.VIDEO_H265 || c.crypto != null ||
                info.secure || f.drmInitData != null)
                throw Rejection("No verified clear HDR10 output path")
            val baseFormat = baseFormat(f) ?: throw Rejection("No verified clear HDR10 base")
            val profile = CodecSpecificDataUtil.getCodecProfileAndLevel(baseFormat)
                ?: throw Rejection("Unrecognized HEVC profile/level")
            val caps = info.capabilities ?: throw Rejection("Unknown HEVC codec capabilities")
            val vc = caps.videoCapabilities ?: throw Rejection("Unknown HEVC video capabilities")
            val size = if (f.frameRate.isFinite() && f.frameRate > 0f)
                vc.areSizeAndRateSupported(f.width, f.height, f.frameRate.toDouble())
            else vc.isSizeSupported(f.width, f.height)
            if (!size || caps.profileLevels.none { it.profile == profile.first && it.level >= profile.second } ||
                !info.isFormatSupported(baseFormat))
                throw Rejection("Alternate HEVC decoder cannot preserve the source format")
            // MediaCodecVideoRenderer otherwise writes a DV profile even for
            // its HEVC alternate. Use the independently parsed HEVC profile;
            // preserve the actual CSD, size, rate, source and all other data.
            for ((i, csd) in f.initializationData.withIndex()) {
                val data = ByteBuffer.wrap(csd.copyOf())
                val count = HdrBaseLayerP5B.keepBaseUnits(data, 0, csd.size)
                data.limit(count)
                c.mediaFormat.setByteBuffer("csd-$i", data)
            }
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
            inputIndex = index; input = b
            return b
        }

        override fun queueInputBuffer(index: Int, offset: Int, size: Int,
                                      presentationTimeUs: Long, flags: Int) {
            val candidate = !proof.sample && inputIndex == index &&
                input?.let { HdrBaseLayerP5B.hasBaseRandomAccess(it, offset, size) } == true
            val submitted = if (inputIndex == index && size > 0)
                input?.let { HdrBaseLayerP5B.keepBaseUnits(it, offset, size) } ?: size else size
            input = null; inputIndex = -1
            codec.queueInputBuffer(index, offset, submitted, presentationTimeUs, flags)
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
