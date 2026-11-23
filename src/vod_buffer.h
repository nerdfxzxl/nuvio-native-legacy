#ifndef NV_VOD_BUFFER_H
#define NV_VOD_BUFFER_H
#include <stdint.h>

/* Absolute Media3 window coordinates. A seek alone can mask bufferedPosition
   to the destination. Only a positive measured interval represents media ahead. */
static inline int64_t nv_vod_buffer_end(int64_t position, int64_t end, int64_t duration) {
  if (position < 0 || end <= position || (duration > 0 && end > duration)) return 0;
  return end;
}
#endif
