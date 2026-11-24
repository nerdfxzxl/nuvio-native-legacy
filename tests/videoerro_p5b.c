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
  puts("videoerro_p5b: PASS (11 production C action assertions)");
  return 0;
}
