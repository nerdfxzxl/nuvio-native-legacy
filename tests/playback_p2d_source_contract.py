#!/usr/bin/env python3
"""P2-D post-READY source contracts; run from a full checkout (not Android QA)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def src(path):
    return (ROOT / path).read_text(encoding="utf-8")
def between(s, start, stop):
    assert start in s and stop in s, (start, stop)
    return s.split(start, 1)[1].split(stop, 1)[0]

p = src("src/player.c")
a = src("src/abrecine.c")
h = src("src/abrecine.h")
v = src("src/video_android.c")
app = src("src/app.c")
nav = src("src/loading_nav_p2c.h")
policy = src("src/loading_handoff_p2d.h")
k = src("android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt")
checkpoints = src("docs/PLAYBACK_50_IMPLEMENTATION_PROGRESS.md")

checks = []
def check(name, passed):
    checks.append((name, bool(passed)))

pronto = between(v, "int  video_pronto(void) {", "int  video_ativo(void)")
drawing = between(p, "void player_desenhar(Uint32 agora)", "void player_osd")
nav_branch = between(p, "// Loading owns D-pad/OK", "// PAINEL DE PAUSA")
back_branch = between(p, "if (k == SDLK_ESCAPE || k == SDLK_AC_BACK", "// A FONTE NAO ABRIU")
app_back = between(app, "// Back during startup/recovery", "// A BARRA LATERAL POR CIMA")
app_change = between(app, "// The slow-loading escape", "// CANAL ESCOLHIDO NO GUIA")

check("P2-D media readiness checks READY AND actual rendered frame or confirmed audio-only",
      "nv_p2d_media_ready(prontoLoad, primeiroQuadro, somenteAudio)" in pronto)
check("no elapsed-time frame fallback", "ANDROID_FURO_PRAZO_MS" not in v and
      "tocandoDesde" not in pronto and "3000u" not in pronto)
check("generation guard still rejects stale Media3 events",
      "geracao > 0 && (unsigned)geracao == sessao" in v)
check("current-surface frame reset on invalidation",
      "case EV_SUPERFICIE_INVALIDADA: superficieEstavel = 0; quadroSuperficie = 0;" in v)
check("rendered current-surface frame recorded",
      "primeiroQuadro = quadroSuperficie = 1" in v)
check("new session and stop clear surface frame",
      v.count("superficieEstavel = quadroSuperficie = 0") >= 2)
check("ready keeps session's first frame for subsequent buffering",
      "return ativo && quadroSuperficie;" in v and "primeiroQuadro = 0" not in
      between(v, "case EV_SUPERFICIE_INVALIDADA", "case EV_AUDIO_SEM_DECODER"))
check("loading-owner helper includes pending visual handoff",
      "!!cinematic_pending" in policy and "!is_live && !source_error && !exiting" in policy)
check("loading nav uses full visible/finishing veil",
      "int abrecine_navegacao(const AbreCine *a)" in h and
      "a->viva || a->veu > 0.0f" in a)
check("player public loading state includes pending cinematic",
      "nv_p2d_loading_owner(esperandoFonte, comVideo, video_pronto()" in p and
      "abrecine_navegacao(&cine)" in p)
check("backend state separate from loading nav, no recursive fade deadlock",
      "static int fonteAindaAbrindo(void)" in p and
      "abrecine_passo(&cine, !erroFonte && !saindo && !ehCanal() &&" in drawing and
      "(fonteAindaAbrindo() || dvtela_ativa())" in drawing)
check("cinematic handoff gated by current stable first frame",
      "nv_p2d_visual_ready(video_quadro_visto(), video_superficie_estavel()" in drawing)
check("no fake audio-only from three-second fallback",
      "int  video_somente_audio(void) { return ativo && prontoLoad && somenteAudio; }" in v)
check("surface instability re-arms fade rather than leaking hardware",
      "if (!imagem) { a->fim = 0; a->veu = 1.0f; return; }" in a)
check("exact 200ms stable handoff and reduced-motion path",
      "agora - a->fim >= 200u" in a and "reduzidas ? 0.0f" in a)
check("15-second escape clock survives READY and surface instability",
      "nv_loading_change_ready(agora, cine.desde," in p and
      "abrecine_navegacao(&cine)" in between(p, "static int cinemaEscapeDisponivel", "// O botao"))
check("exact Down focus, no automatic Change Stream selection",
      "if (k == SDLK_DOWN) { cinemaEscapeFoco = 1; return; }" in nav_branch)
check("OK requires explicitly focused, visible escape",
      "nv_loading_change_confirm(cinemaEscapeDisponivel(SDL_GetTicks()), cinemaEscapeFoco)" in nav_branch)
check("direct OK reveals diagnostics",
      "cine.detalhada = 1;" in nav_branch and "abrindoExp = 1;" in nav_branch)
check("other loading navigation cannot leak to OSD",
      "if (k == SDLK_LEFT || k == SDLK_RIGHT) return;" in nav_branch)
check("Back does not merely dismiss controls during cinema",
      "!player_carregando() && !video_bufferando_ms()" in back_branch and
      "saindo = 1; pediuSair = 1;" in back_branch)
check("app Back invalidates generation and pending URL work",
      "player_carregando() || video_reconectando()" in app_back and
      "novaGeracaoFonte();" in app_back and "limparFontePendente();" in app_back)
check("app Back cancels torrent and diagnostics",
      "torrentSessao++;" in app_back and "p2pmotor_cancelar();" in app_back and
      "diagnostico_cancelar_vazao();" in app_back)
check("Change Stream invalidates source before opening selector",
      "aguardandoFonte == 2 || player_carregando() || video_reconectando()" in app_change and
      "novaGeracaoFonte(); limparFontePendente();" in app_change and
      "folhaParaTocar = 1; folhaCancelarTocar = 0;" in app_change)
check("focus retained until actual cinematic end",
      "if (!abrecine_navegacao(&cine) || erroFonte || saindo || ehCanal()) cinemaEscapeFoco = 0;" in drawing)
check("new source resets visual state, clock and focused escape",
      "memset(&cine, 0, sizeof cine);" in
      between(p, "void player_definir_fonte(const char *url)", "void player_voltar_a_esperar") and
      "cinemaEscapeFoco = 0;" in between(p, "void player_definir_fonte(const char *url)", "void player_voltar_a_esperar"))
check("error and Back clear focus",
      "cinemaEscapeFoco = 0;" in between(p, "void player_erro_fonte(void)", "void player_erro_fonte_motivo") and
      "cinemaEscapeFoco = 0;" in back_branch)
check("Live tune remains compact, not cinematic",
      "player_carregando() && ehCanal()" in drawing and
      "(!is_live" in policy and "&& !ehCanal()" in drawing)
check("Kotlin stability still awaits rendered first frame",
      "player != null && quadroVisto && !estavelEmitido" in k and
      "ev(EV_SUPERFICIE_INVALIDADA)" in k and "ev(EV_PRIMEIRO_QUADRO)" in k)
check("P2-B frame hold retained", "FrameHoldP2B" in k and "capturarRetido" in k)
check("P2-A genuine P2P measurements and unknown readiness retained",
      "Verified from start" in p and "medicao_arquivo" in p and
      "Q18" in checkpoints)
check("P2-C native shared arc and 15s policy preserved",
      "NV_LOADING_CHANGE_MS 15000u" in nav and "plrui_arco(" in drawing)
check("P1 identity and cancellation protections preserved",
      "fonteAcaoInvalidar()" in p and "novaGeracaoFonte();" in app)
check("protected post-134 handoff still present",
      (ROOT / "docs/PLAYER_PENDING_REFINEMENTS_AFTER_134.md").exists())
check("standalone C11 first-frame and handoff test committed",
      (ROOT / "tests/playback_p2d_handoff.c").exists() and
      (ROOT / "tests/playback_p2d_handoff.sh").exists())

bad = [name for name, passed in checks if not passed]
for name, passed in checks:
    print(("PASS" if passed else "FAIL") + ": " + name)
print(f"P2-D source contract: {len(checks)-len(bad)}/{len(checks)}")
if bad:
    raise SystemExit(1)
