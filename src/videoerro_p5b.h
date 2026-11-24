#ifndef NV_VIDEOERRO_P5B_H
#define NV_VIDEOERRO_P5B_H

/* Q39: a user-owned Retry consumes its one attempt even if the synchronous
 * reopening call fails. Other modal actions never spend that budget. */
enum { VIDEOERRO_P5B_NONE = -1, VIDEOERRO_P5B_RETRY = 0,
       VIDEOERRO_P5B_CHANGE = 1, VIDEOERRO_P5B_BACK = 2 };
static inline int videoerro_p5b_acao(int foco, int *jaTentou) {
  if (foco == VIDEOERRO_P5B_RETRY) {
    if (!jaTentou || *jaTentou) return VIDEOERRO_P5B_NONE;
    *jaTentou = 1;
    return VIDEOERRO_P5B_RETRY;
  }
  if (foco == VIDEOERRO_P5B_CHANGE || foco == VIDEOERRO_P5B_BACK) return foco;
  return VIDEOERRO_P5B_NONE;
}
/* Once a video failure owns a generation, queued playback/renderer events
 * cannot clear or replace it. Only a new native opening resets that latch. */
static inline int videoerro_p5b_evento_aceito(int ativo, unsigned geracao,
                                             unsigned atual, int terminal) {
  return ativo && geracao != 0u && geracao == atual && terminal == 0;
}
#endif
