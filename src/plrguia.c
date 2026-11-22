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

#define PG_LEFT  68.0f
#define PG_TOP   72.0f
#define PG_ROW   29.0f
#define PG_LINE  3.0f

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
  float lineHeight=(float)n*20.0f+(float)(n-1)*9.0f;
  float accentR,accentG,accentB;
  ajustes_acento(&accentR,&accentG,&accentB);
  ESCALA_INI();
  // Subtle dark veil only near the top-left text, not a giant glass clock.
  GfxRect shade={PG_LEFT-20.0f,PG_TOP-23.0f,490.0f,lineHeight+56.0f};
  gfx_cor(shade,16.0f/shade.h,.018f,.022f,.032f,.25f*alpha);
  if(line>.001f) {
    float h=lineHeight*line;
    gfx_cor((GfxRect){PG_LEFT,PG_TOP,PG_LINE,h},.5f,
            accentR,accentG,accentB,alpha);
  }
  for(int i=0;i<n;i++) {
    float onset=.7f+i*.28f+.08f;
    float leave=exitStart+(n-1-i)*.21f+.06f;
    float itemA=pgEase((tg-onset)/.20f)*(1.0f-pgEase((tg-leave)/.15f))*alpha;
    if(itemA<.003f)continue;
    float yy=PG_TOP+i*PG_ROW;
    const char *label=parental_rotulo(i),*severity=parental_gravidade(i);
    if(!label||!severity)continue;
    TxtLinha title=txt_linha(TXT_G16B,label,243,244,248,255);
    TxtLinha dot=txt_linha(TXT_ILHA_APOIO," · ",130,134,145,255);
    TxtLinha level=txt_linha(TXT_ILHA_APOIO,severity,183,188,199,255);
    float tx=PG_LEFT+PG_LINE+14.0f;
    txt_desenhar_alpha(title,tx,yy,itemA*.9f);
    txt_desenhar_alpha(dot,tx+title.w,yy,itemA*.5f);
    txt_desenhar_alpha(level,tx+title.w+dot.w,yy,itemA*.68f);
  }
  ESCALA_FIM();
}
