#!/usr/bin/env python3
"""P3 source contracts. Requires a full checkout; does not build an APK."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def read(path): return (ROOT / path).read_text(encoding="utf-8")
def part(src, first, last):
    assert first in src and last in src, (first, last)
    return src.split(first, 1)[1].split(last, 1)[0]

streams = read("src/streams.c")
addons = read("src/addons.c")
app = read("src/app.c")
player = read("src/player.c")
cinema = read("src/abrecine.c")
backend = read("src/video_android.c")
p1 = read("src/fonteid_p1.h")
policy = read("src/stream_reload_p3.h")
nav = read("src/loading_nav_p2c.h")
p2d = read("src/loading_handoff_p2d.h")
resume = read("src/fontevolta.h")
checks = []
def check(name, expr): checks.append((name, bool(expr)))

progressive = part(addons, "static void progDrenar(void) {", "static void progRepublicar(void) {")
search = part(addons, "static void buscarPedido(", "void addons_buscar(")
final = part(addons, "AddEstado addons_estado(void) {", "// --- publicacao por addon")
append = part(streams, "void stream_lista_acrescentar(", "int stream_ordem_addon(")
reload = part(streams, "int stream_recarregar_iniciar(", "static int nFiltrados(void)")
first = part(streams, "void stream_recarregar_primeira(void) {", "void stream_recarregar_concluir(void) {")
finish = part(streams, "void stream_recarregar_concluir(void) {", "int stream_recarregar_retendo(void) {")
restore = part(streams, "static void p3RestaurarAposResposta(void) {", "static void memoriaFolhaRestaurar(void)")
native_pick = part(streams, "int stream_folha_escolheu(int *out) {", "// PONTEIRO (#99)")
android_pick = part(streams, "int stream_folha_escolher_externo(", "void stream_folha_cancelar_externo(")
events = part(streams, "void stream_folha_evento(const SDL_Event *e) {", "void stream_folha_atualizar(float dt, Uint32 agora) {")
manual = part(app, "if (aguardandoFonte != 2 && stream_folha_escolheu(&fonte)) {", "if (player_aberto() && player_pediu_fontes())")
p1_manual = part(app, "// A manual switch of the same episode", "// Escolha manual num canal tambem entra")
change = part(app, "// The slow-loading escape", "// CANAL ESCOLHIDO NO GUIA")
recover = part(app, "static void selecionarAposFalhaVOD(const char *motivo) {", "static void recarregarFonteVOD(void) {")
direct = part(app, "static void abrirFonteManualDireta(", "static void tentarProximaFonteVOD(void) {")
check("per-addon progressive publication is UI-thread serialized",
      "progDrenar();" in addons and "stream_lista_acrescentar(local[q].a" in progressive)
check("addon ordering independent of response timing",
      "chave[exib[pos - 1]] > chave[idx]" in append and "exib[pos] = idx" in append)
check("stale / pending worker responses dropped",
      "!pendId[0] && escopoAindaAtual(&fioEscopo)" in progressive and
      "stream_invalidar(\"pending source target\")" in search)
check("reload requests bypass prior VOD cache",
      "if (renovar) fontecache_vod_apagar(" in search and
      "if (forcar && r == FC_ACERTO)" in search)
check("reload queued during existing request without clearing old cards",
      "if (forcar && !pendId[0]" in search and
      "p3Retendo = stream_recarregar_iniciar(id);" in search and
      "if (!p3Retendo) stream_invalidar(" in search)
check("same-target refresh retains cards until first fresh response",
      "if (forcar) p3Retendo = stream_recarregar_iniciar(id);" in search and
      "if (!p3Retendo) stream_definir_lista(NULL, 0);" in search and
      "stream_recarregar_primeira();" in progressive)
check("failed/empty reload clears retained stale cards",
      "stream_recarregar_primeira();" in final and
      "stream_recarregar_concluir();" in final and
      "stream_recarregar_primeira();" in finish)
check("async launch failure clears retained cards",
      "stream_recarregar_concluir();" in part(addons,"static void dispararBusca(void) {","static void buscarPedido("))
check("Android generation invalidation before old cards are retired",
      "listaGeracao++;" in reload and "nv_p3_reload_begin(&p3Reload)" in reload and
      "pthread_mutex_lock(&verTrava)" in reload)
check("atomic swap keeps old cards non-selectable until cleared",
      "p3Replacing = 1;" in first and "stream_definir_lista(NULL, 0);" in first and
      first.index("stream_definir_lista(NULL, 0)") < first.index("nv_p3_reload_first(&p3Reload)"))
check("only current generation can be confirmed",
      "nv_p3_reload_selectable(&p3Reload, escolhaGeracao, listaGeracao)" in native_pick and
      "nv_p3_reload_selectable(&p3Reload, geracao, listaGeracao)" in android_pick)
check("retained cards blocked from OK and held file choices",
      "p3Reload.phase == NV_P3_RELOAD_RETAINED) return;" in streams and
      "p3Reload.phase != NV_P3_RELOAD_RETAINED" in events and
      "if (p3Reload.phase == NV_P3_RELOAD_RETAINED) { ptEscolhaFonte = -1" in streams)
check("no invisible autocomplete from retained cards",
      "aguardandoFonte == 1 && !stream_recarregar_retendo()" in app)
check("focus snapshot survives refresh chip focus",
      "memoriaFolhaDaAba(filtro)" in reload and "p3FocusSource = lista[i]" in reload)
check("saved release matched immutably rather than source index or CDN URL",
      "nv_fonte_mesma_liberacao(&x, &y)" in streams and
      "p3MesmoLancamento(&p3FocusSource,&lista[i])" in restore and
      "strcmp(a->url,b->url)" in streams)
check("provider filter restored by stable provider key",
      "p3AddonKey" in reload and "strcmp(p3AddonKey,provChaves[k])" in restore)
check("search stays live, and user changes override auto-focus restore",
      "folhaBusca[0] = 0" in streams and "p3RestorePending = 0; // explicit search" in streams and
      "if (strcmp(folhaBusca, texto))" in streams and "p3RestorePending = 0;" in events)
check("scroll offset restored only for surviving matched provider/release",
      "p3ScrollSaved" in reload and "alturaTotal-areaLista()+40.0f" in restore)
check("No Streams Found with reload control and remote access",
      'i18n("No Streams Found")' in streams and
      "if (!nf && grupo == 1" in events and
      "else if (grupo == -1) recarregar = 1" in events)
check("loading is honest, not fake progress",
      "Refreshing streams… Previous results cannot be played yet." in streams and
      "NV_P3_RELOAD_RETAINED" in policy and "return 1;" in policy)
check("manual explicit selection only, old media stopped first",
      "novaGeracaoFonte(); limparFontePendente();" in manual and
      "player_encerrar_fonte();" in p1_manual and "abrirFonteManualDireta(s)" in p1_manual)
check("manual switch retains same-episode time, zoom, and tracks",
      "player_definir_retomada_fonte(trocaPos)" in p1_manual and
      "player_aspecto_repor_troca(aspectoAnterior)" in p1_manual and
      "video_p1_guardar_faixas_manual();" in p1_manual and "mesmaParte" in p1_manual)
check("explicit change cancels delayed jobs and P2P requests",
      "novaGeracaoFonte(); limparFontePendente();" in change and
      "p2pmotor_cancelar();" in change and "diagnostico_cancelar_vazao();" in change)
check("failed recovery returns to manual selector without autoplay",
      "player_voltar_a_esperar();" in recover and
      "stream_folha_focar_primeira_disponivel();" in recover and
      "stream_folha_recarregar_externo();" in recover)
check("TorBox HTTP source is passed through unchanged",
      "definirFonteVOD(s->url);" in direct and
      "video_definir_cabecalhos(s->cabecalhos);" in direct)
check("P1 same-source renewal verifies source provenance",
      "nv_fonte_mesma_liberacao(&antigo, &novo)" in streams and
      "stream_vod_renovada_aplicar" in streams and "same-source" in resume)
check("saved Resume respects explicit manual selection",
      "fontevolta_toca_guardada(int escolherManual" in resume and
      "retomarDiretoPendente" in app and "tocarFonteGuardada()" in app)
check("next-episode preload and user action gates preserved",
      "player_precarregar_proxima_fonte" in app and
      "player_pediu_proximo(&t,&e)" in app and "abrirProximoEpisodio" in app)
check("P2-D cinematic visible handoff and 200ms fade preserved",
      "a->viva || a->veu > 0.0f" in cinema and
      "agora - a->fim >= 200u" in cinema and
      "!!cinematic_pending" in p2d)
check("P2-C 15s Change Stream and Down/OK ownership preserved",
      "NV_LOADING_CHANGE_MS 15000u" in nav and
      "if (k == SDLK_DOWN) { cinemaEscapeFoco = 1; return; }" in player)
check("post-134 scope remains protected",
      (ROOT / "docs/PLAYER_PENDING_REFINEMENTS_AFTER_134.md").exists() and
      (ROOT / "tests/playback_p3_reload.c").exists())

failures = [n for n, passed in checks if not passed]
for name, passed in checks:
    print(("PASS" if passed else "FAIL") + ": " + name)
print(f"P3 source contracts: {len(checks)-len(failures)}/{len(checks)}")
if failures:
    raise SystemExit(1)
