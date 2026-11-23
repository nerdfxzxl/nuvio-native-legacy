package space.nuvio.nativelegacy

/** One immutable snapshot per open, including a silent preparation that may be adopted. */
internal data class RamBufferProfile(
    val minMs: Int = 50_000,
    val maxMs: Int = 0,
    val startMs: Int = 2_500,
    val rebufferMs: Int = 5_000,
    val backMs: Int = 0,
    val keepKeyframe: Boolean = false,
    val preferTime: Boolean = false,
    val targetMb: Int = -1,
) {
    fun durations(manualTarget: Boolean): Pair<Int, Int> {
        val min = maxOf(minMs, startMs, rebufferMs)
        val max = if (maxMs > 0) maxMs else if (manualTarget) 150_000 else 50_000
        return min to maxOf(min, max)
    }
}
