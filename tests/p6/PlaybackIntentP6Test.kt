package space.nuvio.nativelegacy

fun main() {
    var n = 0
    fun ok(value: Boolean) { check(value) { "P6 intent assertion ${n+1}" }; n++ }
    val p = PlaybackIntentP6()
    ok(!p.opening(1, false, false, false))
    ok(p.command(1, true))
    ok(!p.opening(2, true, false, true)) // Q22 ordinary previous pause
    ok(p.command(2, true))
    ok(p.opening(2, true, false, false)) // newer pause before retry opens
    p.onBackground(2)
    ok(p.systemPaused)
    ok(p.opening(2, true, false, false))
    ok(p.opening(3, false, false, false)) // pending source while hidden
    ok(p.command(3, false)) // no background autoplay
    p.onForeground(3)
    ok(p.systemPaused)
    ok(p.opening(3, false, false, false)) // delayed open after resume
    ok(p.opening(4, true, false, false)) // recovery after Home
    ok(p.opening(4, true, true, false)) // decoder reentry after Home
    ok(!p.command(4, false))
    ok(!p.systemPaused)
    ok(!p.opening(4, true, false, true))
    p.onBackground(4)
    p.onForeground(4)
    ok(p.opening(4, false, false, false))
    ok(!p.opening(5, false, false, true)) // new selected source
    ok(!p.systemPaused)
    ok(p.command(5, true))
    ok(!p.opening(6, false, false, true)) // old source's command not reused
    p.onBackground(6)
    p.onBackground(7)
    p.onForeground(7)
    p.onForeground(7)
    ok(p.opening(7, false, false, false))
    ok(p.command(7, true))
    ok(p.opening(7, true, false, false))
    p.reset()
    ok(!p.systemPaused)
    ok(!p.opening(8, false, false, true))
    p.onBackground(8)
    p.onForeground(8)
    ok(p.opening(9, false, false, false, forcedPause = true))
    ok(p.systemPaused)
    ok(p.opening(10, true, false, false))
    ok(!p.command(9, false))
    ok(!p.opening(9, false, false, true, forcedPause = true))
    ok(!p.systemPaused)
    ok(!p.opening(11, false, false, true))
    println("P6 playback intent policy: $n assertions PASS")
}
