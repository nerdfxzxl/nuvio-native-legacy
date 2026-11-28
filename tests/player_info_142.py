#!/usr/bin/env python3
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
def read(p): return (root/p).read_text()
plr=read("src/player.c"); ui=read("src/estatvivoui.c"); guide=read("src/plrguia.c")
rail=read("src/plrui.c")
assert "float bufferedW=w*buf;" in rail
assert "fminf(.5f,bufferedW/(2*h))" in rail
assert plr.index("if (infoPanelFoco && !estatvivo_player_aberto())") < plr.index("if (!visivel) {\n    // Tudo que acorda daqui")
assert "if (estatvivo_player_scroll(1))" in plr
assert "infoPanelFoco=0; barraFoco=1; tempoFoco=skipFoco=0; soBarra=0;" in plr
assert "!tempoFoco && !infoPanelFoco && botao == PLR_INFO" in plr
assert "Movie HUD matches idle" in plr
detail=ui[ui.index("static void playerInfoMontarLinhas(void) {"):ui.index("static void playerInfoMaterial")]
for label in ("Buffer","Back buffer","Size","File bitrate","CPU","Memory","Thermal","Network speed","Dropped","LAST SESSION"):
    assert f'playerInfoAdicionar(0,"{label}"' not in detail
assert 'playerInfoAdicionar(0,"Source metadata"' in detail
assert "INFO_LABEL (64.0f*INFO_DP)" in ui and "TXT_MONO18" in ui
assert "gfx_cor(p,INFO_RADIUS/p.h,0,0,0,.60f*a)" in ui
assert "if(playerInfoFocused) gfx_anel_fora" in ui
assert "PG_LINE  4.8f" in guide and "1.6f/h" in guide
assert "PG_ROW   32.0f" in guide
print("PASS selector-independent player info, focus, rail and parental geometry contracts")
code=r'''
#include <assert.h>
#include "playback_ui_metrics.h"
int main(void) {
  int r,l;
  assert(nv_ui_buffer_estimate(80LL<<20,12LL<<20,64,1,4,4,&r,&l)==1 && r<=l);
  assert(nv_ui_buffer_estimate(80LL<<20,12LL<<20,256,1,4,4,&r,&l)==-1 && r>l);
  assert(nv_ui_buffer_estimate(192LL<<20,32LL<<20,0,1,4,4,&r,&l)==1 && r<=l);
  assert(nv_ui_buffer_estimate(0,0,64,1,4,4,&r,&l)==0);
}
'''
with tempfile.TemporaryDirectory() as d:
    exe=Path(d)/"budget"
    subprocess.run(["cc","-std=c11","-Wall","-Werror","-I",str(root/"src"),"-x","c","-","-o",str(exe)],input=code,text=True,check=True)
    subprocess.run([str(exe)],check=True)
print("PASS RAM budget safe/over-boundary/auto/unavailable")
