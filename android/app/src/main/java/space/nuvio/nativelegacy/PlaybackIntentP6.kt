package space.nuvio.nativelegacy

/** Main-looper policy: a background return is not an automatic retry.
 * Request tickets also cover an open waiting for asynchronous decoder release. */
internal class PlaybackIntentP6 {
    private var background = false
    private var parkedRequest: Int? = null
    private var commandRequest: Int? = null
    private var commandedPause = false
    private var hold = false

    val systemPaused: Boolean get() = background || hold

    fun onBackground(request: Int) {
        background = true
        hold = true
        parkedRequest = request
        commandRequest = null
    }

    fun onForeground(request: Int) {
        if (background) parkedRequest = request
        background = false
    }

    fun command(request: Int, pause: Boolean): Boolean {
        if (background) return true
        commandRequest = request
        commandedPause = pause
        if (!pause) hold = false
        return pause
    }

    fun opening(request: Int, recovery: Boolean, reenter: Boolean, priorPaused: Boolean, forcedPause: Boolean = false): Boolean {
        if (background) return true
        if (commandRequest == request) return commandedPause
        if (forcedPause) { hold = true; parkedRequest = request; return true }
        if (hold && (parkedRequest == request || recovery || reenter)) return true
        if (!recovery && !reenter) hold = false
        return reenter && priorPaused
    }

    fun reset() {
        background = false
        parkedRequest = null
        commandRequest = null
        commandedPause = false
        hold = false
    }
}

/** Track ordinals are not portable across native openings or pending Kotlin requests. */
internal fun trackCommandOwnedP7(posted: Int, latest: Int, active: Int, owner: Int, activeOwner: Int): Boolean =
    posted == latest && posted == active && owner != 0 && owner == activeOwner
