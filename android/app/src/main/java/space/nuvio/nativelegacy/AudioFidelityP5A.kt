package space.nuvio.nativelegacy

/**
 * P5-A/Q36: source audio quality is NOT the same as end-to-end output quality.
 * These are prospective capability decisions, not proof that the HDMI hardware
 * actually emitted an unchanged bitstream. P7/P8 must verify the real output.
 * No codec label, renderer support or mere track existence grants sink support.
 */
internal data class AudioFidelityDecisionP5A(
    val decoder: Boolean,
    val pcmCompatible: Boolean,
    val encodedPassthrough: Boolean,
    val outputFaithfulCandidate: Boolean,
    val unknown: Boolean,
    val sourceQualityScore: Long,
    val reason: String
)

internal object AudioFidelityP5A {
    private fun lossless(mime: String): Boolean {
        val m = mime.lowercase()
        return m.contains("flac") || m.contains("alac") ||
            m.contains("truehd") || m.contains("true-hd") || m.contains("mlp") ||
            m.contains("dts.hd") || m.contains("dts-hd") || m.contains("pcm") ||
            m == "audio/raw"
    }

    private fun hdBitstream(mime: String): Boolean {
        val m = mime.lowercase()
        return m.contains("truehd") || m.contains("true-hd") || m.contains("mlp") ||
            m.contains("dts.hd") || m.contains("dts-hd")
    }

    fun assess(
        mime: String, channels: Int, sampleRate: Int, bitDepth: Int,
        bitrate: Int, decoder: Boolean, pcmRoute: Boolean, encodedRoute: Boolean,
        standardArc: Boolean, dspEnabled: Boolean
    ): AudioFidelityDecisionP5A {
        // No usable format is inferred from a missing channel count or sample rate.
        val valid = channels in 1..16 && sampleRate in 8000..384000
        val bitstream = decoder && valid && encodedRoute && !dspEnabled &&
            !(standardArc && hdBitstream(mime))
        val pcm = decoder && valid && pcmRoute
        // The app's audio DSP is 16-bit/float PCM; it is not a lossless
        // conversion for an unknown or >16-bit lossless compressed source.
        val pcmFaithful = pcm && (!lossless(mime) || bitDepth in 1..16)
        val faithful = bitstream || pcmFaithful
        val unknown = decoder && (!valid || (pcm && lossless(mime) &&
            bitDepth !in 1..16 && !bitstream) || (!pcmRoute && !encodedRoute))
        val quality = sourceQuality(mime, channels, sampleRate, bitDepth, bitrate)
        val reason = when {
            !decoder -> "decoder unavailable"
            !valid -> "channel/sample-rate metadata unknown"
            !pcm && !bitstream && standardArc && hdBitstream(mime) -> "ARC cannot prove HD bitstream or faithful PCM"
            !pcm && !bitstream -> "current route has no confirmed compatible output"
            !faithful -> "decoded PCM may lose source bit depth"
            bitstream -> "encoded path advertised; physical passthrough still unverified"
            else -> "compatible PCM path advertised; actual output fidelity unverified"
        }
        return AudioFidelityDecisionP5A(decoder, pcm, bitstream, faithful, unknown, quality, reason)
    }

    fun sourceQuality(mime: String, channels: Int, sampleRate: Int, bitDepth: Int, bitrate: Int): Long {
        val base = if (lossless(mime)) 1_000_000L else 100_000L
        val ch = channels.coerceIn(0, 16).toLong() * 10_000L
        val rate = sampleRate.coerceIn(0, 384000).toLong() / 10L
        val depth = bitDepth.coerceIn(0, 64).toLong() * 300L
        // Declared bits/s is a source fact, not measured output quality.
        val rateBits = bitrate.coerceIn(0, 20_000_000).toLong() / 1000L
        return base + ch + rate + depth + rateBits
    }

    // Manual user ownership is evaluated before the automatic original choice;
    // a missing or incompatible manual selection NEVER authorizes a replacement.
    fun chooseOriginal(
        candidates: List<Triple<String, AudioFidelityDecisionP5A, Int>>,
        original: String?,
        manualIdentity: Int? = null
    ): Int? {
        if (manualIdentity != null)
            return candidates.firstOrNull { it.third == manualIdentity && it.second.outputFaithfulCandidate }?.third
        if (original.isNullOrBlank() || original == "none") return null
        return candidates.filter { (language, result) ->
            language.isNotBlank() && language.equals(original, ignoreCase = true) &&
                result.outputFaithfulCandidate
        }.maxByOrNull { it.second.sourceQualityScore }?.third
    }
}
