package space.nuvio.nativelegacy

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRouting
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/** Observes the AudioTrack actually created by the stock Media3 sink.
 * Device-add/remove is only a wake-up signal. No getDevices inventory inference.
 * All listener installation and callbacks are serialized on the app looper.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class AudioRouteObserverP5D(context: Context, private val main: Handler) :
    DefaultAudioSink.AudioTrackProvider {
    private val manager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    data class Output(val track: AudioTrack, val config: AudioSink.AudioTrackConfig, val serial: Long)
    private val serial = java.util.concurrent.atomic.AtomicLong()
    @Volatile var output: Output? = null
        private set
    private var listened: AudioTrack? = null
    private var changed: (() -> Unit)? = null
    private var registered = false
    private val routing = AudioRouting.OnRoutingChangedListener { router ->
        if (router === listened && output?.track === router) changed?.invoke()
    }
    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { changed?.invoke() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) { changed?.invoke() }
    }

    override fun getAudioTrack(config: AudioSink.AudioTrackConfig, attributes: AudioAttributes,
                               audioSessionId: Int, context: Context?): AudioTrack {
        val track = DefaultAudioSink.AudioTrackProvider.DEFAULT.getAudioTrack(config, attributes, audioSessionId, context)
        output = Output(track, config, serial.incrementAndGet())
        main.post { if (changed != null) { attach(); changed?.invoke() } }
        return track
    }
    fun start(callback: () -> Unit) {
        changed = callback
        if (!registered && Build.VERSION.SDK_INT >= 23) {
            try { manager.registerAudioDeviceCallback(devices, main); registered = true }
            catch (_: RuntimeException) { }
        }
        attach()
    }
    private fun attach() {
        if (Build.VERSION.SDK_INT < 24) return
        val next = output?.track
        if (next === listened) return
        try { listened?.removeOnRoutingChangedListener(routing) } catch (_: RuntimeException) {}
        listened = next
        try { next?.addOnRoutingChangedListener(routing, main) } catch (_: RuntimeException) {}
    }
    fun stop() {
        changed = null
        if (Build.VERSION.SDK_INT >= 24) {
            try { listened?.removeOnRoutingChangedListener(routing) } catch (_: RuntimeException) {}
        }
        listened = null
        if (registered) {
            try { manager.unregisterAudioDeviceCallback(devices) } catch (_: RuntimeException) {}
            registered = false
        }
    }

    /** Actual playback route wins; for a paused/uninitialized AudioTrack,
     * Android's single media-attributes route is prospective evidence only.
     * Ambiguous/unknown routes remain unknown, never use connected devices. */
    fun route(): AudioDeviceInfo? {
        val actual = if (Build.VERSION.SDK_INT >= 23) try {
            output?.track?.takeIf { it.state == AudioTrack.STATE_INITIALIZED }?.routedDevice
        } catch (_: RuntimeException) { null } else null
        return actual ?: AudioOutputProbeP5A.mediaRoute(manager)
    }
    fun key(route: AudioDeviceInfo?): AudioRouteP5D.Key? = route?.let {
        AudioRouteP5D.Key(it.id, it.type, it.encodings.distinct().sorted(),
            it.sampleRates.distinct().sorted(), it.channelCounts.distinct().sorted(),
            it.channelMasks.distinct().sorted())
    }
}
