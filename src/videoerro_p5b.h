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
/* Explicit same-source retry retains its failure owner until real visual
 * handoff. Generation zero denotes an armed but not yet opened retry. */
typedef struct { int motivo; unsigned geracao, desde; } NvVideoRetryP5B;
static inline void videoerro_p5b_retry_clear(NvVideoRetryP5B *s) {
  s->motivo = 0; s->geracao = s->desde = 0;
}
static inline void videoerro_p5b_retry_arm(NvVideoRetryP5B *s, int motivo) {
  videoerro_p5b_retry_clear(s); s->motivo = motivo;
}
static inline void videoerro_p5b_retry_bind(NvVideoRetryP5B *s, unsigned g, unsigned now) {
  if (s->motivo > 0) { s->geracao = g; s->desde = now; }
}
static inline int videoerro_p5b_retry_owned(const NvVideoRetryP5B *s, unsigned g) {
  return s->motivo > 0 && g != 0u && s->geracao == g;
}
static inline int videoerro_p5b_retry_fail(NvVideoRetryP5B *s, unsigned g) {
  if (!videoerro_p5b_retry_owned(s, g)) return 0;
  int reason = s->motivo; videoerro_p5b_retry_clear(s); return reason;
}
static inline int videoerro_p5b_retry_handoff(NvVideoRetryP5B *s, unsigned g,
                                              int frame, int stable) {
  if (!videoerro_p5b_retry_owned(s, g) || !frame || !stable) return 0;
  videoerro_p5b_retry_clear(s); return 1;
}
#endif
