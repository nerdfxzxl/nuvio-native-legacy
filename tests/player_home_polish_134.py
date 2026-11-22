#!/usr/bin/env python3
"""Focused #134 regression contract: mobile/TV pill, anchored icons, clean hero.

This source-level check is inexpensive; native compiler gate runs separately.
A passing contract does not replace human phone + TV visual acceptance.
"""
from pathlib import Path

player=Path("src/player.c").read_text(encoding="utf-8")
home=Path("src/home.c").read_text(encoding="utf-8")
gfx=Path("src/gfx.c").read_text(encoding="utf-8")
gfxh=Path("src/gfx.h").read_text(encoding="utf-8")

def check(condition, message):
    if not condition:
        raise AssertionError(message)
    print("PASS",message)

pill=player[player.index("  // --- Shared glass transport:"):
            player.index("  // --- tempo",player.index("  // --- Shared glass transport:"))]
check("plrui_material(capsule" in pill and "ponteiro_alvo(target.x" in pill,
      "one glass pill with working per-button pointer targets")
check("if (!toque)" not in pill and "if (toque)" not in pill
      and "transporteDuasLinhas()" not in pill,
      "touch input no longer selects a separate rendering layout")
check("float baseX=(NV_VTELA_W-baseW)*0.5f;" in pill
      and "float x=baseX;" in pill
      and "baseW+2.0f*expanded+28.0f" in pill,
      "icon row stays anchored while glass shell expands and collapses")
check("const float ix = r.x + (diameter - size) * 0.5f;" in pill
      and "fminf(maxExtra" in pill and "float tx = ix + size + 12.0f" in pill,
      "icon stays anchored, label is revealed to its right")
check("gfx_recorte(r.x, r.y, r.w, r.h);" in pill
      and "(1.0f - reveal) * 12.0f" in pill,
      "label is softly translated and clipped within expanding button")
check("return PLR_BARRA_Y;" in player[player.index("static float transporteBarraY(void)"):]
      and "return 0;" in player[player.index("static int transporteDuasLinhas(void)"):
                              player.index("static int transportePrimeiraLinha(void)")],
      "mobile transport uses TV vertical alignment and single row")
check("return !ehCanal() && NV_VTELA_W - PLR_MARGEM" in player,
      "compact layout depends on actual width, not touch detection")
check("if (lay == HOME_LAYOUT_MODERNA && focoHero) base += ANC(82.0f);" in home
      and "else if (lay == HOME_LAYOUT_DINAMICA) base += ANC(70.0f);" in home,
      "main hero copy and View Details move nearer bottom together")
check("gfx_hero_sem_degrade_atual = 1;\n    desenhaHero(agora, pd);\n    gfx_hero_sem_degrade_atual = 0;" in home,
      "clean backdrop mode is scoped only around Home hero rendering")
check("!gfx_hero_sem_degrade_atual && aTrailer > 0.0f" in home,
      "dark gradient does not come back over hero trailer")
check("extern int gfx_hero_sem_degrade_atual;" in gfxh
      and "int gfx_hero_sem_degrade_atual;" in gfx,
      "shader flag is defined once and exposed to Home")
check(gfx.count('if (uFoco < -0.5) { gl_FragColor') == 4,
      "four home hero shader variants support clean artwork")
check(gfx.count("float rampa = uFoco < -0.5 ? 0.0") == 2,
      "both fused/crossfade shaders suppress the gradient")
check("(gfx_hero_sem_degrade_atual && heroMode) ? -1.0f : foco" in gfx,
      "only hero modes receive gradient bypass uniform")

# Geometric contract for responsive capsule: no action focus means a small
# resting pill; the occupied region expands only while focus is active. The
# layout origin for icon positions never follows that expansion.
baseW=7*68+6*12
labelExtras=[71,92,133,65,180,97,106]
def shell(focus):
    extra=sum(labelExtras[i]*focus[i] for i in range(7))
    return baseW+2.0*extra+28,extra
rest,extent=shell([0.0]*7)
assert extent==0.0 and rest==baseW+28
for index in range(7):
    focus=[0.0]*7
    focus[index]=1.0
    expanded,extent=shell(focus)
    assert expanded>rest and extent>0
    assert baseW+extent<=expanded
print("PASS capsule collapses when unfocused; each selected name expands glass")

check("!tempoFoco && !barraFoco" in player
      and "focoB[i]=anim_mola" in player,
      "video-time focus excludes playback highlight and no focus bounce")
check("if(!ehCanal() && duracaoSeg > 0.0f)" in player
      and "if(bufFrac<barraBufferMem)bufFrac=barraBufferMem;" in player,
      "buffer endpoint is retained independent of seek-only OSD")
check("return layout_modo_mobile() && homeRetratoTelefone();" in home,
      "landscape mobile no longer forces a half-height hero")
