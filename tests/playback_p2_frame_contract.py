#!/usr/bin/env python3
"""P2-B read-only source contracts. NOT a Media3/NDK compilation or TV test."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
K = (ROOT / "android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt").read_text()
POL = (ROOT / "android/app/src/main/java/space/nuvio/nativelegacy/FrameHoldP2B.kt").read_text()
TST = (ROOT / "tests/FrameHoldP2BTest.kt").read_text()
N = (ROOT / "src/video_android.c").read_text()
P = (ROOT / "src/player.c").read_text()
HANDOFF = (ROOT / "docs/PLAYER_PENDING_REFINEMENTS_AFTER_134.md").read_text()
checks = []

def check(description, ok):
    checks.append((description, bool(ok)))
    if not ok:
        raise AssertionError(description)

def body_between(text, beginning, ending):
    assert beginning in text and ending in text, (beginning, ending)
    return text.split(beginning, 1)[1].split(ending, 1)[0]

check("Media3 SurfaceView remains in use", "p.setVideoSurfaceView(sv)" in K)
check("Media3 hardware decoder fallback retained", "setEnableDecoderFallback(true)" in K)
check("Media3 audio decoder/clock retained", "liberarSom()" in K and "somLiberado = false" in K)
check("Same-source recovery optional ABI", 'mAbrirRecuperacao = (*env)->GetStaticMethodID(env, gCls, "abrirRecuperacao", "(Ljava/lang/String;Ljava/lang/String;II)V");' in N)
check("New Kotlin entry point and request generation", "@JvmStatic fun abrirRecuperacao(" in K and "pedidos.incrementAndGet()" in K)
check("Native manual open never requests hold", "return abrirSessao(inicioMs, fracao, 0);" in N)
check("Native reload requests same source", "if (!abrirSessao(alvo, 0, 1))" in N)
check("Native bounded reconnection requests same source", "(int)(recon.alvo * 1000.0), 0, 1))" in N)
check("New native event strictly generation-scoped",
    "EV_SUPERFICIE_INVALIDADA = 15" in N and "(unsigned)geracao == sessao" in N)
check("Kotlin only sends surface invalidation for same session", "ev(EV_SUPERFICIE_INVALIDADA)" in K)
check("Surface invalidation gates native first-frame stability", "case EV_SUPERFICIE_INVALIDADA: superficieEstavel = 0;" in N)
check("Old Android first-frame and stable frame gates retained",
    "case EV_PRIMEIRO_QUADRO:" in N and "case EV_SUPERFICIE_ESTAVEL:" in N)
check("SDR check disallows HDR transfers and Dolby decoder",
    "!hdrSignalled && !dolbyDecoder && !hdrTransfer" in POL and
    "MimeTypes.VIDEO_DOLBY_VISION" in K and "C.COLOR_TRANSFER_ST2084" in K and
    "C.COLOR_TRANSFER_HLG" in K)
check("No source identity mixing across async captures",
    "FrameHoldP2B.ownsResult(pedido, pedidos.get(), origem, sessao" in K and
    "player === p, superficie === sv" in K)
check("Capture has bounded, fail-open timeout",
    "principal.postDelayed(prazo, 160L)" in K and "if (!terminou) { terminou = true; seguir() }" in K)
check("Temporary bitmap cleared on source switch/stop",
    "if (!mesmaFonte && esperaInicio < 0) limparRetido()" in K and
    "if (!preservarRetido) limparRetido()" in K)
check("No frame from old request may seek",
    "busca == buscaSerial" in K and "buscaSerial++" in K)
check("Seek keeps existing player and surface",
    "p.seekTo(alvo)" in body_between(K, "@JvmStatic fun buscar(", "@JvmStatic fun volume(") and
    "setVideoSurface" not in body_between(K, "@JvmStatic fun buscar(", "@JvmStatic fun volume(") and
    ".prepare()" not in body_between(K, "@JvmStatic fun buscar(", "@JvmStatic fun volume("))
check("Out-of-buffer requires measured forward endpoint",
    "targetMs > positionMs && bufferedMs >= positionMs" in POL and
    "targetMs > bufferedMs" in POL)
check("Old video frame remains visible during normal rebuffer",
    "video_bufferando_ms() >= 600" in P and "plrui_anel_solto" in P)
check("New release cannot inherit same-source hold",
    "mesmaFonte: Boolean = false" in K and "preservarRetido = mesmaFonte" in K)
check("SDR hold is above warm-cache thumbnail",
    "mostrarQuadro(url, tituloPedido)\n        // A more recent SAME-source" in K and
    "retidoView?.bringToFront()" in K)
check("Surface geometry matches prior video plane",
    "FrameLayout.LayoutParams(it)" in K and "v.scaleType = ImageView.ScaleType.FIT_XY" in K)
check("Preserves Dolby Vision native surface workaround",
    "decoderDv && !estavelEmitido" in K and "if (semRecriar || decoderDv) return" in K)
check("Existing warm frame not removed",
    "private fun mostrarQuadro(url: String, titulo: String)" in K and "quadroVisivel || retidoVisivel" in K)
check("Existing post-134 refinement handoff untouched in scope",
    "OWNER CONFIRMED FORMAT A" in HANDOFF and "Action pill + exclusive focus" in HANDOFF)
check("No guessed P2P readiness in native event path",
    "contiguous_ready_bytes" not in body_between(N, "JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeEvento", "// --- video.h (C -> Kotlin)"))
check("Pure-Kotlin frame ownership regression tests present",
    "fun main()" in TST and "p.ownsResult(" in TST and "p.mayCopy(" in TST)

# C preprocessor nesting sanity, not equivalent to compilation.
level = 0
for line in N.splitlines():
    s = line.lstrip()
    if s.startswith(("#if ", "#ifdef ", "#ifndef ")):
        level += 1
    elif s.startswith(("#else", "#elif")):
        check("C conditional branch nested", level > 0)
    elif s.startswith("#endif"):
        level -= 1
        check("C preprocessor never underflows", level >= 0)
check("C preprocessor balanced", level == 0)

print(f"P2-B source contracts: {len(checks)}/{len(checks)} PASS (static only)")
