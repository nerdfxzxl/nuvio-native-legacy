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
    println("P5-C supported-mode AFR: 8 assertions passed")
}
