package space.nuvio.nativelegacy

/** Main-looper route transaction policy. Device inventory is never a route key. */
internal object AudioRouteP5D {
    const val DEBOUNCE_MS = 750L
    const val FLAP_LIMIT_MS = 5000L
    const val RECOVERY_LIMIT_MS = 8000L
    data class Key(val id: Int, val type: Int, val encodings: List<Int>,
                   val rates: List<Int>, val channels: List<Int>, val masks: List<Int>,
                   val directProfiles: List<String> = emptyList())
    // Only the main looper commits a mode edge. The audio renderer reads
    // the committed value until its current Q49 transaction finishes.
    class OutputPolicy(initialPcm: Boolean) {
        @Volatile var pcm = initialPcm
            private set
        private var requested = initialPcm
        private var since = 0L
        fun request(next: Boolean, now: Long) {
            if (next != requested) { requested = next; since = now }
        }
        fun apply(now: Long, busy: Boolean): Boolean {
            if (busy || pcm == requested || now - since < 120L) return false
            pcm = requested
            return true
        }
    }

    fun needsStereo(channels: Int, standardArc: Boolean, supported: IntArray?, sinkFailed: Boolean) =
        channels in 3..8 && (standardArc || sinkFailed ||
            (supported != null && supported.isNotEmpty() && 2 in supported && channels !in supported))

    // Unflagged commentary/descriptive/dub tracks often carry the role only
    // in a Matroska title. Unknown differing titles need an explicit choice.
    private val technicalLabel = Regex(
        """(?i)\b(?:dolby|digital|digitalplus|truehd|atmos|dts|hd|ma|master|audio|pcm|lpcm|ac3|eac3|aac|flac|stereo|mono|surround|ch|channels?|kbps|khz|bit)\b|[\d\s.()_/+:\-]+"""
    )
    fun sameAudioRole(role: Int, label: String?, otherRole: Int, otherLabel: String?): Boolean =
        role == otherRole && technicalLabel.replace(label.orEmpty().lowercase(), "") ==
            technicalLabel.replace(otherLabel.orEmpty().lowercase(), "")

    fun fallbackSampleRate(input: Int, rates: IntArray?): Int {
        if (input <= 0 || rates == null || rates.isEmpty() || input in rates) return -1
        return listOf(48000, 44100).firstOrNull { it in rates } ?: -1
    }

    enum class Action { KEEP, RESELECT_PCM, RESELECT_ENCODED, ERROR }
    fun plan(faithful: Boolean, pcmCompatible: Boolean, encodedCompatible: Boolean,
             currentCompatible: Boolean, currentPcm: Boolean): Action {
        if (!faithful) return Action.ERROR
        if (currentCompatible && (!currentPcm || !encodedCompatible)) return Action.KEEP
        if (encodedCompatible) return Action.RESELECT_ENCODED
        if (pcmCompatible) return Action.RESELECT_PCM
        return Action.ERROR
    }

    /** Validate the concrete sink configuration independently of prospective support. */
    fun validate(pcm: Boolean, pcmFaithful: Boolean, encodedFaithful: Boolean,
                 sourceRate: Int, sourceChannels: Int, sourceBits: Int,
                 sinkRate: Int, sinkChannels: Int, sinkBits: Int, exactEncoding: Boolean): Boolean =
        if (pcm) pcmFaithful && sourceRate > 0 && sourceRate == sinkRate &&
            sourceChannels > 0 && sourceChannels == sinkChannels &&
            sinkBits > 0 && (sourceBits <= 0 || sinkBits >= sourceBits)
        else encodedFaithful && exactEncoding

    // AudioTrack initialization is real output evidence, unlike a prospective
    // route profile. PCM may be downmixed/resampled by Android; do not call
    // this bit-perfect. Encoded delivery still requires route evidence.
    fun playableOutput(pcm: Boolean, decoder: Boolean, encodedCompatible: Boolean,
                       sinkRate: Int, sinkChannels: Int, sinkBits: Int, exactEncoding: Boolean): Boolean =
        if (pcm) decoder && sinkRate in 8000..384000 && sinkChannels in 1..16 && sinkBits > 0
        else encodedCompatible && exactEncoding

    /** Conventional PCM interleaving, normalized to avoid clipping.
     * Pack two signed PCM16 samples in a single Int to avoid per-frame allocation. */
    fun stereoMix(samples: IntArray): Int {
        val channels = samples.size
        require(channels in 3..8)
        var left = samples[0].toFloat()
        var right = samples[1].toFloat()
        if (channels == 4) {
            left += .7071f * samples[2]; right += .7071f * samples[3]
        } else {
            left += .7071f * samples[2]; right += .7071f * samples[2]
            val surround = if (channels >= 6) 4 else 3
            if (channels >= 6) {
                left += .25f * samples[3]; right += .25f * samples[3]
            }
            if (surround + 1 < channels) {
                left += .7071f * samples[surround]; right += .7071f * samples[surround + 1]
            }
            if (channels == 7) {
                left += .5f * samples[6]; right += .5f * samples[6]
            } else if (channels == 8) {
                left += .5f * samples[6]; right += .5f * samples[7]
            }
        }
        val norm = if (channels <= 4) 1.7071f else if (channels <= 6) 2.6642f else 3.1642f
        val l = kotlin.math.round(left / norm).toInt().coerceIn(-32768, 32767)
        val r = kotlin.math.round(right / norm).toInt().coerceIn(-32768, 32767)
        return (l shl 16) or (r and 65535)
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

    /** At most two audio-only reselects (second only for a newly stable route)
     * and, only following a fatal renderer
     * error, one same-player prepare. Never a new media item or source. */
    class Transaction {
        private var serial = 0L
        var ticket = 0L
            private set
        var deadline = 0L
            private set
        private var reselections = 0
        var attemptedPrepare = false
            private set
        val active get() = ticket != 0L
        fun begin(now: Long): Long {
            check(!active)
            ticket = ++serial
            deadline = now + RECOVERY_LIMIT_MS
            attemptedPrepare = false
            reselections = 0
            return ticket
        }
        fun owns(t: Long) = active && ticket == t
        fun expired(now: Long) = active && now >= deadline
        fun allowReselect(t: Long): Boolean {
            if (!owns(t) || reselections >= 2) return false
            reselections++
            return true
        }
        fun allowPrepare(t: Long): Boolean {
            if (!owns(t) || attemptedPrepare) return false
            attemptedPrepare = true
            return true
        }
        fun cancel() { ticket = 0L }
    }
}
