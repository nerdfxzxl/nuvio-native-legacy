#ifndef NV_WATCHED_P6_H
#define NV_WATCHED_P6_H
#include <math.h>

/* Only a release-owned, explicitly named closing credits chapter can use
 * marker timing. Estimates, last unnamed chapter, preview, neighbour or
 * duration-only third-party matches never confer this authority. */
static inline double p6_watched_marker(double marker, double duration, double window) {
  return isfinite(marker) && isfinite(duration) && isfinite(window) &&
         duration >= 120.0 && marker >= duration * 0.5 &&
         marker < duration && duration - marker <= window ? marker : 0.0;
}

/* Genuine viewing proof is cumulative media time, not the seek target.
 * A trusted previous same-duration checkpoint may supply the already watched
 * prefix. A valid credits marker takes precedence over the user fallback. */
static inline int p6_watched_reached(double position, double duration,
                                    double marker, int percent,
                                    double base, double played) {
  if (!isfinite(position) || !isfinite(duration) || !isfinite(marker) ||
      !isfinite(base) || !isfinite(played) || duration < 120.0 ||
      position < 0.0 || position > duration + 1.0 ||
      percent < 70 || percent > 98 || base < 0.0 || base > duration ||
      played < 1.0) return 0;
  double target = marker > 0.0 && marker < duration ? marker : duration * percent / 100.0;
  return position >= target && base + played >= 120.0 &&
         played + 3.0 >= 0.9 * fmax(0.0, position - base);
}
#endif
