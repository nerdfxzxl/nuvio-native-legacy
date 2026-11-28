#ifndef NV_STREAM_SCROLL_CLOCK_H
#define NV_STREAM_SCROLL_CLOCK_H
#include "anim.h"
// The shared clock limits resumed frames to 100ms. Preserve that safety cap,
// but consume the whole bounded elapsed frame, in <=50ms spring substeps.
static inline float nv_stream_scroll_step(float *v, float current, float target, float dt, float w) {
  if (dt > .1f) dt = .1f;
  while (dt > .05f) {
    current = anim_mola2(v, current, target, .05f, w);
    dt -= .05f;
  }
  return anim_mola2(v, current, target, dt, w);
}
#endif
