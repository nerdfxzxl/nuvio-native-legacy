#ifndef NV_LOADING_TELEMETRY_P2_H
#define NV_LOADING_TELEMETRY_P2_H
/* Display measured socket throughput only. Unknown is not zero.
 * Samples arrive about once per second; smoothing time constant 2.5s. */
#include <stdint.h>
#include <stdio.h>
typedef struct {
  int valido, temAnterior;
  int64_t bytesAnterior;
  uint32_t emAnterior;
  double bps;
} NvMedidorDownload;
static inline void nv_download_zerar(NvMedidorDownload *m) {
  m->valido = m->temAnterior = 0;
  m->bytesAnterior = 0; m->emAnterior = 0; m->bps = -1.0;
}
static inline void nv_download_amostrar(NvMedidorDownload *m, int64_t bytes, uint32_t agora) {
  uint32_t dt;
  int64_t delta;
  if (bytes < 0) { nv_download_zerar(m); return; }
  if (!m->temAnterior || bytes < m->bytesAnterior) {
    m->bytesAnterior = bytes; m->emAnterior = agora;
    m->temAnterior = 1; m->valido = 0; m->bps = -1.0; return;
  }
  dt = agora - m->emAnterior;
  if (dt < 500u) return;
  delta = bytes - m->bytesAnterior;
  m->bytesAnterior = bytes; m->emAnterior = agora;
  if (dt > 5000u) { m->valido = 0; m->bps = -1.0; return; }
  {
    double instant = (double)delta * 1000.0 / (double)dt;
    double alpha = (double)dt / ((double)dt + 2500.0);
    m->bps = m->valido ? m->bps * (1.0 - alpha) + instant * alpha : instant;
    m->valido = 1;
  }
}
static inline int nv_download_texto(double bps, char *out, unsigned cap) {
  if (!out || !cap) return 0;
  out[0] = 0;
  if (bps < 0.0) return 0;
  if (bps >= 1024.0 * 1024.0)
    snprintf(out, cap, "%.1f MB/s", bps / (1024.0 * 1024.0));
  else if (bps >= 1024.0)
    snprintf(out, cap, "%.1f KB/s", bps / 1024.0);
  else snprintf(out, cap, "%.0f B/s", bps);
  return 1;
}
#endif
