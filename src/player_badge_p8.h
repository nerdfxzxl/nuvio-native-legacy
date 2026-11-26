#ifndef NV_PLAYER_BADGE_P8_H
#define NV_PLAYER_BADGE_P8_H
#include "selospacote.h"
#include "cacheboost.h"

/* One visible family. Source metadata is never an input to this policy.
 * Channels are output claims, so omit them when PCM shape is not reported.
 * A selected JOC track alone does not prove Atmos at the sink. */
enum { P8_BADGE_HIDDEN, P8_BADGE_LEGACY, P8_BADGE_PACK };
enum { P8_B_RES=1, P8_B_HDR=2, P8_B_ATMOS=4, P8_B_CODEC=8, P8_B_CHANNELS=16 };
typedef struct { int family; unsigned mask; } P8BadgePolicy;
static inline P8BadgePolicy p8_badge_policy(int choice, int stream, int route,
                                            unsigned enabled, int infoOpen) {
  P8BadgePolicy p={P8_BADGE_HIDDEN,0};
  if (choice==1 || infoOpen) return p;
  p.family=choice==SELOS_LEGACY || (choice==0 && stream==SELOS_LEGACY)
           ? P8_BADGE_LEGACY : P8_BADGE_PACK;
  p.mask=enabled & 31u;
  if (route != CB_GANHO_PASSTHROUGH) p.mask &= ~(P8_B_ATMOS|P8_B_CHANNELS);
  if (route != CB_GANHO_PASSTHROUGH && route != CB_GANHO_PCM) p.mask &= ~P8_B_CODEC;
  if (!p.mask) p.family=P8_BADGE_HIDDEN;
  return p;
}
#endif
