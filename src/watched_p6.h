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

/* Track disjoint viewed intervals, not a sum that can count one scene many
 * times. A bounded full set drops new disjoint spans conservatively. */
#define P6_VIEW_SPANS 64
typedef struct {
  double start[P6_VIEW_SPANS], end[P6_VIEW_SPANS];
  unsigned n;
} P6Viewed;
static inline void p6_view_add(P6Viewed *v, double start, double end) {
  if (!isfinite(start) || !isfinite(end) || end <= start || end <= 0.0) return;
  if (start < 0.0) start = 0.0;
  unsigned first = 0, last;
  while (first < v->n && v->end[first] < start) first++;
  last = first;
  while (last < v->n && v->start[last] <= end) {
    start = fmin(start, v->start[last]);
    end = fmax(end, v->end[last]);
    last++;
  }
  if (first == last && v->n == P6_VIEW_SPANS) return;
  if (last > first) {
    for (unsigned i = last; i < v->n; i++) {
      v->start[first + 1 + i - last] = v->start[i];
      v->end[first + 1 + i - last] = v->end[i];
    }
    v->n -= last - first - 1;
  } else {
    for (unsigned i = v->n; i > first; i--) {
      v->start[i] = v->start[i - 1]; v->end[i] = v->end[i - 1];
    }
    v->n++;
  }
  v->start[first] = start; v->end[first] = end;
}
static inline double p6_view_after(const P6Viewed *v, double base, double position) {
  double total = 0.0;
  if (!isfinite(base) || !isfinite(position) || position <= base) return 0.0;
  for (unsigned i = 0; i < v->n; i++)
    total += fmax(0.0, fmin(position, v->end[i]) - fmax(base, v->start[i]));
  return total;
}

/* Genuine viewing proof is unique media time, not the seek target.
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
