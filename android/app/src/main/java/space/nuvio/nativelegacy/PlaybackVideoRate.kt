package space.nuvio.nativelegacy

/**
 * Bounded decoder-input sample telemetry, in media time, not download time.
 * A reset owns each new seek/renderer generation. No access to sample bytes.
 */
internal class PlaybackVideoRate {
    data class Reading(val bps: Double, val fps: Double)
    private val pts = LongArray(2048)
    private val sizes = IntArray(2048)
    private var next = 0
    private var count = 0
    @Synchronized fun reset() { next=0; count=0 }
    @Synchronized fun sample(timeUs: Long, bytes: Int) {
        if (timeUs<0 || bytes<=0) return
        pts[next]=timeUs; sizes[next]=bytes
        next=(next+1)%pts.size
        if(count<pts.size) count++
    }
    @Synchronized fun measure(positionUs: Long): Reading? {
        // A trailing two-second window uses samples near the actual playhead,
        // not the renderer's newest buffered/decode-ahead sample.
        val end=positionUs
        val start=end-2_000_000L
        if(start<0) return null
        var low=Long.MAX_VALUE; var high=Long.MIN_VALUE
        var bytes=0L; var frames=0
        for(i in 0 until count) {
            val t=pts[i]
            if(t in start..end) {
                low=minOf(low,t); high=maxOf(high,t); bytes+=sizes[i]; frames++
            }
        }
        if(frames<3 || high-low<1_500_000L || end-high>250_000L || low-start>250_000L) return null
        val span=(high-low).toDouble()*frames/(frames-1)
        return Reading(bytes*8_000_000.0/span,frames*1_000_000.0/span)
    }
}
