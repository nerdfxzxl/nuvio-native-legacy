package space.nuvio.nativelegacy

/**
 * P2-B: conservative, platform-independent eligibility checks for a temporary
 * SDR frame hold. This never authorizes a new decoder, HDR conversion or source
 * substitution. A negative result means keep Media3's hardware plane as-is.
 */
internal object FrameHoldP2B {
    fun mayCopy(
        renderedFrame: Boolean,
        surfaceValid: Boolean,
        sameSession: Boolean,
        hdrSignalled: Boolean,
        dolbyDecoder: Boolean,
        hdrTransfer: Boolean
    ): Boolean = renderedFrame && surfaceValid && sameSession &&
        !hdrSignalled && !dolbyDecoder && !hdrTransfer

    // bufferedPosition is only a single contiguous endpoint, not a complete
    // range map. A forward seek *past* it is definitely out of that range.
    // Never claim a backward/inside-range seek is buffered from this reading.
    fun beyondKnownBuffer(targetMs: Long, positionMs: Long, bufferedMs: Long, live: Boolean): Boolean =
        !live && targetMs > positionMs && bufferedMs >= positionMs &&
            targetMs > bufferedMs

    fun ownsResult(
        capturedRequest: Int, currentRequest: Int,
        capturedSession: Int, currentSession: Int,
        playerMatches: Boolean, surfaceMatches: Boolean
    ): Boolean = capturedRequest == currentRequest && capturedSession == currentSession &&
        playerMatches && surfaceMatches
}
