package space.nuvio.nativelegacy

/** Main-looper slot for a selected preload waiting for decoder teardown.
 * Taking transfers ownership to the opener; cancellation transfers it to disposal.
 * A stale ticket can never dispose a newer resource. */
internal class PendingAdoptionP6<T : Any>(private val dispose: (T) -> Unit) {
    private data class Entry<T>(val request: Int, val value: T)
    private var pending: Entry<T>? = null

    fun offer(request: Int, value: T) {
        if (owns(request, value)) return
        cancelAll()
        pending = Entry(request, value)
    }
    fun owns(request: Int, value: T): Boolean =
        pending?.let { it.request == request && it.value === value } == true
    fun take(request: Int): T? {
        val entry = pending ?: return null
        if (entry.request != request) return null
        pending = null
        return entry.value
    }
    fun cancel(request: Int) { take(request)?.let(dispose) }
    fun cancelExcept(request: Int) {
        if (pending?.request != request) cancelAll()
    }
    fun cancelAll() {
        val entry = pending ?: return
        pending = null
        dispose(entry.value)
    }
}
