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
    check(p.needsTclWorkaround("TCL", "TCL", listOf("c2.mtk.dvhe.decoder")))
    check(p.needsTclWorkaround("unknown", "tcl", listOf("OMX.MTK.VIDEO.DECODER.HEVC")))
    check(!p.needsTclWorkaround("Xiaomi", "Xiaomi", listOf("c2.mtk.hevc.decoder")))
    check(!p.needsTclWorkaround("TCL", "TCL", listOf("c2.android.hevc.decoder")))
    check(!p.needsTclWorkaround("TCL", "TCL", listOf("c2.mtk.hevc.decoder", "OMX.MS.HEVC")))
    check(!p.needsTclWorkaround("TCL", "TCL", listOf("c2.mtk.hevc.decoder", "c2.amlogic.hevc.decoder")))
    check(!p.needsTclWorkaround("", "", emptyList()))
    println("P5-C surface transition: PASS (19 production Kotlin assertions)")
}
