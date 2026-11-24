package space.nuvio.nativelegacy

import kotlin.math.abs
import kotlin.math.round

internal object FrameRateP5C {
    // Own only the preference actually changed on this exact Window. A
    // replacement Activity or another component's later mode vote is not ours.
    class PreferenceLease<W : Any>(private val owner: W, private val previous: Int,
                                   private val requested: Int) {
        fun restoreTarget(currentOwner: W?, currentPreference: Int): Int? =
            if (currentOwner === owner && currentPreference == requested &&
                previous != requested) previous else null
    }

    enum class SwitchStatus { WAITING, CONFIRMED, TIMED_OUT }

    // A window preference is not evidence that the panel has switched.
    // Require a continuously observed target before releasing startup.
    class SwitchGate(private val target: Int, private val startedMs: Long) {
        private var observedAt: Long? = null
        private var terminal: SwitchStatus? = null

        fun observe(activeMode: Int?, nowMs: Long): SwitchStatus {
            terminal?.let { return it }
            if (nowMs < startedMs) return SwitchStatus.WAITING
            if (activeMode == target) {
                val first = observedAt
                if (first == null || nowMs < first) observedAt = nowMs
                else if (nowMs - first >= 150L && nowMs - startedMs <= 2500L) {
                    terminal = SwitchStatus.CONFIRMED
                    return SwitchStatus.CONFIRMED
                }
            } else observedAt = null
            if (nowMs - startedMs >= 2500L) {
                terminal = SwitchStatus.TIMED_OUT
                return SwitchStatus.TIMED_OUT
            }
            return SwitchStatus.WAITING
        }
    }

    data class Mode(val id: Int, val width: Int, val height: Int, val hz: Float,
                    val hdrTypes: Set<Int> = emptySet())
    fun choose(enabled: Boolean, fps: Float, current: Mode, supported: List<Mode>,
               requiredHdr: Int?, hdrKnown: Boolean, preserveHdrTypes: Set<Int> = emptySet()): Int? {
        if (!enabled || !fps.isFinite() || fps !in 10f..240f) return null
        if ((requiredHdr != null || preserveHdrTypes.isNotEmpty()) && !hdrKnown) return null
        fun error(mode: Mode): Float {
            val n = round(mode.hz / fps)
            return if (n >= 1f && mode.hz.isFinite()) abs(mode.hz - fps * n) else Float.POSITIVE_INFINITY
        }
        fun suitable(mode: Mode) = mode.width == current.width && mode.height == current.height &&
            (requiredHdr == null || requiredHdr in mode.hdrTypes) &&
            mode.hdrTypes.containsAll(preserveHdrTypes) && error(mode) <= 0.06f
        if (suitable(current)) return null
        return supported.filter(::suitable).minWithOrNull(compareBy<Mode> { error(it) }
            .thenBy { abs(it.hz - current.hz) })?.id
    }
}
