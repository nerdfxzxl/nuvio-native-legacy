#!/usr/bin/env python3
"""P1 source-only contracts; no Android SDK and no APK required."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app = (root / "src/app.c").read_text()
backend = (root / "src/video_android.c").read_text()
streams = (root / "src/streams.c").read_text()
player = (root / "src/player.c").read_text()
kotlin = (root / "android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt").read_text()

def check(cond, label):
    if not cond:
        raise AssertionError(label)
    print("PASS:", label)

check("nv_fonte_mesma_liberacao(&antigo, &novo)" in streams, "identity rechecked at publication")
check("if (!nv_fonte_mesma_liberacao(&antigo, &novo)) continue;" in streams,
      "identity enforced during same-addon lookup")
check("nativeEvento(tipo: Int, a: Int, b: Int, geracao: Int)" in kotlin and
      "jint tipo, jint a, jint b, jint geracao)" in backend,
      "event JNI generation ABI agrees")
check("geracaoDaAbertura" in kotlin and "int atual = ativo &&" in backend,
      "stale listener events rejected")
check("pedido != pedidos.get()" in kotlin and "p.playWhenReady = false" in kotlin,
      "late pause and decoder release are bounded")
check("static void selecionarAposFalhaVOD(const char *motivo)" in app and
      "stream_folha_focar_primeira_disponivel()" in app and
      "stream_folha_recarregar_externo()" in app and
      "folhaParaTocar = 1" in app,
      "failed recovery selects source UI without auto-accept")
start = app.index("static void tentarProximaFonteVOD(void) {")
end = app.index("// TOCAR UM CANAL", start)
android = app[start:app.index("#else", start)]
check("selecionarAposFalhaVOD" in android and
      "pedirProximaFonteVOD(" not in android,
      "Android VOD failure does not substitute source")
check("void video_p1_guardar_faixas_manual(void)" in backend and
      "player_aspecto_repor_troca(aspectoAnterior)" in app and
      "player_definir_retomada_fonte(trocaPos)" in app,
      "same-episode manual source switch carries tracks, zoom and position")
check("reconPausado = 0;" in backend and
      "if (nv_recon_ativa(&recon)) reconPausado = pausaPedida;" in backend and
      "SDL_APP_DIDENTERBACKGROUND" in player,
      "pause/background override auto recovery")
check("bufavanco_segura(&reconBufferAvanco" in backend and
      "bufavanco_segura(&av" in android,
      "progressive REMUX buffering avoids false stall")
check("novaGeracaoFonte(); limparFontePendente();" in app and
      "if (player_aberto() && player_pediu_fontes())" in app and
      "!player_fontes_pendentes()" in app,
      "Change Stream cancels pending renewal")
print("P1 source contracts: PASS")
