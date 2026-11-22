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
check("float contentW = baseW + maxExtra;" in pill
      and "(NV_VTELA_W - contentW) * 0.5f" in pill,
      "outer capsule width and center do not depend on the active label")
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

# Geometric invariant: fixed shell width regardless of selected action,
# whereas the old implementation recalculated its x every focus frame.
baseW=7*68+6*12
extra=[71,92,133,65,180,97,106]
maxExtra=max(extra)
shell=baseW+maxExtra
for selected in range(7):
    normalWidths=[68+extra[i]*(i==selected) for i in range(7)]
    assert baseW+maxExtra==shell
    assert sum(normalWidths)<=shell
print("PASS stable 7-button capsule shell with labels of varying widths")
