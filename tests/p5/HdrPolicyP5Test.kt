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
    println("P5 Q37 HDR source-policy Kotlin replay: 10/10 PASS")
}
