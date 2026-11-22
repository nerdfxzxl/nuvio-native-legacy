/* P2-C host-only loading-escape policy regression, C11. No SDL or Android. */
#include <stdint.h>
#include <stdio.h>
#include "../src/loading_nav_p2c.h"

static int checks;
#define CHECK(expr) do { checks++; if (!(expr)) { \
  fprintf(stderr, "P2-C FAIL line %d: %s\n", __LINE__, #expr); return 1; \
} } while (0)

int main(void) {
  CHECK(NV_LOADING_CHANGE_MS == 15000u);
  CHECK(!nv_loading_change_ready(14999u, 0u, 1));
  CHECK(nv_loading_change_ready(15000u, 0u, 1));
  CHECK(nv_loading_change_ready(20000u, 0u, 1));
  CHECK(!nv_loading_change_ready(20000u, 0u, 0));
  CHECK(!nv_loading_change_ready(100u, 100u, 1));
  CHECK(!nv_loading_change_ready(15100u, 101u, 1));
  CHECK(nv_loading_change_ready(15101u, 101u, 1));
  /* SDL 32-bit tick wraparound: real elapsed time, not wall-clock comparisons. */
  CHECK(!nv_loading_change_ready(14988u, UINT32_MAX - 10u, 1));
  CHECK(nv_loading_change_ready(14989u, UINT32_MAX - 10u, 1));
  /* Explicit Down only focuses, never triggers selection. */
  CHECK(!nv_loading_change_confirm(0, 0));
  CHECK(!nv_loading_change_confirm(1, 0));
  CHECK(!nv_loading_change_confirm(0, 1));
  CHECK(nv_loading_change_confirm(1, 1));
  /* Lost startup state always prevents a lingering focused action. */
  CHECK(!nv_loading_change_confirm(nv_loading_change_ready(15000u, 0u, 0), 1));
  printf("P2-C elapsed/focus guard: %d checks PASS\n", checks);
  return 0;
}
