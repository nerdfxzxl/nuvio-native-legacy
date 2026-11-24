#include "../src/postplay_p6.h"
#include <assert.h>
#include <stdio.h>

int main(void) {
  unsigned n = 0;
#define CHECK(x) do { assert(x); ++n; } while (0)
  CHECK(!p6_postplay_eof(1, 0, 1200, 1200));
  CHECK(!p6_postplay_eof(1, 1, 1100, 1200));
  CHECK(!p6_postplay_eof(0, 1, 1200, 1200));
  CHECK(!p6_postplay_eof(1, 1, NAN, 1200));
  CHECK(!p6_postplay_eof(1, 1, 1200, INFINITY));
  CHECK(!p6_postplay_eof(1, 1, 1200, 0));
  CHECK(!p6_postplay_eof(1, 1, 1202, 1200));
  CHECK(p6_postplay_eof(1, 1, 1199, 1200));
  CHECK(p6_postplay_eof(1, 1, 1200, 1200));
  CHECK(!p6_postplay_countdown(0, 0, 0, 0, 1, 1, 1));
  CHECK(!p6_postplay_countdown(1, 1, 0, 0, 1, 1, 1));
  CHECK(!p6_postplay_countdown(1, 0, 1, 0, 1, 1, 1));
  CHECK(!p6_postplay_countdown(1, 0, 0, 1, 1, 1, 1));
  CHECK(!p6_postplay_countdown(1, 0, 0, 0, 0, 1, 1));
  CHECK(!p6_postplay_countdown(1, 0, 0, 0, 1, 0, 1));
  CHECK(!p6_postplay_countdown(1, 0, 0, 0, 1, 1, 0));
  CHECK(p6_postplay_countdown(1, 0, 0, 0, 1, 1, 1));
  printf("P6 native post-play policy: %u assertions PASS\n", n);
  return 0;
}
