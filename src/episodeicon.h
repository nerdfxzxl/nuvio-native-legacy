// Bold outlined overlapping sheets, with fully transparent interiors.
// Use the ink supplied by the caller: white at rest, black on white focus.
#ifndef NV_EPISODEICON_H
#define NV_EPISODEICON_H
#include "gfx.h"
static inline void episodeicon_draw(GfxRect r,float cr,float cg,float cb,float alpha) {
  const float sx=r.w/32.0f, sy=r.h/32.0f;
  const float thickX=2.25f*sx, thickY=2.25f*sy;
  const GfxRect sheets[3]={
    {r.x+10*sx,r.y+2*sy,20*sx,20*sy},
    {r.x+5*sx,r.y+7*sy,20*sx,20*sy},
    {r.x+1*sx,r.y+12*sy,20*sx,18*sy}
  };
  // Back to front; no opaque fills and no separate background plate.
  for(int i=0;i<3;i++) {
    GfxRect q=sheets[i];
    gfx_cor((GfxRect){q.x,q.y,q.w,thickY},0,cr,cg,cb,alpha);
    gfx_cor((GfxRect){q.x,q.y+q.h-thickY,q.w,thickY},0,cr,cg,cb,alpha);
    gfx_cor((GfxRect){q.x,q.y+thickY,thickX,q.h-2*thickY},0,cr,cg,cb,alpha);
    gfx_cor((GfxRect){q.x+q.w-thickX,q.y+thickY,thickX,q.h-2*thickY},0,cr,cg,cb,alpha);
  }
}
#endif
