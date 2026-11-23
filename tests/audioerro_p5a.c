#include "audioerro_p5a.h"
#include <assert.h>
#include <stdio.h>

int main(void) {
  int focus = AUDIOERRO_P5A_RETRY;
  assert(audioerro_p5a_mover(focus, -1) == AUDIOERRO_P5A_RETRY);
  focus = audioerro_p5a_mover(focus, 1);
  assert(focus == AUDIOERRO_P5A_CHANGE_STREAM);
  focus = audioerro_p5a_mover(focus, 1);
  assert(focus == AUDIOERRO_P5A_BACK);
  assert(audioerro_p5a_mover(focus, 1) == AUDIOERRO_P5A_BACK);
  assert(audioerro_p5a_mover(focus, -1) == AUDIOERRO_P5A_CHANGE_STREAM);
  assert(audioerro_p5a_mover(-10, 0) == AUDIOERRO_P5A_RETRY);
  assert(audioerro_p5a_mover(100, 0) == AUDIOERRO_P5A_BACK);
  assert(audioerro_p5a_pode_repetir(0));
  assert(!audioerro_p5a_pode_repetir(1));
  assert(!audioerro_p5a_pode_repetir(2));
  puts("audioerro_p5a: PASS (10 production C assertions)");
  return 0;
}
