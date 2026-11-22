#!/usr/bin/env python3
"""Regression contract for player-panel refinements (#131).

This checks native source routing and UI requirements at compile/preflight
time. It cannot replace real D-pad or audio/video output tests on Android TV.
"""
from pathlib import Path

def source(name):
    return Path("src", name).read_text(encoding="utf-8")

leg=source("legendasui.c")
faixas=source("faixas.c")
plr=source("player.c")
plrui=source("plrui.c")
parental=source("plrguia.c")
video=source("video.h")
ass=source("assrender.c")

def check(test, message):
    assert test, message
    print("PASS", message)

event=leg[leg.index("int legendasui_evento("):leg.index("// --- drawing the panel")]
check(event.index("if(fonteModo) {") < event.index("if (ok && e->type == SDL_KEYUP)"),
      "source-tab OK dispatch precedes old hold-menu interception")
check('ling_casa(cand[i].idioma,"en")' in leg,
      "source subtitle list is explicitly English-only")
check("ponteiroFonteEscolher" in leg and "fonteScroll=anim_mola" in leg,
      "source pills expose pointer targets and animated offset")
check('"Segure OK"' not in leg, "hold-for-options text was removed")
check("if(modo&&e->type==SDL_KEYDOWN" in faixas and "aberta=0;legendaTopoFoco=0" in faixas,
      "Back is available from every subtitle tab")
check('"Show Less"' in faixas and "plrui_material(small" not in faixas,
      "Show All/Less is a plain label")
check("if (soBarra) cyTempo = yBarra - 25.0f;" in plr,
      "only seek-only OSD moves time above the progress bar")
fn=plrui[plrui.index("void plrui_barra("):plrui.index("void plrui_trilho(")]
check("h = foco ? 10.0f : 8.0f" in fn and "for (i = 0; i < nCaps" not in fn
      and "gfx_rect(" not in fn,
      "seekbar is thicker without chapter marks or focused dot")
check("barraBufferMem" in plr, "buffer end is retained for seek-only OSD")
check("plrilha_pedir" not in parental and "PG_LEFT" in parental,
      "parental guide is top-left and independent of the clock")
check("offsetHalfPct" in video and "shadowOpacity" in video
      and "hdrBrightness" in video and "shadowSize" in video,
      "precise subtitle style fields exist")
check("p->shadowSize" in ass and "p->shadowOpacity" in ass,
      "ASS renderer receives adjustable shadow size/opacity")
