#ifndef NV_LOADING_ARC_P2_H
#define NV_LOADING_ARC_P2_H
/* Native translation of NuvioTV LoadingIndicator's geometric timing.
 * Independent of SDL/GLES so host timing checks can exercise the math. */
#include <math.h>
typedef struct { float inicio; float fracao; } NvLoadingArcP2;
static inline float nv_arc_limitar(float v) { return v < 0.0f ? 0.0f : v > 1.0f ? 1.0f : v; }
/* CubicBezierEasing(0.333,0,0.667,1): invert the x curve to get easing y. */
static inline float nv_arc_ease(float x) {
  float lo = 0.0f, hi = 1.0f, t, xx;
  int i;
  x = nv_arc_limitar(x);
  for (i = 0; i < 12; i++) {
    t = (lo + hi) * 0.5f;
    xx = 3.0f * 0.333f * (1.0f-t)*(1.0f-t)*t +
         3.0f * 0.667f * (1.0f-t)*t*t + t*t*t;
    if (xx < x) lo = t; else hi = t;
  }
  t = (lo + hi) * 0.5f;
  return t*t*(3.0f - 2.0f*t);
}
static inline NvLoadingArcP2 nv_arc_quadro(unsigned agoraMs, int reduzMovimento) {
  NvLoadingArcP2 a;
  const float frame = (float)(agoraMs % 2000u) * (60.0f / 2000.0f);
  const float inicio = 99.0f * nv_arc_ease((frame - 10.0f) / 50.0f);
  const float fim = 1.0f + 99.0f * nv_arc_ease(frame / 50.0f);
  float angulo = -90.0f + frame / 60.0f * 363.0f + inicio * 3.6f;
  if (reduzMovimento) { a.inicio = 0.0f; a.fracao = 0.25f; return a; }
  a.inicio = fmodf((angulo + 90.0f) / 360.0f, 1.0f);
  a.fracao = nv_arc_limitar((fim - inicio) / 100.0f);
  if (a.fracao < 0.01f) a.fracao = 0.01f;
  return a;
}
#endif
