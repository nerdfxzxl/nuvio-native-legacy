package space.nuvio.nativelegacy

/** Host-only Kotlin regression suite; no Android or Gradle/SDK required. */
fun main() {
    val deadline = FrameHoldP2B.ReadyDeadline()
    check(!deadline.expired(true, 0) && !deadline.expired(false, 15000))
    check(!deadline.expired(true, 30000) && !deadline.expired(true, 45999))
    check(deadline.expired(true, 46000) && !deadline.expired(true, 46001))
    val p = FrameHoldP2B
    for ((width, height, aspect) in listOf(Triple(1920, 1080, 1f), Triple(3840, 1606, 1f),
            Triple(720, 576, 16f / 15f), Triple(1080, 1920, 1f))) {
        val valid = p.displayWidth(width, height, aspect)
        check(valid == (width * aspect + .5f).toInt())
        check((p.displayWidth(0, 0, 1f) ?: valid) == valid)
        check(p.displayWidth(width, height, Float.NaN) == null)
    }
    check(p.mayCopy(true, true, true, false, false, false))
    check(!p.mayCopy(false, true, true, false, false, false))
    check(!p.mayCopy(true, false, true, false, false, false))
    check(!p.mayCopy(true, true, false, false, false, false))
    check(!p.mayCopy(true, true, true, true, false, false))
    check(!p.mayCopy(true, true, true, false, true, false))
    check(!p.mayCopy(true, true, true, false, false, true))
    check(p.beyondKnownBuffer(42_000, 10_000, 20_000, false))
    check(!p.beyondKnownBuffer(15_000, 10_000, 20_000, false))
    check(!p.beyondKnownBuffer(5_000, 10_000, 20_000, false))
    check(!p.beyondKnownBuffer(42_000, 10_000, 20_000, true))
    check(!p.beyondKnownBuffer(42_000, 10_000, 9_000, false))
    check(p.ownsResult(3, 3, 18, 18, true, true))
    check(!p.ownsResult(3, 4, 18, 18, true, true))
    check(!p.ownsResult(3, 3, 18, 19, true, true))
    check(!p.ownsResult(3, 3, 18, 18, false, true))
    check(!p.ownsResult(3, 3, 18, 18, true, false))
    println("P2-B frame-hold policy: PASS including geometry")
}
