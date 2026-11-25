package space.nuvio.nativelegacy

private fun decide(mime: String = "audio/ac3", channels: Int = 2, rate: Int = 48000,
                   bits: Int = 0, decoder: Boolean = true, pcm: Boolean = true,
                   pass: Boolean = false, arc: Boolean = false, dsp: Boolean = false) =
    AudioFidelityP5A.assess(mime, channels, rate, bits, 640000, decoder, pcm, pass, arc, dsp)

fun main() {
    check(decide().outputFaithfulCandidate) // lossy original: no new lossy encode
    check(!decide(channels = 6, pcm = false).outputFaithfulCandidate)
    check(!decide(decoder = false, pass = true).encodedPassthrough)
    check(decide(pass = true).encodedPassthrough)
    check(!decide(pass = true, dsp = true).encodedPassthrough)
    check(!decide(mime = "audio/true-hd", channels = 8, pcm = false, pass = true, arc = true).outputFaithfulCandidate)
    check(decide(mime = "audio/true-hd", channels = 2, bits = 16, pcm = true, arc = true).outputFaithfulCandidate)
    check(!decide(mime = "audio/true-hd", channels = 2, bits = 0, pcm = true, arc = true).outputFaithfulCandidate)
    check(decide(mime = "audio/flac", channels = 2, bits = 24).unknown)
    check(!decide(rate = 0).outputFaithfulCandidate)
    check(decide(rate = 0).unknown)
    val original = decide(mime = "audio/flac", channels = 2, bits = 16)
    val dub = decide(mime = "audio/ac3", channels = 2)
    val list = listOf(Triple("en", dub, 0), Triple("ja", original, 1))
    check(AudioFidelityP5A.chooseOriginal(list, "ja") == 1)
    check(AudioFidelityP5A.chooseOriginal(list, "sv") == null)
    check(AudioFidelityP5A.chooseOriginal(list, null) == null)
    check(AudioFidelityP5A.chooseOriginal(list, "ja", manualIdentity = 0) == 0)
    check(AudioFidelityP5A.chooseOriginal(list, "ja", manualIdentity = 7) == null)
    check(original.sourceQualityScore > dub.sourceQualityScore)
    val converted = decide(mime = "audio/true-hd", channels = 8, bits = 24, pcm = false, arc = true)
    check(converted.playableCandidate && !converted.outputFaithfulCandidate)
    check(decide(rate = 0).playableCandidate)
    check(!decide(decoder = false).playableCandidate)
    println("p5_audio_fidelity: PASS (20 Kotlin/JVM policy assertions)")
}
