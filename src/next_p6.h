#include "stream_id_p6.h"
#ifndef NV_NEXT_P6_H
#define NV_NEXT_P6_H
#include <stdint.h>
#include <string.h>
#include <stdio.h>

/* UI-owned exact-target ticket for an already opened Next cinematic.
 * The old worker generation alone cannot authorize a source in a new player. */
typedef struct {
  int tem, t, e;
  unsigned geracao, playerGeracao;
  uint64_t sessaoPerfil;
  char alvo[NV_STREAM_ID_CAP];
} P6NextIntent;
enum { P6_NEXT_NONE, P6_NEXT_CANCEL, P6_NEXT_WAIT, P6_NEXT_CONSUME };
static inline void p6_next_clear(P6NextIntent *i) { memset(i,0,sizeof *i); }
static inline int p6_next_begin(P6NextIntent *i,const char *alvo,int t,int e,
                                unsigned worker,unsigned player,uint64_t perfil) {
  if(i->tem || !nv_stream_id_valid(alvo) ||
      t<0 || e<1 || !worker || !player) return 0;
  i->tem=1;i->t=t;i->e=e;i->geracao=worker;i->playerGeracao=player;i->sessaoPerfil=perfil;
  snprintf(i->alvo,sizeof i->alvo,"%s",alvo);
  return 1;
}
static inline int p6_next_player_matches(const P6NextIntent *i,const char *alvo,
                                         unsigned player,uint64_t perfil) {
  return i->tem && alvo && *alvo && !strcmp(i->alvo,alvo) &&
      i->playerGeracao==player && i->sessaoPerfil==perfil;
}
static inline int p6_next_decide(const P6NextIntent *i,const char *alvo,unsigned worker,
                                unsigned player,uint64_t perfil,int cancel,int running) {
  if(!i->tem)return P6_NEXT_NONE;
  if(cancel || !p6_next_player_matches(i,alvo,player,perfil) || i->geracao!=worker)
    return P6_NEXT_CANCEL;
  return running?P6_NEXT_WAIT:P6_NEXT_CONSUME;
}
#endif
