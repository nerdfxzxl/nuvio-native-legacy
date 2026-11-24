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
  NvVideoRetryP5B retry = {0};
  videoerro_p5b_retry_arm(&retry, 5);
  assert(!videoerro_p5b_retry_owned(&retry, 7));
  videoerro_p5b_retry_bind(&retry, 8, 100);
  assert(videoerro_p5b_retry_owned(&retry, 8));
  assert(retry.desde == 100);
  assert(!videoerro_p5b_retry_owned(&retry, 7));
  assert(!videoerro_p5b_retry_handoff(&retry, 8, 0, 1));
  assert(!videoerro_p5b_retry_handoff(&retry, 8, 1, 0));
  assert(!videoerro_p5b_retry_handoff(&retry, 7, 1, 1));
  assert(videoerro_p5b_retry_fail(&retry, 7) == 0);
  assert(videoerro_p5b_retry_owned(&retry, 8));
  assert(videoerro_p5b_retry_fail(&retry, 8) == 5);
  assert(!videoerro_p5b_retry_owned(&retry, 8));
  videoerro_p5b_retry_arm(&retry, 1);
  videoerro_p5b_retry_bind(&retry, 9, 200);
  assert(videoerro_p5b_retry_handoff(&retry, 9, 1, 1));
  assert(!videoerro_p5b_retry_owned(&retry, 9));
  assert(videoerro_p5b_retry_fail(&retry, 9) == 0);
  /* A real runtime failure may acquire the active opening; duplicate
   * events cannot replace its error reason or refresh its deadline. */
  assert(!videoerro_p5b_retry_begin_current(&retry, 0, 300, 1));
  assert(!videoerro_p5b_retry_begin_current(&retry, 10, 300, 0));
  assert(videoerro_p5b_retry_begin_current(&retry, 10, 300, 1));
  assert(videoerro_p5b_retry_owned(&retry, 10));
  assert(!videoerro_p5b_retry_begin_current(&retry, 10, 999, 5));
  assert(retry.desde == 300 && retry.motivo == 1);
  assert(!videoerro_p5b_retry_handoff(&retry, 10, 1, 0));
  assert(videoerro_p5b_retry_handoff(&retry, 10, 1, 1));
  assert(!videoerro_p5b_retry_owned(&retry, 10));
  videoerro_p5b_retry_arm(&retry, 4);
  videoerro_p5b_retry_bind(&retry, 11, 400);
  assert(!videoerro_p5b_retry_begin_current(&retry, 11, 999, 1));
  assert(retry.desde == 400 && retry.motivo == 4);
  assert(videoerro_p5b_retry_fail(&retry, 11) == 4);
  puts("videoerro_p5b: PASS (47 production C assertions)");
  return 0;
}
