#ifndef NV_AUDIOERRO_P5A_H
#define NV_AUDIOERRO_P5A_H
/* Q36: pure policy used by the EXISTING player error card. No SDL/Android.
 * The audio card has Retry / Change Stream / Back (three focus stops);
 * the ordinary video source-error card remains unchanged (two stops).
 */
enum { AUDIOERRO_P5A_RETRY = 0, AUDIOERRO_P5A_CHANGE_STREAM = 1,
       AUDIOERRO_P5A_BACK = 2 };
static inline int audioerro_p5a_mover(int foco, int delta) {
  if (foco < AUDIOERRO_P5A_RETRY) foco = AUDIOERRO_P5A_RETRY;
  if (foco > AUDIOERRO_P5A_BACK) foco = AUDIOERRO_P5A_BACK;
  if (delta < 0 && foco > AUDIOERRO_P5A_RETRY) return foco - 1;
  if (delta > 0 && foco < AUDIOERRO_P5A_BACK) return foco + 1;
  return foco;
}
static inline int audioerro_p5a_pode_repetir(int jaTentou) {
  return jaTentou == 0;
}
#endif
