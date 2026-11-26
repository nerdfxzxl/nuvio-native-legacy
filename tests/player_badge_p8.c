#include "player_badge_p8.h"
#include <assert.h>
#include <stdio.h>
int main(void) {
  for (int c=-3;c<=4;c++) for (int s=-2;s<=2;s++)
    for (int r=0;r<=2;r++) for (unsigned m=0;m<32;m++) {
      P8BadgePolicy p=p8_badge_policy(c,s,r,m,0);
      assert(!(p.mask & ~m));
      if (c==1 || !p.mask) assert(p.family==P8_BADGE_HIDDEN);
      else if (c==SELOS_LEGACY || (c==0 && s==SELOS_LEGACY)) assert(p.family==P8_BADGE_LEGACY);
      else assert(p.family==P8_BADGE_PACK);
      if (r!=CB_GANHO_PASSTHROUGH) assert(!(p.mask&(P8_B_ATMOS|P8_B_CHANNELS)));
      if (r==CB_GANHO_DESCONHECIDO) assert(!(p.mask&P8_B_CODEC));
      P8BadgePolicy hidden=p8_badge_policy(c,s,r,m,1);
      assert(hidden.family==P8_BADGE_HIDDEN && !hidden.mask);
    }
  puts("P8 badge policy: 3840 combinations PASS (choice, route, all switch masks and info ownership)");
  return 0;
}
