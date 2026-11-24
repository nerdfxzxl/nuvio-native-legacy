#include "../src/watched_p6.h"
#include <assert.h>
#include <stdio.h>
int main(void) {
  unsigned n = 0;
#define CHECK(x) do { assert(x); ++n; } while (0)
  CHECK(p6_watched_marker(800, 1000, 220) == 800);
  CHECK(p6_watched_marker(400, 1000, 700) == 0);
  CHECK(p6_watched_marker(600, 1000, 200) == 0);
  CHECK(p6_watched_marker(1000, 1000, 200) == 0);
  CHECK(p6_watched_marker(1100, 1000, 200) == 0);
  CHECK(p6_watched_marker(NAN, 1000, 200) == 0);
  CHECK(p6_watched_marker(80, 100, 30) == 0);
  CHECK(!p6_watched_reached(799, 1000, 800, 70, 0, 799));
  CHECK(p6_watched_reached(800, 1000, 800, 98, 0, 800));
  CHECK(!p6_watched_reached(900, 1000, 950, 70, 0, 900));
  CHECK(!p6_watched_reached(799, 1000, 0, 80, 0, 799));
  CHECK(p6_watched_reached(800, 1000, 0, 80, 0, 800));
  CHECK(p6_watched_reached(700, 1000, 0, 70, 0, 700));
  CHECK(!p6_watched_reached(700, 1000, 0, 98, 0, 700));
  CHECK(!p6_watched_reached(950, 1000, 0, 90, 0, 5));
  CHECK(!p6_watched_reached(1000, 1000, 0, 90, 0, 100));
  CHECK(p6_watched_reached(900, 1000, 0, 90, 700, 200));
  CHECK(!p6_watched_reached(900, 1000, 0, 90, 700, 1));
  CHECK(!p6_watched_reached(1000, 1000, 0, 99, 0, 1000));
  CHECK(!p6_watched_reached(1002, 1000, 0, 90, 0, 1000));
  CHECK(!p6_watched_reached(50, 60, 0, 90, 0, 50));
  CHECK(!p6_watched_reached(950, 1000, 0, 90, 0, NAN));
  CHECK(!p6_watched_reached(950, 1000, 0, 90, 1001, 20));
  printf("P6 watched policy: %u assertions PASS\n", n);
  return 0;
}
