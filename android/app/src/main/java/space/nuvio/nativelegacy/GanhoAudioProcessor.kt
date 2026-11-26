package space.nuvio.nativelegacy

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** One floating-point mix and one output quantization. The renderer thread owns
 * all filter state. Only the requested controls cross threads. No lookahead. */
@androidx.annotation.OptIn(UnstableApi::class)
class GanhoAudioProcessor : BaseAudioProcessor() {
    @Volatile var ganho = 1f
    @Volatile var amplificacaoDb = 0
    @Volatile var baixoDb = 0
    @Volatile var centroDb = 0
    @Volatile var noiteNivel = 0
    @Volatile var stereo = false

    private var frame = FloatArray(0)
    private var z1 = DoubleArray(0)
    private var z2 = DoubleArray(0)
    private var level = 1f
    private var center = 1f
    private var bass = 0f
    private var initial = true
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private val dynamics = PlaybackDynamics()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val f = inputAudioFormat
        if (f.encoding != C.ENCODING_PCM_16BIT && f.encoding != C.ENCODING_PCM_FLOAT)
            return AudioProcessor.AudioFormat.NOT_SET
        return if (stereo && f.channelCount in 3..8)
            AudioProcessor.AudioFormat(f.sampleRate, 2, f.encoding) else f
    }

    override fun onFlush() {
        val count = inputAudioFormat.channelCount.coerceAtLeast(0)
        frame = FloatArray(count)
        z1 = DoubleArray(count)
        z2 = DoubleArray(count)
        bass = 0f
        initial = true
        dynamics.reset(inputAudioFormat.sampleRate.coerceAtLeast(1))
    }

    // RBJ 110 Hz low shelf with independent per-channel filter history.
    private fun setShelf(db: Float, rate: Int) {
        if (db == bass) return
        bass = db
        if (db == 0f) {
            z1.fill(0.0)
            z2.fill(0.0)
            return
        }
        val a = 10.0.pow(db / 40.0)
        val w = 2.0 * PI * 110 / rate
        val c = cos(w)
        val s = sqrt(2.0 * a) * sin(w)
        val d = a + 1 + (a - 1) * c + s
        b0 = a * (a + 1 - (a - 1) * c + s) / d
        b1 = 2 * a * (a - 1 - (a + 1) * c) / d
        b2 = a * (a + 1 - (a - 1) * c - s) / d
        a1 = -2 * (a - 1 + (a + 1) * c) / d
        a2 = (a + 1 + (a - 1) * c - s) / d
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val f = inputAudioFormat
        val outputs = outputAudioFormat.channelCount
        val fold = outputs < f.channelCount
        val float = f.encoding == C.ENCODING_PCM_FLOAT
        val frames = inputBuffer.remaining() / f.bytesPerFrame
        val out = replaceOutputBuffer(frames * outputAudioFormat.bytesPerFrame).order(ByteOrder.nativeOrder())
        val src = inputBuffer.order(ByteOrder.nativeOrder())
        val gainTarget = ganho * linear(amplificacaoDb.coerceIn(0, 10))
        val centerTarget = linear(centroDb.coerceIn(-10, 30))
        val bassTarget = baixoDb.coerceIn(-12, 6).toFloat()
        val night = noiteNivel.coerceIn(0, 3)
        // A persistent processor permits live changes without rebuilding the sink.
        if (initial) {
            level = gainTarget
            center = centerTarget
            setShelf(bassTarget, f.sampleRate)
            initial = false
        }
        if (!fold && gainTarget == 1f && level == 1f && centerTarget == 1f && center == 1f &&
            bassTarget == 0f && bass == 0f && night == 0 && dynamics.idle()) {
            out.put(src)
            out.flip()
            return
        }
        val slew = 1f - exp(-1f / (0.005f * f.sampleRate))
        val shelfSlew = 1f - exp(-16f / (0.005f * f.sampleRate))
        repeat(frames) { index ->
            level = approach(level, gainTarget, slew)
            center = approach(center, centerTarget, slew)
            if (index % 16 == 0) setShelf(approach(bass, bassTarget, shelfSlew), f.sampleRate)
            for (ch in frame.indices) {
                frame[ch] = if (float) src.float.let { if (it.isFinite()) it else 0f } else src.short / 32768f
            }
            if (frame.size in 3..8 && frame.size != 4) frame[2] *= center
            if (fold) AudioRouteP5D.foldStereo(frame)
            var peak = 0f
            for (ch in 0 until outputs) {
                var v = frame[ch]
                if (bass != 0f) {
                    val y = b0 * v + z1[ch]
                    z1[ch] = b1 * v - a1 * y + z2[ch]
                    z2[ch] = b2 * v - a2 * y
                    v = y.toFloat()
                }
                frame[ch] = v * level
                peak = max(peak, abs(frame[ch]))
            }
            val scale = dynamics.gain(peak, night)
            for (ch in 0 until outputs) {
                val v = frame[ch] * scale
                if (float) out.putFloat(v.coerceIn(-1f, 1f))
                else out.putShort((v * 32768f).roundToInt().coerceIn(-32768, 32767).toShort())
            }
        }
        out.flip()
    }

    private fun linear(db: Int) = if (db == 0) 1f else 10.0.pow(db / 20.0).toFloat()
    private fun approach(value: Float, target: Float, step: Float) =
        if (abs(value - target) < 0.0001f) target else value + (target - value) * step
}

