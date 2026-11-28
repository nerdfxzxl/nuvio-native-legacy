// Native equivalent of official NuvioTV's ParentalGuideOverlay.kt (dev).
// Independent top-left overlay, not a request to expand the Dynamic Clock.
#include "plrguia.h"
#include "parental.h"
#include "anim.h"
#include "ajustes.h"
#include "text.h"
#include "gfx.h"
#include "layout.h"
#define NV_ESCALA_TELA
#include "escala.h"
#include <string.h>

// Official Compose uses 3dp line, 18dp rows, 11sp type and a 10dp
// inner gap. Native's virtual-to-physical scale is different: these are
// calibrated TV-layout coordinates, not literal Compose dp values.
#define PG_LEFT  51.2f // 32dp
#define PG_TOP   38.4f // 24dp
#define PG_ROW   32.0f // 18dp row + 2dp gap
#define PG_LINE  4.8f  // 3dp
#define PG_GAP   16.0f // 10dp
#define PG_ROW_H 28.8f

static float pgEase(float t) {
  t=anim_clamp(t,0.0f,1.0f);
  return t*t*(3.0f-2.0f*t);
}

// Offsets copy the official sequence:
// container 300ms; accent line 400ms; each label 80ms stagger + 200ms fade;
// five seconds fully visible; items reverse (60ms + 150ms each), line and
// container disappear last. Current NuvioTV uses Compose Animatable; this is
// the same choreography implemented natively in C and SDL.
void plrguia_pedir(float tg,float osd,const char *classificacao) {
  int n=parental_n();
  (void)osd; (void)classificacao;
  if(n<1||tg<0.0f)return;
  float appearance=.7f+(float)n*.28f;
  float exitStart=appearance+5.0f;
  float lineEnd=exitStart+(float)n*.21f+.10f;
  float containerEnd=lineEnd+.30f+.20f;
  float alpha=pgEase(tg/.30f)*(1.0f-pgEase((tg-containerEnd)/.20f));
  if(alpha<=.002f)return;
  float line=pgEase((tg-.30f)/.40f)*(1.0f-pgEase((tg-lineEnd)/.30f));
  // Real Nuvio has no dark rounded plaque behind the labels. The vertical
  // accent runs alongside the full three-row text stack.
  float lineHeight=(float)(n-1)*PG_ROW+PG_ROW_H;
  ESCALA_INI();
  if(line>.001f) {
    float h=lineHeight*line;
    gfx_cor((GfxRect){PG_LEFT,PG_TOP,PG_LINE,h},fminf(.5f,1.6f/h),
            1.0f,1.0f,1.0f,alpha);
  }
  for(int i=0;i<n;i++) {
    float onset=.7f+i*.28f+.08f;
    float leave=exitStart+(n-1-i)*.21f+.06f;
    float itemA=pgEase((tg-onset)/.20f)*(1.0f-pgEase((tg-leave)/.15f))*alpha;
    if(itemA<.003f)continue;
    float yy=PG_TOP+i*PG_ROW;
    const char *label=parental_rotulo(i),*severity=parental_gravidade(i);
    if(!label||!severity)continue;
    // 11sp at 1.6 scene units per dp; identical baseline and official opacities.
    TxtLinha title=txt_linha(TXT_G20B,label,255,255,255,255);
    TxtLinha dot=txt_linha(TXT_G18R," · ",255,255,255,255);
    TxtLinha level=txt_linha(TXT_G18R,severity,255,255,255,255);
    float tx=PG_LEFT+PG_LINE+PG_GAP;
    float textY=yy+(PG_ROW_H-title.h)*.5f;
    txt_desenhar_alpha(title,tx,textY,itemA*.85f);
    txt_desenhar_alpha(dot,tx+title.w,yy+(PG_ROW_H-dot.h)*.5f,itemA*.40f);
    txt_desenhar_alpha(level,tx+title.w+dot.w,yy+(PG_ROW_H-level.h)*.5f,itemA*.50f);
  }
  ESCALA_FIM();
}
