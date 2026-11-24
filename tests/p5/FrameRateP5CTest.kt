package space.nuvio.nativelegacy

fun main() {
    val current = FrameRateP5C.Mode(1, 3840, 2160, 60f, setOf(1,2))
    val modes = listOf(current, FrameRateP5C.Mode(2,3840,2160,24f,setOf(2)),
        FrameRateP5C.Mode(3,3840,2160,23.976f,setOf(1,2)),
        FrameRateP5C.Mode(4,1920,1080,24f,setOf(1,2)))
    check(FrameRateP5C.choose(false,24f,current,modes,null,true) == null)
    check(FrameRateP5C.choose(true,30f,current,modes,null,true) == null)
    check(FrameRateP5C.choose(true,24f,current,modes,null,true) == 2)
    check(FrameRateP5C.choose(true,23.976f,current,modes,1,true) == 3)
    check(FrameRateP5C.choose(true,24f,current,modes,1,false) == null)
    check(FrameRateP5C.choose(true,24f,current,listOf(modes[3]),null,true) == null)
    check(FrameRateP5C.choose(true,Float.NaN,current,modes,null,true) == null)
    check(FrameRateP5C.choose(true,25f,current,modes,null,true) == null)
    val hdr10PlusCurrent = current.copy(hdrTypes = setOf(1,2,4))
    val plainHdr = modes[1]
    val dynamicHdr = plainHdr.copy(id = 5, hdrTypes = setOf(2,4))
    check(FrameRateP5C.choose(true,24f,hdr10PlusCurrent,listOf(plainHdr),2,true,setOf(4)) == null)
    check(FrameRateP5C.choose(true,24f,hdr10PlusCurrent,listOf(plainHdr,dynamicHdr),2,true,setOf(4)) == 5)
    check(FrameRateP5C.choose(true,24f,hdr10PlusCurrent,listOf(dynamicHdr),null,true,setOf(1,2,4)) == null)
    check(FrameRateP5C.choose(true,24f,hdr10PlusCurrent,listOf(dynamicHdr),2,false,setOf(4)) == null)
    val window = Any()
    val replacement = Any()
    val lease = FrameRateP5C.PreferenceLease(window, 0, 5)
    check(lease.restoreTarget(window, 5) == 0)
    check(lease.restoreTarget(window, 2) == null)
    check(lease.restoreTarget(replacement, 5) == null)
    check(lease.restoreTarget(null, 5) == null)
    check(FrameRateP5C.PreferenceLease(window, 5, 5).restoreTarget(window, 5) == null)
    check(FrameRateP5C.PreferenceLease(window, 3, 5).restoreTarget(window, 5) == 3)
    println("P5-C supported-mode AFR and preference restoration: 18 assertions passed")
}
