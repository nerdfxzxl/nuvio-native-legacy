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
  P6Viewed v = {0};
  for (int i = 0; i < 100; i++) p6_view_add(&v, 0, 100);
  CHECK(v.n == 1 && p6_view_after(&v, 0, 1000) == 100);
  CHECK(!p6_watched_reached(950, 1000, 0, 90, 0, p6_view_after(&v, 0, 950)));
  p6_view_add(&v, 300, 400);
  p6_view_add(&v, 90, 310);
  CHECK(v.n == 1 && p6_view_after(&v, 0, 1000) == 400);
  CHECK(p6_view_after(&v, 200, 350) == 150);
  p6_view_add(&v, 700, 950);
  CHECK(v.n == 2 && p6_view_after(&v, 0, 950) == 650);
  CHECK(!p6_watched_reached(950, 1000, 0, 90, 0, p6_view_after(&v, 0, 950)));
  p6_view_add(&v, 400, 700);
  CHECK(v.n == 1 && p6_view_after(&v, 0, 950) == 950);
  CHECK(p6_watched_reached(950, 1000, 0, 90, 0, p6_view_after(&v, 0, 950)));
  CHECK(p6_view_after(&v, 800, 950) == 150);
  P6Viewed full = {0};
  for (unsigned i = 0; i < P6_VIEW_SPANS + 1; i++) p6_view_add(&full, i*2.0, i*2.0+1.0);
  CHECK(full.n == P6_VIEW_SPANS && p6_view_after(&full, 0, 1000) == P6_VIEW_SPANS);
  p6_view_add(&full, 0, 128);
  CHECK(full.n == 1 && p6_view_after(&full, 0, 1000) == 128);
  p6_view_add(&full, NAN, 1000);
  CHECK(full.n == 1 && p6_view_after(&full, 0, 1000) == 128);
  printf("P6 watched policy: %u assertions PASS\n", n);
  return 0;
}
