#!/usr/bin/env python3
"""P2-C: read-only native loader/navigation regression contract (not device tests).

Run from a complete repo checkout: python3 tests/playback_p2c_navigation_contract.py
The companion shell test compiles the *actual* small loading policy on host C11.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

checks = []
def check(label, ok):
    checks.append((label, bool(ok)))

player = source("src/player.c")
app = source("src/app.c")
streams = source("src/streams.c")
sh = source("src/streams.h")
arc = source("src/plrui.c")
policy = source("src/loading_nav_p2c.h")
cinema = source("src/abrecine.c")
profile = source("src/perfilsel.c")
p2p = source("src/p2pmotor.c")
android = source("android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt")
bridge = source("src/video_android.c")
holder = source("android/app/src/main/java/space/nuvio/nativelegacy/FrameHoldP2B.kt")

def between(text, first, last):
    assert first in text and last in text, (first, last)
    return text.split(first, 1)[1].split(last, 1)[0]

nav = between(player, "// Loading owns D-pad/OK", "// PAINEL DE PAUSA")
open_sheet = between(app, "// The slow-loading escape", "// CANAL ESCOLHIDO NO GUIA")
focus = between(streams, "void stream_folha_focar_fonte", "// Q11: only the recovery path")
escape_card = between(player, "// The delayed escape also exists", "// O MODAL DO ERRO")

check("15-second monotonic SDL interval", "NV_LOADING_CHANGE_MS 15000u" in policy and "(uint32_t)(now - started)" in policy)
check("time guard owned by active cinema", "nv_loading_change_ready(agora, cine.desde," in player and "abrecine_navegacao(&cine)" in player)
check("only Down focuses", "if (k == SDLK_DOWN) { cinemaEscapeFoco = 1; return; }" in nav)
check("Up clears explicit focus", "if (k == SDLK_UP && cinemaEscapeFoco) { cinemaEscapeFoco = 0; return; }" in nav)
check("OK confirms only after two guards", "nv_loading_change_confirm(cinemaEscapeDisponivel(SDL_GetTicks()), cinemaEscapeFoco)" in nav)
check("other OK opens detailed loading", "cine.detalhada = 1;" in nav and "abrindoExp = 1;" in nav)
check("no accidental implicit selector/menu from loading OK", "addons_busca_parcial()" not in nav and "pedAjFonte = 1" not in nav)
check("OSD ignores D-pad during loading", "if (k == SDLK_LEFT || k == SDLK_RIGHT) return;" in nav)
check("Space behaves as OK", "k == SDLK_SPACE" in nav)
check("media keys not blanketed", "Physical Play/Pause" in nav)
check("source acceptance resets old escape focus", "cinemaEscapeFoco = 0; /* A newly accepted source" in player)
check("back resets old escape focus", "cinemaEscapeFoco = 0;\n    saindo = 1; pediuSair = 1;" in player)
check("player waiting reset clears escape focus", "cinemaEscapeFoco = 0;\n  erroTitulo[0]" in player)
check("cinema and card share the same 15s predicate", player.count("if (cinemaEscapeDisponivel(agora))") >= 2)
check("card grows for visible control", "cinemaEscapeDisponivel(abrindoAgora) ? 34.0f" in player)
check("card shows label and visible underline", "Change Stream" in escape_card and "if (cinemaEscapeFoco)" in escape_card)
check("cine focus decoration", "float xx = canto ? tx : cx - l.w * .5f;" in player)
check("cinema logo static", "artwork/logo must be static" in player)
check("cinema first-frame gate unchanged", "nv_p2d_visual_ready(video_quadro_visto(), video_superficie_estavel()" in player and "imagem = imagem && video_quadro_visto() && video_superficie_estavel()" in player)
check("reduced motion from common arc", "nv_arc_quadro(agora, ajustes_animacoes_reduzidas())" in arc)
check("shared arc endpoints match stroke alpha", arc.count("cb, arcAlfa") >= 3)
check("shared arc is still single provider", all(x in arc for x in ("void plrui_arco(", "void plrui_anel(", "void plrui_respira(")))
check("profile selector no legacy dot loader", "plrui_arco(cx, cy, av.w * 0.48f" in profile)
for mod in ("aovivoui.c", "dvtelaui.c", "ilha.c", "plrilha.c", "pessoas.c", "recenviar.c"):
    check("shared arc: " + mod, bool(re.search(r"plrui_(respira|anel|arco)\(", source("src/" + mod))))
check("selector semantic refresh arrow retained", "gfx_girar(spin" in streams and '"aj_rotate-cw"' in streams)
check("selector reduced-motion refresh preserved", "if(!ajustes_animacoes_reduzidas()) spin=" in streams)
check("selector focus decorations intact", "gfx_anel_fora" in streams)
check("Back invalidates queued URL work", "if (carga && !saindoAntes && player_quer_sair())" in app and "novaGeracaoFonte();" in app)
check("Back cancels P2P and active diagnostics", "torrentSessao++;" in app and "p2pmotor_cancelar();" in app and "diagnostico_cancelar_vazao();" in app)
check("change stream cancels accepted URLs too", "aguardandoFonte == 2 || player_carregando() || video_reconectando()" in open_sheet)
check("change stream owned by manual choice", "folhaParaTocar = 1; folhaCancelarTocar = 0;" in open_sheet)
check("selector reopens without autoplay", "nomeParaFolha(); stream_folha_abrir();" in open_sheet and "stream_folha_escolheu" not in open_sheet)
check("selected row restored only from loading", "if (restaurarFocoCarga) stream_folha_focar_fonte(fonteAnterior);" in open_sheet)
check("selector focus API declared", "void stream_folha_focar_fonte(int indice);" in sh)
check("selector focus does not play a source", "foco = focoVisto = row;" in focus and "stream_folha_escolheu" not in focus and "player_" not in focus)
check("recovery first-available policy preserved", "void stream_folha_focar_primeira_disponivel(void)" in streams and "p1RecuperacaoFoco" in streams)
check("selected P2P piece metrics retained", "get_stream_stats" in source("src/p2pmotor_motor.c") and "medicao_arquivo" in player)
check("P2P unknown readiness stays indeterminate", "No readiness percentage" in player and "Verified from start" in player)
check("P2-B Android frame helper remains", "FrameHoldP2B" in holder and "PixelCopy" in android)
check("P2-B source-scoped recovery bridge remains", "same-source" in bridge.lower() or "mesma fonte" in bridge.lower())
check("Android HTTP telemetry untouched", "TransferListener" in android)
check("audio-only first-frame escape remains", "video_somente_audio()" in player)
check("live tune remains lightweight", 'player_carregando() && ehCanal()' in player)
check("cinematic opening logic retains real frame", "if (!imagem) { a->fim = 0; a->veu = 1.0f; return; }" in cinema)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(("PASS" if ok else "FAIL") + ": " + name)
print(f"P2-C source contract: {len(checks) - len(failed)}/{len(checks)}")
if failed:
    raise SystemExit(1)
