// P3 Dynamic Clock reload policy. Header-only for host C11 regression tests.
// Old cards may remain visible, but are never selectable during refresh.
#ifndef NV_STREAM_RELOAD_P3_H
#define NV_STREAM_RELOAD_P3_H

typedef enum {
  NV_P3_RELOAD_IDLE = 0,
  NV_P3_RELOAD_RETAINED, // previous cards visible; awaiting first fresh batch
  NV_P3_RELOAD_FRESH     // only current-request cards visible; more may arrive
} NvP3ReloadPhase;

typedef struct {
  NvP3ReloadPhase phase;
  unsigned serial;
} NvP3Reload;

static inline void nv_p3_reload_begin(NvP3Reload *r) {
  if (!r) return;
  if (++r->serial == 0) ++r->serial;
  r->phase = NV_P3_RELOAD_RETAINED;
}
static inline int nv_p3_reload_first(NvP3Reload *r) {
  if (!r || r->phase != NV_P3_RELOAD_RETAINED) return 0;
  r->phase = NV_P3_RELOAD_FRESH;
  return 1;
}
static inline void nv_p3_reload_finish(NvP3Reload *r) {
  if (r) r->phase = NV_P3_RELOAD_IDLE;
}
static inline int nv_p3_reload_selectable(const NvP3Reload *r,
                                          unsigned cardGeneration,
                                          unsigned listGeneration) {
  return (!r || r->phase != NV_P3_RELOAD_RETAINED) &&
         cardGeneration == listGeneration;
}
#endif
