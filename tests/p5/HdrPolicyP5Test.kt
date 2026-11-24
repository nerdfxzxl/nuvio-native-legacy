import space.nuvio.nativelegacy.HdrPolicyP5

fun main() {
    fun checkMode(mode: String, dv: Int, mimeDv: Boolean, decoderDv: Boolean,
                  pq: Boolean, hlg: Boolean, base: Boolean = false) {
        val actual = HdrPolicyP5.decide(mimeDv, decoderDv, pq, hlg, base)
        check(actual.mode == mode && actual.dolbyVision == dv) {
            "HDR decision unexpected for mimeDv=$mimeDv decoderDv=$decoderDv pq=$pq hlg=$hlg base=$base: $actual"
        }
    }
    checkMode("DolbyVision", 1, true, true, true, false)
    checkMode("DolbyVision", 1, true, true, false, true)
    checkMode("none", 0, true, false, true, false) // no invented HDR10 fallback
    checkMode("none", 0, true, false, false, true) // not an HLG source
    checkMode("none", 0, true, false, false, false)
    checkMode("HDR10", 0, true, false, true, false, true) // verified same-file base ONLY
    checkMode("none", 0, true, false, false, false, true)
    checkMode("HDR10", 0, false, false, true, false)
    checkMode("HLG", 0, false, false, false, true)
    checkMode("none", 0, false, false, false, false)
    val p = HdrPolicyP5
    check(p.failure(4001, true, false) == p.FAIL_DECODER)
    check(p.failure(4003, true, false) == p.FAIL_DECODER)
    check(p.failure(4004, true, false) == p.FAIL_FORMAT)
    check(p.failure(4005, true, false) == p.FAIL_FORMAT)
    check(p.failure(6008, false, false) == p.FAIL_DRM)
    check(p.failure(4001, false, true) == 0)
    check(p.failure(6008, false, true) == 0)
    check(p.failure(2002, false, false) == 0)
    check(p.compatibleDolby(true,true,true))
    check(!p.compatibleDolby(false,true,true))
    check(!p.compatibleDolby(true,false,true))
    check(!p.compatibleDolby(true,true,false))
    println("P5 Q37/Q39 HDR and video failure production Kotlin replay: 22/22 PASS")
}
