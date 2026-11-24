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

    // Stable cross-language P5-B reasons, distinct from Media3's error code.
    const val FAIL_DECODER = 1
    const val FAIL_FORMAT = 2
    const val FAIL_DRM = 3
    const val FAIL_SURFACE = 4
    const val FAIL_OUTPUT = 5

    /** Media3 1.8 PlaybackException codes; keep audio renderer errors separate. */
    fun failure(code: Int, videoRenderer: Boolean, audioRenderer: Boolean): Int {
        if (audioRenderer) return 0
        if (code in 6000..6999) return FAIL_DRM
        if (!videoRenderer) return 0
        return when (code) {
            4004, 4005 -> FAIL_FORMAT
            4001, 4002, 4003, 4006 -> FAIL_DECODER
            else -> FAIL_DECODER // renderer evidence, not an inferred network failure
        }
    }


    // Preserve Media3's ordering within each hardware class. The renderer
    // still sorts candidates by support for the actual format afterwards.
    fun <T> hardwareFirst(candidates: List<T>, hardware: (T) -> Boolean,
                          software: (T) -> Boolean): List<T> =
        candidates.sortedBy { if (hardware(it)) 0 else if (software(it)) 2 else 1 }

    fun compatibleDolby(displayDv: Boolean, profileSupported: Boolean, sizeSupported: Boolean): Boolean =
        displayDv && profileSupported && sizeSupported

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
