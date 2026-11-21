package space.nuvio.nativelegacy

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real PCM processing, not a volume shortcut:
 * - Bass uses a 110 Hz low-shelf biquad (-12..+6 dB, cuts prioritised).
 * - Amplification is broadband +0..+10 dB.
 * - Center mix changes the center PCM channel when a surround track exists.
 * - Night Mode offers three progressively stronger peak-compression levels.
 *
 * Media3 bypasses AudioProcessors for direct bitstream passthrough; never
 * silently disable passthrough or claim these controls work in that mode.
 * All DSP state belongs to the audio-renderer thread; UI values are volatile.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class GanhoAudioProcessor : BaseAudioProcessor() {
    @Volatile var ganho = 1f
    @Volatile var amplificacaoDb = 0
    @Volatile var baixoDb = 0
    @Volatile var centroDb = 0
    @Volatile var noiteNivel = 0

    // Transposed Direct Form II with per-channel histories (never shared).
    private var z1 = FloatArray(0)
    private var z2 = FloatArray(0)
    private var channels = 2
    private var sampleRate = 48000
    private var currentBass = Int.MIN_VALUE
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) return AudioProcessor.AudioFormat.NOT_SET
        channels = inputAudioFormat.channelCount.coerceIn(1, 32)
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        z1 = FloatArray(channels)
        z2 = FloatArray(channels)
        currentBass = Int.MIN_VALUE
        return inputAudioFormat
    }

    override fun onFlush() {
        z1.fill(0f)
        z2.fill(0f)
    }

    // RBJ low-shelf cookbook, slope S=1, cutoff 110 Hz.
    private fun updateBass(db: Int) {
        if (currentBass == db) return
        currentBass = db
        z1.fill(0f)
        z2.fill(0f)
        if (db == 0) { b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f; return }
        val A = 10.0.pow(db / 40.0)
        val omega = 2.0 * Math.PI * 110.0 / sampleRate
        val co = cos(omega)
        val sn = sin(omega)
        val alpha = sn / 2.0 * sqrt(2.0)
        val root = 2.0 * sqrt(A) * alpha
        val aa0 = (A + 1.0) + (A - 1.0) * co + root
        val bb0 = A * ((A + 1.0) - (A - 1.0) * co + root)
        val bb1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * co)
        val bb2 = A * ((A + 1.0) - (A - 1.0) * co - root)
        val aa1 = -2.0 * ((A - 1.0) + (A + 1.0) * co)
        val aa2 = (A + 1.0) + (A - 1.0) * co - root
        b0 = (bb0 / aa0).toFloat()
        b1 = (bb1 / aa0).toFloat()
        b2 = (bb2 / aa0).toFloat()
        a1 = (aa1 / aa0).toFloat()
        a2 = (aa2 / aa0).toFloat()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val n = inputBuffer.remaining()
        if (n == 0) return
        val out = replaceOutputBuffer(n)
        val src = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
        val bass = baixoDb.coerceIn(-12, 6)
        val amp = amplificacaoDb.coerceIn(0, 10)
        val center = centroDb.coerceIn(-10, 30)
        val night = noiteNivel.coerceIn(0, 3)
        val g = ganho.coerceAtLeast(0f)
        if (g <= 1f && bass == 0 && amp == 0 && center == 0 && night == 0) {
            out.put(src)
            inputBuffer.position(inputBuffer.limit())
            out.flip()
            return
        }
        updateBass(bass)
        val volume = (g * 10.0.pow(amp / 20.0)).toFloat()
        val centerGain = 10.0.pow(center / 20.0).toFloat()
        val nightGain = floatArrayOf(1f, 1.10f, 1.20f, 1.30f)[night]
        val threshold = floatArrayOf(1f, 0.52f, 0.40f, 0.30f)[night]
        val ratio = floatArrayOf(1f, 1.8f, 2.5f, 3.4f)[night]
        val applyBass = bass != 0
        val processCenter = center != 0 && channels >= 3
        val floatPcm = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerSample = if (floatPcm) 4 else 2
        var channel = 0
        while (src.remaining() >= bytesPerSample) {
            val x = if (floatPcm) src.float else src.short / 32768f
            var y = x
            if (applyBass) {
                // Direct Form II transposed low-shelf.
                val v = b0 * y + z1[channel]
                z1[channel] = b1 * y - a1 * v + z2[channel]
                z2[channel] = b2 * y - a2 * v
                y = v
            }
            if (processCenter && channel == 2) y *= centerGain
            y *= volume
            if (night > 0) {
                y *= nightGain
                val magnitude = abs(y)
                if (magnitude > threshold) {
                    val compressed = threshold + (magnitude - threshold) / ratio
                    y = if (y < 0f) -compressed else compressed
                }
            }
            // Protect 16-bit output from integer wrap and gross clipping.
            y = y.coerceIn(-1f, 1f)
            if (floatPcm) out.putFloat(y)
            else out.putShort((y * 32767f).toInt().coerceIn(-32768, 32767).toShort())
            channel++
            if (channel >= channels) channel = 0
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }
}
