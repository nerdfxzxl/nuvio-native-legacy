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
#define PG_LEFT  60.0f
#define PG_TOP   30.0f
#define PG_ROW   58.0f
#define PG_LINE  8.0f
#define PG_GAP   29.0f

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
  float lineHeight=(float)(n-1)*PG_ROW+47.0f;
  ESCALA_INI();
  if(line>.001f) {
    float h=lineHeight*line;
    gfx_cor((GfxRect){PG_LEFT,PG_TOP,PG_LINE,h},.5f,
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
    // The 11sp Compose labels read like ~22 native virtual pixels on this
    // renderer. All three components share one baseline and stagger together.
    TxtLinha title=txt_linha(TXT_G26B,label,244,245,247,255);
    TxtLinha dot=txt_linha(TXT_G22M," · ",140,140,144,255);
    TxtLinha level=txt_linha(TXT_G22M,severity,170,173,178,255);
    float tx=PG_LEFT+PG_LINE+PG_GAP;
    float textY=yy+10.0f;
    txt_desenhar_alpha(title,tx,textY,itemA*.90f);
    txt_desenhar_alpha(dot,tx+title.w,textY,itemA*.55f);
    txt_desenhar_alpha(level,tx+title.w+dot.w,textY,itemA*.65f);
  }
  ESCALA_FIM();
}
