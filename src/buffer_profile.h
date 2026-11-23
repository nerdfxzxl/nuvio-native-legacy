#ifndef NV_BUFFER_PROFILE_H
#define NV_BUFFER_PROFILE_H
#include <math.h>
#include <stdlib.h>
#include <string.h>

/* MediaInfo's General/Overall bit rate is a whole-file average, unlike its
   Video/Audio bit rate fields. Never consume an arbitrary "Mbps" badge. */
static inline int nv_overall_bps(const char *description) {
  int result = 0;
  for (const char *p = description; p && *p; ) {
    size_t length = strcspn(p, "\r\n");
    const char *end = p + length;
    while (p < end && (*p == ' ' || *p == '\t')) p++;
    const char label[] = "Overall bit rate";
    if ((size_t)(end - p) > sizeof label - 1 && !strncmp(p, label, sizeof label - 1)) {
      p += sizeof label - 1;
      while (p < end && (*p == ' ' || *p == '\t')) p++;
      if (*p == ':') {
        p++;
        while (p < end && *p == ' ') p++;
        if (*p < '0' || *p > '9') return 0;
        char *unit;
        double value = strtod(p, &unit);
        while (unit < end && *unit == ' ') unit++;
        while (end > unit && end[-1] == ' ') end--;
        size_t n = (size_t)(end - unit);
        int scale = n == 4 && !memcmp(unit, "Mb/s", 4) ? 1000000 :
                    n == 4 && !memcmp(unit, "kb/s", 4) ? 1000 :
                    n == 3 && !memcmp(unit, "b/s", 3) ? 1 : 0;
        double bits = value * scale;
        if (!isfinite(bits) || bits < 1 || bits > 2000000000) return 0;
        int rate = (int)ceil(bits);
        if (result && result != rate) return 0;
        result = rate;
      }
    }
    p = strpbrk(p, "\r\n");
    if (p) p += strspn(p, "\r\n");
  }
  return result;
}

static inline int nv_buffer_low(int averageBps, int thresholdMbps) {
  return averageBps > 0 && thresholdMbps > 0 &&
         (long long)averageBps < (long long)thresholdMbps * 1000000;
}
#endif
