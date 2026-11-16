// Android card payloads travel with Stream, which is copied by value by the
// sorter, prefetcher and player. No pointer into the HTTP response survives.
#ifndef NV_STREAM_DISPLAY_H
#define NV_STREAM_DISPLAY_H
#include <stddef.h>
#include <stdint.h>
typedef struct {
  uint64_t offset;
  size_t length;
  char local[768];
} StreamDisplay;
char *stream_display_read(const StreamDisplay *display);
#endif
