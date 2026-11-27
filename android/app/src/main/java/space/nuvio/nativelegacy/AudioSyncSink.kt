package space.nuvio.nativelegacy

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

// F06: o AudioSink de sempre (DefaultAudioSink do DefaultRenderersFactory) com
// uma escuta na ENTRADA. handleBuffer traz o tempo de midia de cada buffer,
// coisa que o AudioProcessor/TeeAudioProcessor do Media3 1.8 nao da.
// Non-normal speed requires decoded PCM so Sonic can change the audio clock.
@androidx.annotation.OptIn(UnstableApi::class)
class AudioSyncSink(sink: AudioSink, private val tap: AudioSyncTap,
                    private val forcePcm: () -> Boolean = { false },
                    private val configurePcm: () -> Unit = {},
                    private val processor: GanhoAudioProcessor = GanhoAudioProcessor(),
                    private val precision: (Int) -> Boolean = { false }) : ForwardingAudioSink(sink) {
    private var processPcm = false
    private var pendingSource: ByteBuffer? = null
    private var pendingOutput: ByteBuffer? = null

    override fun supportsFormat(format: Format): Boolean =
        getFormatSupport(format) != AudioSink.SINK_FORMAT_UNSUPPORTED

    override fun getFormatSupport(format: Format): Int =
        if (forcePcm() && format.sampleMimeType != MimeTypes.AUDIO_RAW)
            AudioSink.SINK_FORMAT_UNSUPPORTED
        else super.getFormatSupport(format)

    override fun getFormatOffloadSupport(format: Format): androidx.media3.exoplayer.audio.AudioOffloadSupport =
        if (forcePcm()) androidx.media3.exoplayer.audio.AudioOffloadSupport.DEFAULT_UNSUPPORTED
        else super.getFormatOffloadSupport(format)

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        val pcm = MimeTypes.AUDIO_RAW == inputFormat.sampleMimeType
        if (pcm) configurePcm()
        val enc = when (inputFormat.pcmEncoding) {
            C.ENCODING_PCM_16BIT -> AudioSyncTap.ENC_PCM16
            C.ENCODING_PCM_FLOAT -> AudioSyncTap.ENC_FLOAT
            C.ENCODING_PCM_8BIT -> AudioSyncTap.ENC_PCM8
            C.ENCODING_PCM_24BIT -> AudioSyncTap.ENC_PCM24
            C.ENCODING_PCM_32BIT -> AudioSyncTap.ENC_PCM32
            else -> 0
        }
        tap.configurar(pcm, enc, inputFormat.channelCount, inputFormat.sampleRate)
        pendingSource = null
        pendingOutput = null
        processPcm = false
        if (pcm) {
            processor.channelMap = outputChannels
            processor.requirePcm16 = precision(inputFormat.pcmEncoding)
            val output = processor.configure(AudioProcessor.AudioFormat(inputFormat))
            if (output != AudioProcessor.AudioFormat.NOT_SET) {
                processor.flush()
                processPcm = true
                // Frame count, timestamps, encoder delay/padding and sample rate
                // are preserved. Only this stage applies user adjustments.
                super.configure(inputFormat.buildUpon().setPcmEncoding(output.encoding)
                    .setChannelCount(output.channelCount).build(), specifiedBufferSize, null)
                return
            }
        }
        super.configure(inputFormat, specifiedBufferSize, outputChannels)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        tap.buffer(buffer, presentationTimeUs)
        // Media3 may consume a buffer across multiple writes. Pin either the
        // original buffer or the transformed one until the sink acknowledges it,
        // even when the owner moves a slider between the two writes.
        if (pendingSource == null && (!processPcm || processor.transparent())) {
            pendingSource = buffer
        }
        if (pendingSource === buffer && pendingOutput == null) {
            val done = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
            if (done) pendingSource = null
            return done
        }
        val output = pendingOutput ?: run {
            processor.queueInput(buffer.duplicate().order(buffer.order()))
            processor.output.also { pendingOutput = it; pendingSource = buffer }
        }
        check(pendingSource === buffer)
        if (!super.handleBuffer(output, presentationTimeUs, encodedAccessUnitCount)) return false
        buffer.position(buffer.limit())
        pendingSource = null
        pendingOutput = null
        return true
    }

    override fun flush() {
        pendingSource = null
        pendingOutput = null
        if (processPcm) processor.flush()
        tap.descontinuidade()
        super.flush()
    }

    override fun reset() {
        pendingSource = null
        pendingOutput = null
        processPcm = false
        processor.reset()
        tap.descontinuidade()
        super.reset()
    }
}
