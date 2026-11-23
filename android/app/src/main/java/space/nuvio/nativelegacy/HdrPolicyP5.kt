package space.nuvio.nativelegacy

/**
 * Q37: Only report a display HDR mode for which there is positive evidence.
 * A Dolby Vision MIME with ST2084 is NOT proof of a decodable HDR10 base layer.
 * The `none` return for unverified DV means "no confirmed output mode", not SDR.
 * Callers must separately fail/offer user action when no compatible decoder
 * or independently verified same-file HDR10 base-layer path exists.
 */
internal object HdrPolicyP5 {
    data class Decision(val mode: String, val dolbyVision: Int)

    fun decide(
        dolbyVisionMime: Boolean,
        dolbyVisionDecoder: Boolean,
        st2084: Boolean,
        hlg: Boolean,
        verifiedSameFileHdr10Base: Boolean = false
    ): Decision {
        if (dolbyVisionMime) {
            if (dolbyVisionDecoder) return Decision("DolbyVision", 1)
            // This must never be inferred from colorTransfer=ST2084 alone.
            if (verifiedSameFileHdr10Base && st2084) return Decision("HDR10", 0)
            return Decision("none", 0)
        }
        return when {
            st2084 -> Decision("HDR10", 0)
            hlg -> Decision("HLG", 0)
            else -> Decision("none", 0)
        }
    }
}
