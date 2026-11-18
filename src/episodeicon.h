// Three solid overlapping sheets matching the supplied Episodes icon.
// Draw back to front so each sheet remains visible through its silhouette.
#ifndef NV_EPISODEICON_H
#define NV_EPISODEICON_H
#include "gfx.h"
static inline void episodeicon_draw(GfxRect r,float cr,float cg,float cb,float alpha) {
  float sx=r.w/32.0f,sy=r.h/32.0f;
  // Three opaque, filled overlapping rectangles. The background stays
  // transparent; focus inversion uses exactly the caller's ink colour.
  gfx_cor((GfxRect){r.x+10*sx,r.y+2*sy,20*sx,20*sy},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){r.x+5*sx,r.y+7*sy,20*sx,20*sy},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){r.x+1*sx,r.y+12*sy,20*sx,18*sy},0,cr,cg,cb,alpha);
}

#endif
