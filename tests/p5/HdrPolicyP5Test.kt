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
    data class Decoder(val name: String, val hardware: Boolean, val software: Boolean)
    val sw1 = Decoder("sw1", false, true)
    val sw2 = Decoder("sw2", false, true)
    val hw1 = Decoder("hw1", true, false)
    val hw2 = Decoder("hw2", true, false)
    val unknown = Decoder("unknown", false, false)
    fun order(list: List<Decoder>) = p.hardwareFirst(list, { it.hardware }, { it.software })
    check(order(listOf(sw1, hw2, unknown, hw1, sw2)) == listOf(hw2, hw1, unknown, sw1, sw2))
    check(order(listOf(hw1, hw2)) == listOf(hw1, hw2))
    check(order(listOf(sw2, sw1)) == listOf(sw2, sw1))
    check(order(listOf(unknown, sw1, hw1)) == listOf(hw1, unknown, sw1))
    check(order(emptyList()).isEmpty())
    val stock = listOf(sw1, hw2, unknown, hw1, sw2)
    fun candidates(mobile: Boolean, supported: Set<Decoder>) =
        p.ordinaryDecoders(stock, mobile, { it.hardware }, { it.software }, { it in supported })
    check(candidates(true, setOf(hw1)) == stock)
    check(candidates(true, emptySet()) == stock)
    check(candidates(false, setOf(hw1)) == listOf(hw2, hw1))
    check(candidates(false, setOf(sw1)) == stock)
    check(p.ordinaryDecoders(emptyList<Decoder>(), true, { it.hardware }, { it.software }, { true }).isEmpty())
    println("P5 Q37/Q39 HDR, video failure and decoder order, mobile fallback: PASS")
}
