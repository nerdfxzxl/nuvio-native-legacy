// Episodes / Play Manually: three clean nested outline cards, matching
// the supplied reference silhouette. No bitmap, fills or watermark.
#ifndef NV_EPISODEICON_H
#define NV_EPISODEICON_H
#include "gfx.h"
static inline void episodeicon_draw(GfxRect r,float cr,float cg,float cb,float alpha) {
  const float sx=r.w/32.0f, sy=r.h/32.0f;
  // One dark, readable stroke on the white focused button; the caller sets
  // the ink white for unfocused buttons. Back/middle cards are L-shaped.
  const float tx=2.7f*sx, ty=2.7f*sy;
  // Back card: only the top and right edges, ending behind the middle.
  gfx_cor((GfxRect){r.x+13*sx,r.y+2*sy,17*sx,ty},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){r.x+30*sx-tx,r.y+2*sy,tx,17*sy},0,cr,cg,cb,alpha);
  // Middle card: staggered top/right edges; no lower/left border.
  gfx_cor((GfxRect){r.x+8*sx,r.y+9*sy,17*sx,ty},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){r.x+25*sx-tx,r.y+9*sy,tx,16*sy},0,cr,cg,cb,alpha);
  // Foreground is the ONLY complete rectangle, with an empty center.
  const float x=r.x+2*sx,y=r.y+15*sy,w=17*sx,hh=15*sy;
  gfx_cor((GfxRect){x,y,w,ty},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){x,y+hh-ty,w,ty},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){x,y+ty,tx,hh-2*ty},0,cr,cg,cb,alpha);
  gfx_cor((GfxRect){x+w-tx,y+ty,tx,hh-2*ty},0,cr,cg,cb,alpha);
}
#endif
