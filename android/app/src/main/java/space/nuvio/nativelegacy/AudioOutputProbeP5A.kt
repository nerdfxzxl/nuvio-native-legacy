package space.nuvio.nativelegacy

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.Format

/**
 * P5-A only: probe the CURRENT Android media route. Connected outputs or
 * renderer/MediaCodec support alone are never accepted as current-route proof.
 * Standard HDMI ARC is not eARC and does not carry arbitrary multichannel PCM,
 * Dolby TrueHD or DTS-HD MA. A route advertises *possible* fidelity, not proof
 * of physical bit-perfect output; AudioTrack telemetry/P7-P8 must confirm it.
 *
 * P5-D can supply the observed AudioTrack route; direct-playback evidence
 * still requires the Android media-attributes route to match that device.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object AudioOutputProbeP5A {
    data class Observation(
        val routeType: Int,
        val routeObserved: Boolean,
        val channelCount: Int,
        val sampleRate: Int,
        val bitDepth: Int,
        val sourceBitrate: Int,
        val result: AudioFidelityDecisionP5A
    )

    private fun bitDepth(encoding: Int): Int = when (encoding) {
        C.ENCODING_PCM_8BIT -> 8
        C.ENCODING_PCM_16BIT -> 16
        C.ENCODING_PCM_24BIT -> 24
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
        else -> 0
    }

    private fun channelMask(channels: Int): Int = when (channels) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        6 -> AudioFormat.CHANNEL_OUT_5POINT1
        8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
        else -> 0
    }

    fun encoding(mime: String): Int = when (mime.lowercase()) {
        "audio/ac3", "audio/ac-3" -> AudioFormat.ENCODING_AC3
        "audio/eac3", "audio/e-ac3" -> AudioFormat.ENCODING_E_AC3
        "audio/eac3-joc" -> AudioFormat.ENCODING_E_AC3_JOC
        "audio/vnd.dts", "audio/dts" -> AudioFormat.ENCODING_DTS
        "audio/true-hd", "audio/truehd" -> AudioFormat.ENCODING_DOLBY_TRUEHD
        "audio/vnd.dts.hd", "audio/dts-hd" -> AudioFormat.ENCODING_DTS_HD
        else -> 0
    }

    private fun explicitProfile(device: AudioDeviceInfo, format: Format): Boolean {
        val channels = format.channelCount
        val hz = format.sampleRate
        if (channels <= 0 || hz <= 0) return false
        // These arrays describe the device, not playback authorization.
        // Empty means unrestricted per Android documentation, not missing data.
        val enc = device.encodings
        val rates = device.sampleRates
        val counts = device.channelCounts
        return AudioFormat.ENCODING_PCM_16BIT in enc &&
            (rates.isEmpty() || hz in rates) &&
            (counts.isEmpty() || channels in counts)
    }

    private fun mediaAttributes() = android.media.AudioAttributes.Builder()
        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE).build()

    fun mediaRoute(manager: AudioManager?): AudioDeviceInfo? =
        if (Build.VERSION.SDK_INT >= 33) try {
            manager?.getAudioDevicesForAttributes(mediaAttributes())?.singleOrNull()
        } catch (_: RuntimeException) { null } else null

    fun inspect(ctx: Context?, fmt: Format, decoder: Boolean, dsp: Boolean,
                observed: AudioDeviceInfo? = null, actualRoute: Boolean = false): Observation {
        val channels = fmt.channelCount.coerceAtLeast(0)
        val hz = fmt.sampleRate.coerceAtLeast(0)
        val bits = bitDepth(fmt.pcmEncoding)
        val bitrate = fmt.bitrate.coerceAtLeast(0)
        val mime = fmt.sampleMimeType.orEmpty()
        val manager = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val attrs = mediaAttributes()
        val policyRoute = mediaRoute(manager)
        val route = if (actualRoute) observed else policyRoute
        val type = route?.type ?: -1
        val arc = type == AudioDeviceInfo.TYPE_HDMI_ARC
        // Standard ARC physically supports stereo LPCM, not 5.1/7.1 LPCM;
        // don't infer eARC from a 5.1 decoder or an HDMI port.
        val pcmRoute = route != null && channels in 1..8 &&
            (!arc || channels <= 2) && explicitProfile(route, fmt)
        val enc = encoding(mime)
        val mask = channelMask(channels)
        val hdSource = mime.contains("truehd", ignoreCase = true) ||
            mime.contains("true-hd", ignoreCase = true) ||
            mime.contains("dts.hd", ignoreCase = true) ||
            mime.contains("dts-hd", ignoreCase = true)
        // A generic HDMI route can conceal a TV -> ARC downstream connection.
        // Never assert HD encoded delivery without an explicitly observed eARC
        // path; decoder presence or HDMI ports are not an eARC guarantee.
        val hdTransportProven = type == AudioDeviceInfo.TYPE_HDMI_EARC
        val canBitstream = if (Build.VERSION.SDK_INT < 33 || route == null ||
            policyRoute?.id != route.id || enc == 0 || mask == 0 ||
            hz <= 0 || enc !in route.encodings || dsp ||
            (hdSource && !hdTransportProven)) false else try {
            val af = AudioFormat.Builder().setEncoding(enc).setSampleRate(hz)
                .setChannelMask(mask).build()
            val support = AudioManager.getDirectPlaybackSupport(af, attrs)
            support and AudioManager.DIRECT_PLAYBACK_BITSTREAM_SUPPORTED != 0
        } catch (_: RuntimeException) { false }
        val decision = AudioFidelityP5A.assess(
            mime, channels, hz, bits, bitrate, decoder, pcmRoute, canBitstream,
            arc, dsp)
        return Observation(type, route != null, channels, hz, bits, bitrate, decision)
    }
}
