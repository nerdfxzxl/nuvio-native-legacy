#include "abrecine.h"
#include "anim.h"

void abrecine_passo(AbreCine *a, int abrindo, int imagem, int detalhadaPadrao,
                    Uint32 agora, float dt, int reduzidas) {
  if (abrindo) {
    if (!a->viva && a->veu < 0.01f) { a->detalhada = detalhadaPadrao; a->desde = agora; a->foco = 0.0f; a->calado = 1; }
    a->viva = 1;
    a->fim = 0; /* 200ms exit starts only when the frame is confirmed */
    a->veu = 1.0f;
    // O recuo da arte leva ~1 s: o corte para a tela de espera nao se ve.
    a->foco = reduzidas ? 1.0f : anim_mola(a->foco, 1.0f, dt, 3.0f);
    return;
  }
  a->viva = 0;
  if (a->veu <= 0.0f) return;
  /* Never expose the hardware plane before Media3's first frame and surface
   * confirmation. READY may arrive up to hundreds of ms earlier. */
  if (!imagem) return;
  if (!a->fim) a->fim = agora ? agora : 1u;
  a->veu = reduzidas ? 0.0f : (agora - a->fim >= 200u ? 0.0f :
                              1.0f - (float)(agora - a->fim) / 200.0f);
}

int abrecine_osd_calado(const AbreCine *a) { return a->calado; }

void abrecine_acordou(AbreCine *a, int carregando) {
  if (!carregando && !a->viva) a->calado = 0;
}

int abrecine_cinema(const AbreCine *a) { return a->veu > 0.0f && !a->detalhada; }

int abrecine_demorando(const AbreCine *a, Uint32 agora) {
  return a->viva && agora - a->desde >= ABRECINE_DEMORA_MS;
}

int abrecine_tecla(AbreCine *a, SDL_Keycode k) {
  if (!a->viva || a->detalhada || (k != SDLK_UP && k != SDLK_DOWN)) return 0;
  a->detalhada = 1;
  return 1;
}

AbreCineEtapa abrecine_etapa(int esperandoFonte) {
  return esperandoFonte ? ABRECINE_RESOLVENDO : ABRECINE_CARREGANDO;
}
