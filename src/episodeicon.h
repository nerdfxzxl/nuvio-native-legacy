// Exact three overlapping outlined rectangles supplied as the Episodes icon.
// Geometry is vector-based, so it stays sharp at all Android TV resolutions.
#ifndef NV_EPISODEICON_H
#define NV_EPISODEICON_H
#include "gfx.h"
static inline void episodeicon_outline(GfxRect r,float thickness,float cr,float cg,float cb,float a) {
  gfx_cor((GfxRect){r.x,r.y,r.w,thickness},0,cr,cg,cb,a);
  gfx_cor((GfxRect){r.x,r.y+r.h-thickness,r.w,thickness},0,cr,cg,cb,a);
  gfx_cor((GfxRect){r.x,r.y+thickness,thickness,r.h-2*thickness},0,cr,cg,cb,a);
  gfx_cor((GfxRect){r.x+r.w-thickness,r.y+thickness,thickness,r.h-2*thickness},0,cr,cg,cb,a);
}
static inline void episodeicon_draw(GfxRect r,float cr,float cg,float cb,float alpha) {
  float sx=r.w/32.0f,sy=r.h/32.0f,t=fmaxf(1.0f,r.w*.065f);
  // Backmost rectangle is up/right; frontmost is down/left.
  episodeicon_outline((GfxRect){r.x+10*sx,r.y+2*sy,20*sx,20*sy},t,cr,cg,cb,alpha);
  episodeicon_outline((GfxRect){r.x+5*sx,r.y+7*sy,20*sx,20*sy},t,cr,cg,cb,alpha);
  episodeicon_outline((GfxRect){r.x+1*sx,r.y+12*sy,20*sx,18*sy},t,cr,cg,cb,alpha);
}
#endif
