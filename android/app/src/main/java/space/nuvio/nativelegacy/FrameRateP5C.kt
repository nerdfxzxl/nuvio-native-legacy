package space.nuvio.nativelegacy

import kotlin.math.abs
import kotlin.math.round

internal object FrameRateP5C {
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
