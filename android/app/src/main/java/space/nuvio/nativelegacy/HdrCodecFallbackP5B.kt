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

    // Actual parsed base is mandatory at all call sites. A phone can try a
    // hardware codec with positive functional (MIME/profile) support even
    // when stock firmware reports that the requested size/rate exceeds caps.
    private fun canTryBase(info: MediaCodecInfo, base: Format, mobile: Boolean): Boolean =
        info.mimeType == MimeTypes.VIDEO_H265 &&
            (info.isFormatSupported(base) || (mobile && HdrPolicyP5.canProbeVerifiedBase(
                true, info.hardwareAccelerated, info.softwareOnly, info.secure,
                info.isFormatFunctionallySupported(base))))

    // Stock Media3 may reuse a codec even when Dolby profile/CSD changes.
    // Require a fresh capability check for changed DV or software-decoded
    // formats; ordinary hardware SDR/HDR adaptation stays stock.
    fun needsRevalidation(old: Format, next: Format, software: Boolean): Boolean {
        val guarded = software || old.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION ||
            next.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION
        return guarded && (!sameVideoFormat(old, next) ||
            java.lang.Float.compare(old.frameRate, next.frameRate) != 0)
    }

    class PacketFailure : IllegalStateException("Invalid clear Dolby Vision access unit")

    // Extend only the actual platform video renderer. Preserve Media3's
    // format negotiation, clock, surface and extension renderer behavior.
    class GuardedRenderer(builder: MediaCodecVideoRenderer.Builder,
                          private val config: () -> VideoConfig?,
                          private val recovery: () -> RuntimeRecovery?,
                          private val displayDv: () -> Boolean,
                          private val mobile: Boolean,
                          private val positionReset: (Long) -> Unit,
                          private val videoSample: (Long, Int) -> Unit = { _, _ -> }) : MediaCodecVideoRenderer(builder) {
        override fun onQueueInputBuffer(buffer: androidx.media3.decoder.DecoderInputBuffer) {
            // Called after flip, before codec queue; never change position or data.
            val size=buffer.data?.remaining() ?: 0
            if(size>0) videoSample(buffer.timeUs-getStreamOffsetUs(),size)
            super.onQueueInputBuffer(buffer)
        }
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
            val baseSupport = RendererCapabilities.getFormatSupport(support)
            if (baseSupport == C.FORMAT_HANDLED)
                return RendererCapabilities.create(C.FORMAT_HANDLED)
            // Media3's normal EXCEEDS_CAPABILITIES outcome is selectable when
            // enabled by the track selector; keep it distinct from HANDLED.
            if (mobile && baseSupport == C.FORMAT_EXCEEDS_CAPABILITIES &&
                super.getDecoderInfos(selector, base, false).any { canTryBase(it, base, true) })
                return RendererCapabilities.create(C.FORMAT_EXCEEDS_CAPABILITIES)
            return native
        }

        override fun getDecoderInfos(selector: MediaCodecSelector, format: Format,
                                     requiresSecureDecoder: Boolean): List<MediaCodecInfo> {
            val stock = super.getDecoderInfos(selector, format, requiresSecureDecoder)
            if (format.sampleMimeType != MimeTypes.VIDEO_DOLBY_VISION) {
                // Apply hardware preference after Media3 evaluated this representation.
                return HdrPolicyP5.ordinaryDecoders(stock, mobile,
                    { it.hardwareAccelerated }, { it.softwareOnly }, { it.isFormatSupported(format) })
            }
            val native = stock.filter { it.mimeType == MimeTypes.VIDEO_DOLBY_VISION && displayDv() }
            val base = baseFormat(format) ?: return native
            if (requiresSecureDecoder) return native
            return native + super.getDecoderInfos(selector, base, false).filter {
                canTryBase(it, base, mobile)
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
            // A codec may omit color keys on surface output, including after a
            // seek. Record available metadata without rejecting a decoded frame.
            super.onOutputFormatChanged(format, mediaFormat)
        }
    }

    data class VideoConfig(val info: MediaCodecInfo, val source: Format, val base: Proof?) {
        var convertedDv81 = false
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
                  private val mobile: Boolean,
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
            val isDv = f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION
            val dv7 = isDv && f.codecs?.split('.')?.getOrNull(1)?.toIntOrNull() == 7
            val caps = info.capabilities?.profileLevels.orEmpty()
            val native7 = caps.any { it.profile == android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDtb }
            val accepts8 = caps.any { it.profile == android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt }
            val convert = !alternate && dv7 && !native7 && displayDv() && info.hardwareAccelerated &&
                (accepts8 || android.os.Build.MANUFACTURER.equals("Xiaomi", true)) &&
                configuration.crypto == null && f.drmInitData == null && baseFormat(f) != null && DvPacket.available
            val proof = if (alternate) {
                validateAlternate(configuration)
                Proof().apply { requestSdr = !displayHdr10() }
            } else {
                if (isDv && !displayDv()) throw Rejection("Display does not advertise Dolby Vision")
                if (dv7 && !native7 && !convert && baseFormat(f) != null)
                    throw Rejection("Trying verified same-stream HEVC base")
                if (convert) {
                    stripEnhancementCsd(configuration)
                    configuration.mediaFormat.setInteger(MediaFormat.KEY_PROFILE,
                        android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt)
                }
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
            report(VideoConfig(info, f, proof).also {
                it.requestedToneMap = toneMap
                it.convertedDv81 = convert
            })
            return if (proof != null || convert) ObservedBase(codec, proof, convert) else codec
        }

        private fun stripEnhancementCsd(c: MediaCodecAdapter.Configuration) {
            for ((i, csd) in c.format.initializationData.withIndex()) {
                val data = ByteBuffer.wrap(csd.copyOf())
                data.limit(HdrBaseLayerP5B.keepBaseUnits(data, 0, csd.size))
                c.mediaFormat.setByteBuffer("csd-$i", data)
            }
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
            // Media3 accounts for platform alignment and fractional frame rates.
            if (!canTryBase(info, baseFormat, mobile)) throw Rejection("Unsupported HEVC representation")
            if (mobile && !info.isFormatSupported(baseFormat))
                trace("verified clear HEVC base hardware trial; platform size/rate capability exceeded")
            stripEnhancementCsd(c)
            c.mediaFormat.setInteger(MediaFormat.KEY_PROFILE, profile.first)
            c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
            c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084)
            c.mediaFormat.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)

        }
    }

    // Observe the real codec; do not copy, transcode or replace its samples.
    // Keep ordinary audio/video adapters untouched and delegate every other
    // operation, including asynchronous codec behavior and surface ownership.
    private class ObservedBase(private val codec: MediaCodecAdapter, private val proof: Proof?, private val convertDv: Boolean) :
        MediaCodecAdapter by codec {
        private var scratch: ByteBuffer? = null
        private var inputIndex = -1
        private var input: ByteBuffer? = null

        override fun getInputBuffer(index: Int): ByteBuffer? {
            val b = codec.getInputBuffer(index)
            inputIndex = index; input = b
            return b
        }

        override fun queueInputBuffer(index: Int, offset: Int, size: Int,
                                      presentationTimeUs: Long, flags: Int) {
            val buffer = input?.takeIf { index == inputIndex }
            val candidate = proof?.sample == false && buffer != null &&
                HdrBaseLayerP5B.hasBaseRandomAccess(buffer, offset, size)
            val submitted = when {
                size == 0 -> 0
                buffer == null -> throw PacketFailure()
                convertDv -> {
                    // A single reusable scratch buffer, sized to this access unit
                    // plus RPU headroom and bounded by the codec's own input slot.
                    val capacity = minOf(size.toLong() + 8192, (buffer.capacity() - offset).toLong()).toInt()
                    val out = scratch?.takeIf { it.capacity() >= capacity } ?:
                        ByteBuffer.allocateDirect(capacity).also { scratch = it }
                    DvPacket.convert(buffer, offset, size, out).also { if (it < 0) throw PacketFailure() }
                }
                else -> HdrBaseLayerP5B.keepBaseUnits(buffer, offset, size)
            }
            input = null
            inputIndex = -1
            codec.queueInputBuffer(index, offset, submitted, presentationTimeUs, flags)
            if (candidate) proof?.sample = true
        }

        override fun queueSecureInputBuffer(index: Int, offset: Int, info: CryptoInfo,
                                            presentationTimeUs: Long, flags: Int) {
            proof?.valid = false
            input = null; inputIndex = -1
            codec.queueSecureInputBuffer(index, offset, info, presentationTimeUs, flags)
        }

        override fun getOutputFormat(): MediaFormat {
            val f = codec.outputFormat
            proof?.hdrOutput = try {
                f.getInteger(MediaFormat.KEY_COLOR_STANDARD) == MediaFormat.COLOR_STANDARD_BT2020 &&
                    f.getInteger(MediaFormat.KEY_COLOR_TRANSFER) == MediaFormat.COLOR_TRANSFER_ST2084 &&
                    f.getInteger(MediaFormat.KEY_COLOR_RANGE) == MediaFormat.COLOR_RANGE_LIMITED
            } catch (_: Exception) { false }
            proof?.sdrOutput = try {
                f.getInteger(MediaFormat.KEY_COLOR_TRANSFER) == MediaFormat.COLOR_TRANSFER_SDR_VIDEO
            } catch (_: Exception) { false }
            return f
        }

        override fun flush() {
            proof?.sample = false
            input = null; inputIndex = -1
            codec.flush()
        }

        override fun release() {
            proof?.valid = false
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


// The production Rust build supplies a separate JNI library for both Android ABIs.
internal object DvPacket {
    val available by lazy {
        try { System.loadLibrary("nuvio_dovi"); true } catch (_: UnsatisfiedLinkError) { false }
    }
    external fun convert(input: ByteBuffer, offset: Int, length: Int, scratch: ByteBuffer): Int
}
