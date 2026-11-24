import space.nuvio.nativelegacy.RecriaHdr

fun main() {
    val p = RecriaHdr
    check(p.ownsTransition(1, 1, 2, 2, true, true))
    check(!p.ownsTransition(1, 2, 2, 2, true, true))
    check(!p.ownsTransition(1, 1, 2, 3, true, true))
    check(!p.ownsTransition(1, 1, 2, 2, false, true))
    check(!p.ownsTransition(1, 1, 2, 2, true, false))
    check(p.ownsRenderedFrame(true, true, 100, 100))
    check(!p.ownsRenderedFrame(true, true, 99, 100))
    check(!p.ownsRenderedFrame(false, true, 101, 100))
    check(!p.ownsRenderedFrame(true, false, 101, 100))
    check(p.mayRecoverDetach(false, true))
    check(!p.mayRecoverDetach(true, true))
    check(!p.mayRecoverDetach(false, false))
    println("P5-C surface transition: PASS (12 production Kotlin assertions)")
}
