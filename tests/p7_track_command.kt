package space.nuvio.nativelegacy

fun main() {
    check(trackCommandOwnedP7(10, 10, 10, 100, 100))
    // Old command queued before source replacement.
    check(!trackCommandOwnedP7(10, 11, 11, 100, 101))
    // Old callback sees the newly incremented request counter before opening.
    check(!trackCommandOwnedP7(11, 11, 10, 100, 100))
    // Same old callback executes after the replacement becomes active.
    check(!trackCommandOwnedP7(11, 11, 11, 100, 101))
    // New native opening may advance even with request counter unchanged.
    check(!trackCommandOwnedP7(10, 10, 10, 100, 101))
    check(!trackCommandOwnedP7(11, 12, 11, 101, 101))
    check(!trackCommandOwnedP7(11, 11, 11, 0, 0))
    check(trackCommandOwnedP7(11, 11, 11, 101, 101))
    println("P7 track command ownership: 8/8 PASS")
}
