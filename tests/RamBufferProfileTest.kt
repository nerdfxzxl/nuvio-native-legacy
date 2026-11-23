package space.nuvio.nativelegacy

fun main() {
    val all = RamBufferProfile()
    val low = all.copy(minMs = 10_000, maxMs = 30_000, targetMb = 64,
        startMs = 500, rebufferMs = 1_000, backMs = 5_000, keepKeyframe = true)
    check(all.durations(false) == (50_000 to 50_000))
    check(all.durations(true) == (50_000 to 150_000))
    check(low.durations(true) == (10_000 to 30_000))
    check(low.copy(minMs = 100, maxMs = 500).durations(false) == (1_000 to 1_000))
    check(all.targetMb == -1 && all.backMs == 0 && !all.keepKeyframe)
    println("Independent RAM profiles and Media3 duration constraints PASS")
}
