#include "detail.c"
#include <assert.h>
static float testScale = 1;
float gfx_escala_tela_txt(void) { return testScale; }
void home_hero_rect(float *x,float *y,float *w,float *h) { *x=*y=0; *w=1920; *h=1080; }
int main(void) {
  for (int scale=1; scale<=3; scale++) {
    testScale=0.5f+scale*.5f;
    for (int card=0;card<3;card++) {
      GfxRect physical={143+410*card,377+33*card,226+93*card,314-11*card};
      item.rect=homeRectDetalhe(physical);
      GfxRect forward[101];
      float a;
      for (int i=0;i<=100;i++) {
        t=i/100.f; backdropRect(&forward[i],&a);
        assert(forward[i].w >= item.rect.w-.01f);
        if(i) assert(forward[i].w>=forward[i-1].w && forward[i].h>=forward[i-1].h);
      }
      assert(fabsf(forward[0].x*testScale-physical.x)<.01f);
      assert(fabsf(forward[0].y*testScale-physical.y)<.01f);
      assert(fabsf(forward[100].w*testScale-1920)<.01f && fabsf(forward[100].h*testScale-1080)<.01f);
      for (int i=100;i>=0;i--) {
        GfxRect reverse; t=i/100.f; backdropRect(&reverse,&a);
        assert(!memcmp(&reverse,&forward[i],sizeof reverse));
      }
    }
  }
  puts("PASS: forward/reverse bounds coincide for 101 samples, 3 scale settings, 3 launch rectangles.");
}
