package space.nuvio.nativelegacy

/** Main-looper route transaction policy. Device inventory is never a route key. */
internal object AudioRouteP5D {
    const val DEBOUNCE_MS = 750L
    const val FLAP_LIMIT_MS = 5000L
    const val RECOVERY_LIMIT_MS = 8000L
    data class Key(val id: Int, val type: Int, val encodings: List<Int>,
                   val rates: List<Int>, val channels: List<Int>, val masks: List<Int>)
    enum class Action { KEEP, RESELECT_PCM, RESELECT_ENCODED, ERROR }
    fun plan(faithful: Boolean, pcmCompatible: Boolean, encodedCompatible: Boolean,
             currentCompatible: Boolean, currentPcm: Boolean): Action {
        if (!faithful) return Action.ERROR
        if (currentCompatible && (!currentPcm || !encodedCompatible)) return Action.KEEP
        if (encodedCompatible) return Action.RESELECT_ENCODED
        if (pcmCompatible) return Action.RESELECT_PCM
        return Action.ERROR
    }

    /** Duplicate samples do not extend the quiet period or absolute deadline. */
    class Debounce {
        private var initialized = false
        private var stable: Key? = null
        private var candidate: Key? = null
        private var since = 0L
        private var started = 0L
        var pending = false
            private set
        fun seed(key: Key?) {
            initialized = key != null
            stable = key
            pending = false
        }
        enum class Result { NONE, WAIT, CHANGED, UNSTABLE }
        fun observe(key: Key?, now: Long): Result {
            if (!initialized) {
                if (key != null) seed(key)
                return Result.NONE
            }
            if (key == stable) {
                pending = false
                return Result.NONE
            }
            if (!pending) {
                started = now
                since = now
                candidate = key
                pending = true
            } else if (candidate != key) {
                candidate = key
                since = now
            }
            if (now - started >= FLAP_LIMIT_MS) {
                pending = false
                return Result.UNSTABLE
            }
            if (now - since < DEBOUNCE_MS) return Result.WAIT
            stable = candidate
            pending = false
            return Result.CHANGED
        }
    }

    /** Exactly one audio-only reselect and, only following a fatal renderer
     * error, one same-player prepare. Never a new media item or source. */
    class Transaction {
        private var serial = 0L
        var ticket = 0L
            private set
        var deadline = 0L
            private set
        var attemptedPrepare = false
            private set
        val active get() = ticket != 0L
        fun begin(now: Long): Long {
            check(!active)
            ticket = ++serial
            deadline = now + RECOVERY_LIMIT_MS
            attemptedPrepare = false
            return ticket
        }
        fun owns(t: Long) = active && ticket == t
        fun expired(now: Long) = active && now >= deadline
        fun allowPrepare(t: Long): Boolean {
            if (!owns(t) || attemptedPrepare) return false
            attemptedPrepare = true
            return true
        }
        fun cancel() { ticket = 0L }
    }
}
