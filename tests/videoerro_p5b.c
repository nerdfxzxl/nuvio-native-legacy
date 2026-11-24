#include "videoerro_p5b.h"
#include <assert.h>
#include <stdio.h>

int main(void) {
  int used = 0;
  assert(videoerro_p5b_acao(-1, &used) == VIDEOERRO_P5B_NONE);
  assert(videoerro_p5b_acao(3, &used) == VIDEOERRO_P5B_NONE);
  assert(videoerro_p5b_acao(1, &used) == VIDEOERRO_P5B_CHANGE);
  assert(videoerro_p5b_acao(2, &used) == VIDEOERRO_P5B_BACK);
  assert(used == 0);
  assert(videoerro_p5b_acao(0, &used) == VIDEOERRO_P5B_RETRY);
  assert(used == 1);
  /* No result-dependent rollback: a failed JNI open consumes the attempt. */
  assert(videoerro_p5b_acao(0, &used) == VIDEOERRO_P5B_NONE);
  assert(videoerro_p5b_acao(1, &used) == VIDEOERRO_P5B_CHANGE);
  assert(videoerro_p5b_acao(2, &used) == VIDEOERRO_P5B_BACK);
  assert(videoerro_p5b_acao(0, NULL) == VIDEOERRO_P5B_NONE);
  // Actual production event gate: reject both old generations and any
  // late READY/frame/audio/buffer/error event after terminal video failure.
  assert(videoerro_p5b_evento_aceito(1, 7u, 7u, 0));
  assert(!videoerro_p5b_evento_aceito(1, 6u, 7u, 0));
  assert(!videoerro_p5b_evento_aceito(0, 7u, 7u, 0));
  assert(!videoerro_p5b_evento_aceito(1, 0u, 0u, 0));
  for (int reason = 1; reason <= 5; ++reason)
    assert(!videoerro_p5b_evento_aceito(1, 7u, 7u, reason));
  assert(videoerro_p5b_evento_aceito(1, 8u, 8u, 0));
  puts("videoerro_p5b: PASS (21 production C assertions)");
  return 0;
}
