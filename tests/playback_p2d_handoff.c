/* P2-D: compile/link the REAL src/abrecine.c on host C11.
 * The SDL stub supplies only AbreCine's public types/key constants. No APK.
 */
#include <stdio.h>
#include <string.h>
#include "../src/abrecine.h"
#include "../src/loading_handoff_p2d.h"
#include "../src/loading_nav_p2c.h"

static int checks;
#define CHECK(x) do { ++checks; if (!(x)) { \
  fprintf(stderr, "P2-D FAIL at line %d: %s\\n", __LINE__, #x); return 1; \
} } while (0)

int main(void) {
  AbreCine cine = {0}, audio = {0};
  int focused = 0, ready;
  /* Three seconds after READY with no frame is NOT video-ready. */
  CHECK(!nv_p2d_media_ready(0, 1, 0));
  CHECK(!nv_p2d_media_ready(1, 0, 0));
  CHECK(!nv_p2d_media_ready(1, 0, 0)); /* independent of elapsed ticks */
  CHECK(!nv_p2d_visual_ready(0, 1, 0));
  abrecine_passo(&cine, 1, 0, 0, 100u, .016f, 0);
  CHECK(cine.viva && cine.veu == 1.0f && cine.desde == 100u);
  CHECK(nv_p2d_loading_owner(0, 1, 0, abrecine_navegacao(&cine), 0, 0, 0));
  CHECK(!nv_loading_change_ready(15099u, cine.desde, abrecine_navegacao(&cine)));
  CHECK(nv_loading_change_ready(15100u, cine.desde, abrecine_navegacao(&cine)));
  CHECK(!nv_loading_change_confirm(1, focused)); /* direct OK = diagnostics */
  CHECK(!nv_loading_change_confirm(0, 1));
  focused = 1; /* DOWN focuses; no automatic activation */
  CHECK(nv_loading_change_confirm(1, focused)); /* DOWN then OK */
  focused = 0;
  /* First frame has arrived but hardware surface still unstable. */
  ready = nv_p2d_media_ready(1, 1, 0);
  CHECK(ready);
  CHECK(!nv_p2d_visual_ready(1, 0, 0));
  abrecine_passo(&cine, 0, nv_p2d_visual_ready(1, 0, 0), 0, 15101u, .016f, 0);
  CHECK(!cine.viva && cine.veu == 1.0f && !cine.fim);
  CHECK(abrecine_navegacao(&cine));
  CHECK(nv_p2d_loading_owner(0, 1, ready, abrecine_navegacao(&cine), 0, 0, 0));
  CHECK(nv_loading_change_ready(15101u, cine.desde, abrecine_navegacao(&cine)));
  CHECK(!nv_p2d_visual_ready(0, 1, 0)); /* invalidated surface, stale frame */
  /* Stable FIRST frame begins an unforced 200ms handoff. */
  abrecine_passo(&cine, 0, nv_p2d_visual_ready(1, 1, 0), 0, 15150u, .016f, 0);
  CHECK(cine.veu == 1.0f && cine.fim == 15150u);
  abrecine_passo(&cine, 0, 1, 0, 15190u, .016f, 0);
  CHECK(cine.veu > 0.0f && cine.veu < 1.0f && abrecine_navegacao(&cine));
  /* Instability MID-FADE must re-arm rather than reveal the old surface. */
  abrecine_passo(&cine, 0, 0, 0, 15191u, .016f, 0);
  CHECK(cine.veu == 1.0f && !cine.fim && abrecine_navegacao(&cine));
  abrecine_passo(&cine, 0, 1, 0, 15200u, .016f, 0);
  CHECK(cine.fim == 15200u);
  abrecine_passo(&cine, 0, 1, 0, 15399u, .016f, 0);
  CHECK(cine.veu > 0.0f && abrecine_navegacao(&cine));
  abrecine_passo(&cine, 0, 1, 0, 15400u, .016f, 0);
  CHECK(cine.veu == 0.0f && !cine.viva && !cine.fim);
  CHECK(!abrecine_navegacao(&cine));
  CHECK(!nv_p2d_loading_owner(0, 1, ready, abrecine_navegacao(&cine), 0, 0, 0));
  CHECK(!nv_loading_change_ready(30400u, cine.desde, abrecine_navegacao(&cine)));
  CHECK(!nv_loading_change_confirm(0, 1)); /* stale focus cannot switch */
  /* Confirmed audio-only exits without demanding a video frame/surface. */
  CHECK(nv_p2d_media_ready(1, 0, 1));
  CHECK(nv_p2d_visual_ready(0, 0, 1));
  abrecine_passo(&audio, 1, 0, 0, 200u, .016f, 0);
  abrecine_passo(&audio, 0, 1, 0, 222u, .016f, 0);
  abrecine_passo(&audio, 0, 1, 0, 422u, .016f, 0);
  CHECK(!abrecine_navegacao(&audio));
  CHECK(nv_p2d_media_ready(1, 1, 0));
  /* No new cinematic on ordinary buffering/seek of already ready video. */
  CHECK(!nv_p2d_loading_owner(0, 1, 1, 0, 0, 0, 0));
  CHECK(!nv_p2d_loading_owner(0, 1, 1, 1, 1, 0, 0)); /* Live TV */
  CHECK(nv_p2d_loading_owner(1, 0, 0, 0, 1, 0, 0));  /* live tune */
  CHECK(!nv_p2d_loading_owner(0, 1, 1, 1, 0, 1, 0)); /* error */
  CHECK(!nv_p2d_loading_owner(0, 1, 1, 1, 0, 0, 1)); /* exit */
  CHECK(abrecine_osd_calado(&audio));
  abrecine_acordou(&audio, 0);
  CHECK(!abrecine_osd_calado(&audio));
  /* Reduced motion still requires a genuine ready frame before instant cut. */
  memset(&audio, 0, sizeof audio);
  abrecine_passo(&audio, 1, 0, 0, 500u, .016f, 1);
  abrecine_passo(&audio, 0, 0, 0, 550u, .016f, 1);
  CHECK(abrecine_navegacao(&audio) && audio.veu == 1.0f);
  abrecine_passo(&audio, 0, 1, 0, 600u, .016f, 1);
  CHECK(!abrecine_navegacao(&audio) && audio.veu == 0.0f);
  printf("P2-D real cinematic handoff: %d checks PASS\\n", checks);
  return 0;
}
