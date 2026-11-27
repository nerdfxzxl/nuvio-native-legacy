package space.nuvio.nativelegacy

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.*

/** Execute the AudioProcessor installed by NvPlayer, including the negotiated
 * output channel count. Report settled RMS separately from overload peaks. */
fun main() {
    val rate = 48000
    fun db(ratio: Double) = 20 * log10(ratio)
    fun processor(channels: Int = 2, fold: Boolean = false,
                  setup: GanhoAudioProcessor.() -> Unit = {}): GanhoAudioProcessor {
        val p = GanhoAudioProcessor().apply(setup)
        p.stereo = fold
        check(p.configure(AudioProcessor.AudioFormat(rate, channels, C.ENCODING_PCM_FLOAT)).channelCount ==
            if (fold) 2 else channels)
        p.flush()
        return p
    }
    fun render(p: GanhoAudioProcessor, count: Int, channels: Int, source: (Int, Int) -> Float): FloatArray {
        val input = ByteBuffer.allocateDirect(count * channels * 4).order(ByteOrder.nativeOrder())
        repeat(count) { i -> repeat(channels) { ch -> input.putFloat(source(i, ch)) } }
        input.flip()
        p.queueInput(input)
        check(!input.hasRemaining())
        val samples = p.output.order(ByteOrder.nativeOrder()).asFloatBuffer()
        return FloatArray(samples.remaining()).also { samples.get(it) }
    }
    fun tone(p: GanhoAudioProcessor, hz: Int, amplitude: Float, seconds: Int = 2): Double {
        val y = render(p, rate * seconds, 2) { i, ch ->
            (sin(2.0 * PI * hz * i / rate) * amplitude * (if (ch == 0) 1f else -0.5f)).toFloat()
        }
        var sum = 0.0
        val from = y.size - rate
        for (i in from until y.size step 2) {
            check(abs(y[i + 1] + y[i] * 0.5f) < 1e-5f)
            sum += y[i] * y[i]
        }
        return db(sqrt(sum / (rate / 2)) / (amplitude / sqrt(2.0)))
    }
    fun measure(label: String, value: Double, low: Double, high: Double) {
        println(String.format(Locale.ROOT, "%s %.3f dB", label, value))
        check(value in low..high) { "$label outside [$low, $high]" }
    }
    val neutral = processor()
    val dry = render(neutral, 200, 2) { i, ch -> (i - ch - 99) / 199f }
    check(dry.indices.all { i -> dry[i].toRawBits() == ((i / 2 - i % 2 - 99) / 199f).toRawBits() })
    val amp = processor { amplificacaoDb = 10 }
    measure("Gain at -30 dBFS", tone(amp, 1000, 0.0316228f), 9.98, 10.02)
    val hot = render(amp, rate, 2) { i, ch -> (sin(2.0 * PI * 900 * i / rate) * (if (ch == 0) 1 else -1)).toFloat() }
    val peak = hot.maxOf { abs(it) }
    check(peak <= 32767f / 32768f + 1e-6f && peak > .98f && hot.none { abs(it) == 1f })
    println("10 dB overload peak=$peak, full-scale samples=0")
    for (bass in listOf(-12, 6)) {
        val shelf = processor { baixoDb = bass }
        measure("Bass $bass at 30 Hz", tone(shelf, 30, .05f), bass - .2, bass + .2)
        measure("Bass $bass at 1 kHz", tone(shelf, 1000, .05f), -.02, .02)
    }
    for ((night, makeup) in listOf(1 to 6, 2 to 10, 3 to 15)) {
        val gains = listOf(125, 1000, 8000).map { hz -> tone(processor { noiteNivel = night }, hz, .01f, 3) }
        for (v in gains) measure("Night $night quiet tone", v, makeup - .2, makeup + .2)
        val loud = tone(processor { noiteNivel = night }, 1000, .8f, 3)
        check(loud < gains[1] - 3 && loud > -1)
        println("Night $night quiet/loud gain difference=${gains[1]-loud} dB")
        val noise = tone(processor { noiteNivel = night }, 1000, .0001f)
        check(abs(noise) < .02)
    }
    fun isolated(channels: Int, lane: Int, center: Int = 0): FloatArray {
        val p = processor(channels, true) { centroDb = center }
        return render(p, 480, channels) { _, ch -> if (ch == lane) .01f else 0f }
    }
    val front = isolated(6, 0)
    check(front[0] == .01f && front[1] == 0f)
    val center = isolated(6, 2)
    for (setting in listOf(-10, 0, 6, 30)) {
        val y = isolated(6, 2, setting)
        check(y[0] == y[1])
        measure("Center $setting", db((y[0]/center[0]).toDouble()), setting - .01, setting + .01)
    }
    check(isolated(6, 3).all { it == 0f })
    check(isolated(4, 2, 30).contentEquals(isolated(4, 2, 0)))
    // Packed 24-bit input preserves detail below a 16-bit LSB.
    val packed = GanhoAudioProcessor()
    check(packed.configure(AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_24BIT)).encoding == C.ENCODING_PCM_FLOAT)
    packed.flush()
    packed.queueInput(ByteBuffer.wrap(byteArrayOf(1, 0, 0, -1, -1, -1)))
    val precise = packed.output.order(ByteOrder.nativeOrder())
    check(precise.float == 1f / 8388608 && precise.float == -1f / 8388608)
    val nativeDownmix = processor { decoderDownmixed = true }
    val limited = render(nativeDownmix, 1, 2) { _, ch -> if (ch == 0) 1.8f else .9f }
    check(limited[0] < 1f && limited[0] > .99f && limited[0] == 2f * limited[1])
    val transition = processor()
    render(transition, 100, 2) { _, _ -> .05f }
    transition.amplificacaoDb = 10
    val moving = render(transition, 4800, 2) { _, _ -> .05f }
    check(moving[0] in .05f..0.051f && abs(moving.last() - .158114f) < .0001f)
    transition.amplificacaoDb = 0
    val restored = render(transition, 6000, 2) { _, _ -> .05f }
    check(restored.last() == .05f)
    println("PCM neutral, linked channels, quad layout and live restoration PASS")
}
