package space.nuvio.nativelegacy

/**
 * P2-B: conservative, platform-independent eligibility checks for a temporary
 * SDR frame hold. This never authorizes a new decoder, HDR conversion or source
 * substitution. A negative result means keep Media3's hardware plane as-is.
 */
internal object FrameHoldP2B {
    // Count only continuously READY, foreground, unsuppressed video. Network
    // buffering, audio-only items and paused playback do not consume the budget.
    class ReadyDeadline {
        private var since = -1L
        fun expired(waiting: Boolean, now: Long): Boolean {
            if (!waiting) { since = -1L; return false }
            if (since < 0) since = now
            if (now - since < 16000L) return false
            since = -1L
            return true
        }
    }

    fun displayWidth(width: Int, height: Int, pixelAspect: Float): Int? {
        if (width <= 0 || height <= 0 || !pixelAspect.isFinite() || pixelAspect <= 0f) return null
        return (width * pixelAspect + 0.5f).toInt().takeIf { it > 0 }
    }

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
