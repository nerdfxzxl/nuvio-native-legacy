// "CONTINUAR ASSISTINDO" OTIMISTA NA SAIDA DO PLAYER (pedido do dono, 03/10:
// "se o filme nao ta no continue watching ele so entra depois que ele sair da
// ilha"). O titulo que acabou de sair no meio vai para a FRENTE da fileira ja
// no quadro em que a home reaparece, por baixo do voo ate a ilha — e a mesma
// posicao que montarContinuar da a ele (o instante mais novo ganha), entao a
// refacao que vem depois (fecharSessao -> desc_refazer_continuar) republica a
// mesma ordem e a home nao remonta nada (hash por identidade, home.c).
//
// Composicao pura, sem trava nem catalogo, para a regressao (tests/cwfrente.c).
#ifndef NV_CWFRENTE_H
#define NV_CWFRENTE_H
#include "catalogo.h"
#include "idbase.h"
#include <string.h>

// A paused CW card may store its full base and explicit episode pair when
// the composite cannot fit. The card is never a stream ID.
static inline int cw_card_episodio(CatItem *item, const char *base, int t, int e) {
  char id[sizeof item->imdb], composto[sizeof item->imdb];
  if (!item || t < 0 || e <= 0 || !base || idbase_len(base) != strlen(base) ||
      !idbase_copiar_exato(base, id, sizeof id)) return 0;
  if (!idbase_compor_episodio(id, t, e, composto, sizeof composto))
    memcpy(composto, id, strlen(id) + 1);
  if (item->temporada != t || item->episodio != e) {
    item->nomeEpisodio[0] = 0; item->tvdbEp = 0;
  }
  memcpy(item->imdb, composto, strlen(composto) + 1);
  item->temporada = t; item->episodio = e;
  return 1;
}
// The displayed pair wins over an older composite after a progress update.
static inline void cw_card_par(const CatItem *item, int *t, int *e) {
  *t = item->temporada; *e = item->episodio;
  if (*t < 0 || *e <= 0) idbase_episodio(item->imdb, t, e);
}

// `novo` na frente, as outras obras na ordem em que estavam (a mesma obra em
// outro episodio sai: uma serie entra uma vez), ate `max`. *mudou = 0 quando a
// fileira ja era essa (mesma ordem e mesma barra na frente): nada a publicar.
static inline int cw_frente_compor(const CatItem *atual, int n, const CatItem *novo,
                                   CatItem *saida, int max, int *mudou) {
  size_t L = idbase_len(novo->imdb);
  int i, k = 0;
  *mudou = 0;
  if (max < 1 || !novo->imdb[0]) return 0;
  saida[k++] = *novo;
  for (i = 0; i < n && k < max; i++) {
    if (idbase_len(atual[i].imdb) == L && !strncmp(atual[i].imdb, novo->imdb, L)) continue;
    saida[k++] = atual[i];
  }
  if (k != n || atual[0].progresso != novo->progresso ||
      atual[0].temporada != novo->temporada || atual[0].episodio != novo->episodio ||
      atual[0].retomadoMs != novo->retomadoMs ||
      atual[0].restanteMin != novo->restanteMin) *mudou = 1;
  for (i = 0; i < k && !*mudou; i++)
    if (strcmp(saida[i].imdb, atual[i].imdb)) *mudou = 1;
  return k;
}
#endif
