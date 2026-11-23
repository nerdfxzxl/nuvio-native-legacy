// Standalone P3 policy checks: no SDL/Android/third-party dependencies.
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include "../src/stream_reload_p3.h"
#include "../src/fonteid_p1.h"

static NvFonteId id(const char *addon, const char *hash, int file,
                    const char *filename, const char *label,
                    unsigned long long bytes, const char *group) {
  NvFonteId a = {addon, hash, file, filename, label, bytes, group};
  return a;
}
int main(void) {
  NvP3Reload r = {0};
  unsigned generation = 14;
  assert(nv_p3_reload_selectable(&r, 14, generation));
  nv_p3_reload_begin(&r);
  assert(r.phase == NV_P3_RELOAD_RETAINED && r.serial == 1);
  assert(!nv_p3_reload_selectable(&r, generation, generation));
  assert(!nv_p3_reload_selectable(&r, 13, generation));
  assert(nv_p3_reload_first(&r));
  generation++;
  assert(nv_p3_reload_selectable(&r, 15, generation));
  assert(!nv_p3_reload_selectable(&r, 14, generation));
  assert(!nv_p3_reload_first(&r)); // second/out-of-order batch is append-only
  nv_p3_reload_finish(&r);
  assert(nv_p3_reload_selectable(&r, 15, generation));
  nv_p3_reload_begin(&r); // rapid reload invalidates even a queued fresh OK
  assert(r.serial == 2 && !nv_p3_reload_selectable(&r, 15, generation));
  nv_p3_reload_finish(&r); // empty/error/cancellation cleans up
  assert(r.phase == NV_P3_RELOAD_IDLE);
  NvFonteId old = id("addon-a","ABCDEF",2,"Movie.2160p.mkv","4K",123456,"g1");
  NvFonteId rotated = id("addon-a","abcdef",2,"Movie.2160p.mkv","4K",123456,"g1");
  NvFonteId differentFile = id("addon-a","ABCDEF",3,"Movie.2160p.mkv","4K",123456,"g1");
  NvFonteId differentAddon = id("addon-b","ABCDEF",2,"Movie.2160p.mkv","4K",123456,"g1");
  NvFonteId differentSize = id("addon-a","ABCDEF",2,"Movie.2160p.mkv","4K",123457,"g1");
  assert(nv_fonte_mesma_liberacao(&old,&rotated));
  assert(!nv_fonte_mesma_liberacao(&old,&differentFile));
  assert(!nv_fonte_mesma_liberacao(&old,&differentAddon));
  assert(!nv_fonte_mesma_liberacao(&old,&differentSize));
  NvFonteId noProofA = id("addon-a","",-1,"","same label",0,"");
  NvFonteId noProofB = id("addon-a","",-1,"","same label",0,"");
  assert(!nv_fonte_mesma_liberacao(&noProofA,&noProofB));
  NvFonteId otherEpisode = id("addon-a","OTHER",2,"Other Episode.mkv","4K",123456,"g1");
  assert(!nv_fonte_mesma_liberacao(&old,&otherEpisode));
  puts("P3 reload policy: PASS (retention, stale generation, rapid refresh, exact release, URL rotation, episode isolation)");
  return 0;
}
