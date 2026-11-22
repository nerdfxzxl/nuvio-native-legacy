/* P2 native, SDK-independent regression for timing and measured throughput.
 * bash tests/playback_p2_timing.sh (uses a temporary SDL type stub only).
 */
#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
#include "../src/loading_arc_p2.h"
#include "../src/loading_telemetry_p2.h"
#include "../src/abrecine.h"

static void perto(float a, float b) { assert(fabsf(a - b) < 0.02f); }

int main(void) {
  NvMedidorDownload m;
  NvLoadingArcP2 arc;
  AbreCine cine = {0};
  char txt[64];

  arc = nv_arc_quadro(0u, 0);
  assert(arc.inicio >= 0.0f && arc.inicio <= 1.0f);
  assert(arc.fracao > 0.0f && arc.fracao <= 1.0f);
  arc = nv_arc_quadro(1250u, 0);
  assert(arc.inicio >= 0.0f && arc.inicio <= 1.0f);
  assert(arc.fracao > 0.0f && arc.fracao <= 1.0f);
  arc = nv_arc_quadro(1450u, 1);
  perto(arc.inicio, 0.0f);
  perto(arc.fracao, 0.25f);

  nv_download_zerar(&m);
  assert(!m.valido && !nv_download_texto(m.bps, txt, sizeof txt));
  nv_download_amostrar(&m, 4096, 1000u);
  assert(!m.valido);
  nv_download_amostrar(&m, 1089536, 2000u);
  assert(m.valido);
  assert(m.bps >= 1040000 && m.bps <= 1090000);
  assert(nv_download_texto(m.bps, txt, sizeof txt));
  assert(strstr(txt, "MB/s") != NULL);
  nv_download_amostrar(&m, 1089536, 3000u);
  assert(m.valido && m.bps > 0.0); /* smoothing does not fabricate zero */
  nv_download_amostrar(&m, -1, 4000u);
  assert(!m.valido && !nv_download_texto(m.bps, txt, sizeof txt));
  assert(nv_download_texto(256.0, txt, sizeof txt) && strcmp(txt, "256 B/s") == 0);
  assert(nv_download_texto(2048.0, txt, sizeof txt) && strstr(txt, "KB/s"));
  assert(nv_download_texto(2097152.0, txt, sizeof txt) && strcmp(txt, "2.0 MB/s") == 0);

  abrecine_passo(&cine, 1, 0, 0, 1000u, 0.016f, 0);
  perto(cine.veu, 1.0f);
  abrecine_passo(&cine, 0, 0, 0, 1500u, 0.016f, 0);
  assert(cine.fim == 0u); perto(cine.veu, 1.0f);
  abrecine_passo(&cine, 0, 1, 0, 1700u, 0.016f, 0);
  assert(cine.fim == 1700u); perto(cine.veu, 1.0f);
  abrecine_passo(&cine, 0, 1, 0, 1800u, 0.016f, 0);
  perto(cine.veu, 0.5f);
  abrecine_passo(&cine, 0, 1, 0, 1900u, 0.016f, 0);
  perto(cine.veu, 0.0f);
  memset(&cine, 0, sizeof cine);
  abrecine_passo(&cine, 1, 0, 0, 2000u, 0.016f, 1);
  abrecine_passo(&cine, 0, 1, 0, 2001u, 0.016f, 1);
  perto(cine.veu, 0.0f);
  puts("P2 arc, measured download, first-frame/reduced-motion timing: PASS");
  return 0;
}
