#ifndef NV_LOADING_NAV_P2C_H
#define NV_LOADING_NAV_P2C_H
#include <stdint.h>

/* Native loading escape: never a progress estimate. The owner explicitly
 * focuses Change Stream only after this elapsed startup interval. Uint32
 * subtraction remains well-defined across the SDL ticks wraparound. */
#define NV_LOADING_CHANGE_MS 15000u

static inline int nv_loading_change_ready(uint32_t now, uint32_t started, int loading) {
  return !!loading && (uint32_t)(now - started) >= NV_LOADING_CHANGE_MS;
}

/* OK must not switch streams without an explicit visible focus. */
static inline int nv_loading_change_confirm(int visible, int focused) {
  return !!visible && !!focused;
}
#endif