/** NuvioTV dev 3c14893, NightModeAudioProcessor and PeakLimiter, adapted for
 * the floating-point post-mix frame. Gain and limiting always link channels. */
private class PlaybackDynamics {
    private var env = 0f
    private var gainDb = 0f
    private var lift = 1f
    private var step = 0f
    private var phase = 0
    private var ea = 0f
    private var er = 0f
    private var ga = 0f
    private var gr = 0f
    private var hold = 0
    private var holdFrames = 0
    private var releaseFrames = 1f
    private var limiter = 1f
    private var release = 1f

    fun reset(rate: Int) {
        ea = exp(-1000f / rate)
        er = exp(-5f / rate)
        ga = exp(-16f / (0.005f * rate))
        gr = exp(-16f / (0.300f * rate))
        holdFrames = (0.010f * rate).toInt()
        releaseFrames = 0.100f * rate
        env = 0f
        gainDb = 0f
        lift = 1f
        step = 0f
        phase = 0
        hold = 0
        limiter = 1f
        release = 1f
    }

    fun idle() = gainDb == 0f && lift == 1f && limiter == 1f

    fun gain(peak: Float, night: Int): Float {
        if (night != 0 || lift != 1f || gainDb != 0f) {
            env = peak + (if (peak > env) ea else er) * (env - peak)
            if (phase == 0) {
                val makeup = when (night) { 1 -> 6f; 2 -> 10f; 3 -> 15f; else -> 0f }
                val ratio = night + 1f
                val db = 20f * log10(max(env, 1e-6f))
                val over = if (night == 0) 0f else db + makeup * ratio / (ratio - 1f)
                val slope = 1f / ratio - 1f
                val reduction = when {
                    over <= -3f -> 0f
                    over < 3f -> slope * (over + 3f) * (over + 3f) / 12f
                    else -> slope * over
                }
                val target = max(0f, makeup * ((db + 65f) / 15f).coerceIn(0f, 1f) + reduction)
                gainDb = target + (if (target < gainDb) ga else gr) * (gainDb - target)
                if (night == 0 && gainDb < 0.0001f) gainDb = 0f
                step = (10f.pow(gainDb / 20f) - lift) / 16f
            }
            lift += step
            phase = (phase + 1) % 16
            if (phase == 0 && night == 0 && gainDb == 0f) {
                lift = 1f
                env = 0f
            }
        }
        val ceiling = if (night != 0) 0.891f else 32767f / 32768f
        val hot = peak * lift
        if (hot * limiter > ceiling) {
            limiter = ceiling / hot
            hold = holdFrames
            release = (1f / limiter).pow(1f / releaseFrames)
        }
        val current = limiter
        if (hold > 0) hold-- else if (limiter < 1f)
            limiter = min(1f, max(Math.nextUp(limiter), limiter * release))
        return lift * current
    }
}
