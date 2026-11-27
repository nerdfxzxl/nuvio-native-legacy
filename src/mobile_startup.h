#ifndef NV_MOBILE_STARTUP_H
#define NV_MOBILE_STARTUP_H
/* Preserve the TV deadline. A phone may keep opening while real socket
 * bytes are arriving; no extension for unknown/stale/zero throughput.
 * The absolute cap prevents a trickle from hanging the player forever.
 * Genuine errors and stable frames are handled by the existing caller. */
static inline unsigned nv_mobile_startup_deadline(int mobile, unsigned normal,
                                                   double bytes_per_second, int loading) {
    return mobile && loading && bytes_per_second > 0.0 && normal < 90000u
        ? 90000u : normal;
}
#endif
