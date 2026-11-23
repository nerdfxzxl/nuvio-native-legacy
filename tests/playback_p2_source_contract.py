#!/usr/bin/env python3
"""P2 static/source integration contracts. No compilation, network or APK.

Run from an authenticated repository checkout with:
    python3 tests/playback_p2_source_contract.py
Also run the actual native timing, P1 and NDK/Media3 tests in P7/P8.
A passing source contract is NOT a passing Android/device test.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


def require(name, *clauses):
    if not all(clauses):
        raise SystemExit(f"P2 SOURCE FAIL: {name}")
    print(f"PASS: {name}")


gfxh = source("src/gfx.h")
gfx = source("src/gfx.c")
player = source("src/player.c")
profiles = source("src/perfilsel.c")
leg = source("src/legenda.c")
legh = source("src/legenda.h")
p2p = source("src/p2pmotor.c")
p2ph = source("src/p2pmotor.h")
p2pabi = source("src/p2pmotor_motor.c")
kotlin = source("android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt")
native = source("src/video_android.c")
cinema = player.split("static void desenharCinema(", 1)[1].split("static void ", 1)[0]

require("official cinematic shader modes",
        "GFX_CINE_ARTE = 54" in gfxh, "GFX_CINE_GRAD = 55" in gfxh,
        "GFX_NMODOS = 56" in gfxh, "[GFX_CINE_GRAD]" in gfx)
require("exact four-stop alpha ramp",
        all(s in gfx for s in ("0.3019608", "0.6", "0.8", "0.9019608",
                               "t<0.35", "t<0.7")))
require("ContentScale.Crop TopEnd mapping",
        "uv.y=uv.y/ra; else uv.x=uv.x*ra+(1.0-ra)" in gfx)
require("cinematic full-height overlay, no old half-screen veil",
        "gfx_rect(tela, 0, GFX_CINE_GRAD" in cinema,
        "GFX_VEU_BAIXO" not in cinema)
require("logo is static inside the transitioning overlay",
        "Q01: artwork/logo must be static" in cinema)
require("event-driven status crossfade and opening reset",
        "aberturaStatus != cine.desde" in cinema, "alterouStatus = agora" in cinema,
        "180.0f" in cinema)
require("real subtitle download/prefetch state",
        "int legenda_baixando(void)" in legh,
        "baixandoGeracao=p->g" in leg,
        "preDownload && !preDownload->terminou" in leg,
        "baixandoGeracao == geracao" in leg,
        'if (legenda_baixando()) return "Fetching subtitles…";' in player)
require("subtitle cancellation and generation isolation",
        "ligada=0;geracao++;baixandoGeracao=0" in leg,
        "if (baixandoGeracao == p->g) baixandoGeracao = 0" in leg)
require("P2P stages from real motor milestone callbacks",
        all(x in p2ph for x in ("P2PM_ETAPA_METADADOS", "P2PM_ETAPA_ARQUIVOS",
                                "P2PM_ETAPA_PREPARANDO", "P2PM_ETAPA_BYTES")),
        "registrarEtapa(p, P2PM_ETAPA_BYTES)" in p2p,
        "r = sondaFn(ev.url" in p2p)
require("P2P generation guard and reset on cancellation/failure",
        "atomic_load(&geracao) == p->g) estado.etapa" in p2p,
        "estado.etapa = P2PM_ETAPA_NENHUMA;" in p2p)
require("engine's exact selected-stream stats ABI is wired, static and dlopen",
        "nuvio_engine_get_stream_stats" in p2pabi,
        "nuvio_engine_stream_stats_init_sized" in p2pabi,
        "PEGA(get_stream_stats)" in p2pabi,
        "st.contiguous_ready_bytes" in p2pabi,
        "st.verified_file_bytes" in p2pabi,
        "medicaoArquivo" in p2pabi)
require("selected-file snapshot rejects stale stream and mismatched episode file",
        "char tidAtual[65], sidAtual[65]" in p2p,
        "p2pm_medicao_corresponde(&medicao, idxArquivo, tamEsperado)" in p2p,
        "s->indice_arquivo == (uint32_t)indice" in p2ph,
        "s->tamanho_arquivo == tamanho" in p2ph,
        "geracaoArquivo == atomic_load(&geracao)" in p2p,
        "!strcmp(sidArquivo, sidAtual)" in p2p,
        "idxArquivo == indiceArquivoAtual" in p2p,
        "limparMedicaoArquivo(); // no stale" in p2p)
require("P2P has real measured bytes, but NO guessed playable readiness percent",
        "get_stream_stats()" in p2ph,
        "NOT a playable-buffer readiness percentage" in p2ph,
        "Verified from start" in player,
        "p.prefixo_verificado" in player,
        "p.medicao_arquivo && n < ABR_MAX" in player,
        "0..100% readiness percentage" in player,
        "Never turn them or total torrent payload bytes into a guessed %" in p2ph)
require("HTTP measured payload from Media3 actual transfer events",
        "onBytesTransferred(source: DataSource" in kotlin,
        "lidos.addAndGet(bytesTransferred.toLong())" in kotlin)
require("HLS/DASH/serial fallback is not double-counted with parallel",
        "source !is ParaleloDataSource" in kotlin,
        "if (e.usado) e.bytes.get() + serial.coerceAtLeast(0) else serial" in kotlin)
require("live playlist revalidation and resource lifecycle accounted",
        "r.contadorHttp.medir(DefaultHttpDataSource.Factory()" in kotlin,
        "val contadorHttp = ContadorHttp()" in kotlin)
require("audio stays silent until first stable rendered video frame",
        "somLiberado = false" in kotlin,
        "player?.volume = if (somLiberado) volumeRequisitado else 0f" in kotlin,
        "ev(EV_SUPERFICIE_ESTAVEL)" in kotlin,
        "liberarSom() // audio-only" in kotlin or
        "liberarSom() // no video plane exists to synchronize with" in kotlin)
require("P1 JNI generation guards survive",
        "(unsigned)geracao == sessao" in native)
require("manual 15-second Change Stream and Back remote behavior",
        "nv_loading_change_ready(agora, cine.desde" in player and
        "NV_LOADING_CHANGE_MS 15000u" in source("src/loading_nav_p2c.h"),
        'i18n("Change Stream")' in player,
        "if (k == SDLK_DOWN) { cinemaEscapeFoco = 1; return; }" in player,
        "saindo = 1; pediuSair = 1;" in player)
require("global shared loading arc and reduced motion",
        "plrui_arco(" in cinema, "ajustes_animacoes_reduzidas()" in player)
require("profile-selection loading uses shared official arc, not twelve dots",
        "plrui_arco(cx, cy, av.w * 0.48f" in profiles,
        "for (k = 0; k < 12; k++)" not in profiles)


print("P2 SOURCE CONTRACTS: PASS (source-only, not native/Android/device verified)")
