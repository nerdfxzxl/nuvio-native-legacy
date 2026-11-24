#ifndef NV_POSTPLAY_P6_H
#define NV_POSTPLAY_P6_H
#include <math.h>

/* A recommendation window, metadata credits marker or seek target is not EOF.
 * Only the current pipeline's ended signal plus its real position permits an
 * automatic transition. User-selected Next is deliberately independent. */
static inline int p6_postplay_eof(int active, int ended, double position,
                                  double duration) {
  return active && ended && isfinite(position) && isfinite(duration) &&
         duration > 1.0 && position >= duration - 1.0 &&
         position <= duration + 1.0;
}

static inline int p6_postplay_countdown(int eof, int suspended, int seeking,
                                       int dismissed, int visible, int series,
                                       int autoplay) {
  return eof && !suspended && !seeking && !dismissed &&
         visible && series && autoplay;
}
#endif
