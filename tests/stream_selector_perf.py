#!/usr/bin/env python3
"""Selector-only source contract plus a compiled spring timing regression."""
from pathlib import Path
import subprocess, tempfile
root = Path(__file__).resolve().parents[1]
s = (root / "src/streams.c").read_text()
t = (root / "src/stream_card_android.inc").read_text()
k = (root / "android/app/src/main/java/space/nuvio/nativelegacy/StreamCardText.kt").read_text()
assert s.count("refinarCartoes(automaticaDaFolha());") == 1
assert "if (y>folhaBase) break;" in s
assert "drawCardText(i,t->cardWidth,t->cardHeight,&t->cardRetryAt" in s
assert "row = cardLayout(env, index, width, 1);" in t
assert t.index("if (!complete) return 0;") < t.index("gfx_rect((GfxRect){x, y + top")
assert "queuedLayouts.entries.removeIf { it.value.isDone }" in k
code = r'''
#include <assert.h>
#include <math.h>
#include "stream_scroll_clock.h"
int main(void) {
  float v=0, x=0, v2=0, y=0;
  for (int i=0;i<60;i++) x=nv_stream_scroll_step(&v,x,500,1.f/120,11.5f);
  for (int i=0;i<5;i++) y=nv_stream_scroll_step(&v2,y,500,.1f,11.5f);
  assert(fabsf(x-y)<.01f && fabsf(v-v2)<.02f);
  float a=0,b=0, va=0,vb=0;
  for (int i=0;i<30;i++) a=nv_stream_scroll_step(&va,a,500,1.f/60,11.5f);
  for (int i=0;i<15;i++) b=nv_stream_scroll_step(&vb,b,500,1.f/30,11.5f);
  assert(fabsf(a-b)<.01f);
  assert(nv_stream_scroll_step(&v,10,20,2,11.5f)<20);
  anim_politica_reduzida=1;
  assert(nv_stream_scroll_step(&v,10,20,.1f,11.5f)==20 && v==0);
  return 0;
}
'''
with tempfile.TemporaryDirectory() as d:
    exe = Path(d) / "clock"
    subprocess.run(["cc","-std=c11","-Wall","-Werror","-I",str(root/"src"),"-x","c","-","-lm","-o",str(exe)],input=code,text=True,check=True)
    subprocess.run([str(exe)],check=True)
print("Selector: 120/60/30/10 Hz elapsed-time equivalence, cache recovery and one viewport pass contracts PASS")
