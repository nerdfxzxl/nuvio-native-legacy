package space.nuvio.nativelegacy

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.view.PixelCopy
import android.widget.ImageView
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.WrappingMediaSource
import androidx.media3.exoplayer.source.preload.PreloadException
import androidx.media3.exoplayer.source.preload.PreloadMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// Player do Nuvio no Android TV: Media3 ExoPlayer numa SurfaceView ATRAS da
// SDLSurface (o C abre um furo transparente por onde ela aparece, ver
// src/video_android.c). Mesma ABI do host .NET do .tpk (tizen-tpk/Video.cs):
// o C pede por abrir/parar/pausar/buscar/volume/janela/escolher de QUALQUER
// fio e tudo aqui passa ao fio principal; o que acontece volta ao C pelos
// natives, sempre do fio principal.
//
// Sessao monotonica: cada abrir/parar sobe `sessao`, e todo callback confere o
// numero em que nasceu. Um player velho nao fala mais depois de liberado.
//
// Eventos (nativeEvento): 1 PRONTO(durMs) 2 TOCANDO 3 PAUSADO 4 FIM 5 ERRO(cod, C.TRACK_TYPE_* do renderer)
// 6 TAMANHO(w,h) 7 BUFFER(pct) 8 PRIMEIRO_QUADRO, e a extensao 9 = a fonte tem
// audio mas nenhuma faixa tem decoder aqui (o C responde video_audio_nao_suportado).
//
// Tipo 2 do escolher() = atraso de legenda em ms: fica GUARDADO aqui e vale
// adiando a entrega de cada cue ao C (so atraso positivo; adiantar um cue que
// ainda nao chegou nao da). O estilo inteiro e do C.
@androidx.annotation.OptIn(UnstableApi::class)
object NvPlayer {
    private const val TAG = "NvPlayer"
    private const val EV_PRONTO = 1
    private const val EV_TOCANDO = 2
    private const val EV_PAUSADO = 3
    private const val EV_FIM = 4
    private const val EV_ERRO = 5
    private const val EV_TAMANHO = 6
    private const val EV_BUFFER = 7
    private const val EV_PRIMEIRO_QUADRO = 8
    private const val EV_AUDIO_SEM_DECODER = 9
    private const val EV_SUPERFICIE_ESTAVEL = 10
    private const val EV_LIVE_RECOVERY = 11
    private const val EV_LIVE_SEGMENT = 12
    private const val EV_CARGA_FASE = 13 // observed Media3 startup milestones
    private const val EV_SO_AUDIO = 14  // READY with selected audio and no video tracks
    private const val EV_SUPERFICIE_INVALIDADA = 15 // same session; rebuilt hardware plane
    private const val EV_AUDIO_SAIDA_INCOMPATIVEL = 16 // P5-A: route cannot faithfully carry selected audio
    private const val EV_VIDEO_FALHA = 17 // P5-B: user-owned terminal video error
    private const val EV_VIDEO_RECOVERY = 18 // bounded same-source decoder recovery
    private const val EV_AUDIO_ROUTE_RECOVERY = 19 // generation-owned Q49, not a video handoff

    private const val TELA_W = 1920   // coordenadas de layout do app
    private const val TELA_H = 1080
    private const val FAIXAS_MAX = 64 // NV_FAIXA_MAX do C
    private const val TIQUE_MS = 250L
    private const val RETRY_DECODER_MS = 5000L

    private val videoCodecSelector = MediaCodecSelector { mime, secure, tunneling ->
        val codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
        if (MimeTypes.isVideo(mime)) HdrPolicyP5.hardwareFirst(codecs,
            { it.hardwareAccelerated }, { it.softwareOnly }) else codecs
    }

    private val principal = Handler(Looper.getMainLooper())
    private var activity: Activity? = null
    // isLowRamDevice/memoryClass medidos no criarPlayer (true = pouca RAM;
    // false tambem antes de medir). CargaPlayer le sem Context.
    @Volatile internal var poucaRamAparelho = false
    private var camada: FrameLayout? = null
    private var interfaceMobile = false
    private var superficie: SurfaceView? = null
    // #202: a TCL segurou o AudioFlinger por 27-38 s ao soltar o audio do filme
    // anterior; release() no fio principal congelava o app. Cada abertura tem a
    // sua SurfaceView e o player velho e solto num fio proprio.
    private val liberacoesEmCurso = AtomicInteger()
    private var ouvinteAtual: Player.Listener? = null
    private var analiticoAtual: AnalyticsListener? = null

    // Tudo abaixo so no fio principal.
    private var player: ExoPlayer? = null
    private var sessao = 0
    // Invalida um abrir/seek ainda na fila antes de o fio principal executa-lo.
    // A sessao acima segue protegendo os callbacks do ExoPlayer liberado.
    private val pedidos = AtomicInteger()
    private var pedidoAtivo = 0
    private var inicioAtualMs = 0
    // 2.0.3: retomada so com percentual (centesimos de ponto, 0 = nada). Fica
    // pendente ate a timeline trazer a duracao do container; aplicada antes
    // do primeiro quadro, o C nao busca depois do "pronto".
    private var fracaoAtual = 0
    private var fracaoPendente = 0
    private var geracaoNative = 0
    @Volatile private var limitedNext = false
    private var limitedSession = false
    @JvmStatic fun limitedConnections(on: Int) { limitedNext = on != 0 }
    private var urlAtual = ""
    private var cabAtual = ""
    private var abriuEm = 0L
    private var retentou = false
    private var recuperacaoLive: LiveWindowRecovery? = null
    private var pronto = false
    private var duracaoEnviada = -1
    private var videoW = 0
    private var videoH = 0
    private var temJanela = false
    private var jx = 0
    private var jy = 0
    private var jw = TELA_W
    private var jh = TELA_H
    private var atrasoMs = 0
    private var audios = ArrayList<Pair<Tracks.Group, Int>>()
    private var capacidadesAudio = ArrayList<AudioOutputProbeP5A.Observation>()
    private var legendas = ArrayList<Pair<Tracks.Group, Int>>()
    private var assinatura = ""
    private var decoderDv = false
    private var videoDecoderName: String? = null
    private var videoOutputFailed = false
    private var baseLayerRendered: HdrCodecFallbackP5B.VideoConfig? = null
    private var ultHdr = ""
    private var ultDv = -1
    private var ultAtmos = -1
    private var avisouSemAudio = false
    private var quadrosPerdidos = 0L
    private var statsTiques = 0
    private var bufferAlvoMs = androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
    private var bufferMinMs = 0
    private var bufferBytes = 0

    // --- natives (src/video_android.c, Java_space_nuvio_nativelegacy_NvPlayer_*) ---
    @JvmStatic external fun nativeIniciar()
    @JvmStatic external fun nativeLimitedStatus(): Int
    @JvmStatic external fun nativeAfr(): Boolean
    @JvmStatic external fun nativeTunneling(): Boolean
    @JvmStatic external fun nativeEvento(tipo: Int, a: Int, b: Int, geracao: Int)
    // flags (#287): 1 = forcada (SELECTION_FLAG_FORCED), 2 = SDH
    // (ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND); nome = Format.label.
    // mime/canais (#293): codec e canais da faixa de audio.
    @JvmStatic external fun nativeFaixa(tipo: Int, idx: Int, lingua: String, flags: Int, nome: String, mime: String, canais: Int, geracao: Int)
    @JvmStatic external fun nativeAudioCapacidade(idx: Int, hz: Int, bits: Int, bitrate: Int,
                                                 decoder: Int, pcm: Int, pass: Int, fiel: Int, incerto: Int, geracao: Int)
    @JvmStatic external fun nativeAudioRouteCapacidade(geracao: Int, idx: Int, hz: Int, bits: Int, bitrate: Int,
                                                       decoder: Int, pcm: Int, pass: Int, fiel: Int, incerto: Int)
    @JvmStatic external fun nativeFaixasFim(selAudio: Int, selLeg: Int, geracao: Int)
    @JvmStatic external fun nativeLegenda(texto: String, durMs: Int, geracao: Int)
    @JvmStatic external fun nativePos(ms: Long, bufferFimMs: Long, live: Boolean,
                                      sampleBytes: Int, geracao: Int, seekRevision: Int)
    @JvmStatic external fun nativeDecoder4k(hevc: Int, avc: Int, vp9: Int, av1: Int)
    @JvmStatic external fun nativeTela(hdr: Int, dv: Int)
    @JvmStatic external fun nativeHdr(hdr: String, dv: Int, atmos: Int, geracao: Int)
    @JvmStatic external fun nativeErroTexto(texto: String)
    @JvmStatic external fun nativeAudioLog(texto: String)
    @JvmStatic external fun nativeRetomada(geracao: Int, aceita: Int)
    @JvmStatic external fun nativeFitPassiva(rede: Long, geracao: Int, origem: String, kbps: IntArray, fimMs: Long)
    @JvmStatic external fun nativeAudioPcm(pcm: ShortArray, n: Int, ptsUs: Long)
    @JvmStatic external fun nativeAudioEstado(fmt: Int)
    // Estatisticas ao vivo (src/estatvivo.h), 1x/s pelo tique: bytes do socket
    // (-1 = esta abertura nao passou pelo ParaleloDataSource, ex. HLS), buffer
    // a frente e o alvo do DefaultLoadControl em ms, conexoes abertas, 1o byte,
    // quadros perdidos (onDroppedVideoFrames) e entregues; host final.
    @JvmStatic external fun nativeStats(bytes: Long, bufferMs: Int, alvoMs: Int, conexoes: Int,
                                        primeiroByteMs: Int, perdidos: Long, quadros: Long, host: String,
                                        minMs: Int, alvoMb: Int, carregando: Int, geracao: Int)

    // F06 sincronia por audio (AudioSyncTap.kt / AudioSyncSink.kt). Fio de
    // reproducao; o C so copia para um anel limitado (src/audsync.c).
    private var tap = AudioSyncTap(
        { pcm, n, pts -> try { nativeAudioPcm(pcm, n, pts) } catch (t: UnsatisfiedLinkError) { } },
        { fmt -> try { nativeAudioEstado(fmt) } catch (t: UnsatisfiedLinkError) { } }
    )
    private var recursosAtuais: RecursosPlayer? = null

    // F07 (src/video_android.c -> cacheboost.h): seek cache state of this open.
    // The audio output state for the boost is F06's nativeAudioEstado (one
    // detection for both: PCM vs bitstream at the sink input).
    @JvmStatic external fun nativeCache(estado: Int, pedidoMb: Int, limiteMb: Int, usadoMb: Int)

    // --- F07: seek cache and volume boost ---------------------------------------
    // The cache limit is STICKY (the C side sends it only when it changes) and
    // read at each open; any thread writes it, the open reads it on main.
    @Volatile private var cacheMbPedido = 0
    // Warm resume (#492): id of the title of the NEXT open ("" = trailer or
    // channel: it neither keeps nor drops the kept cache) and of the cache
    // being kept when this one closes.
    @Volatile private var tituloPedido = ""
    private var tituloAberto = ""
    private var chaveAberta = ""
    @Volatile private var mornoLigado = true
    private var quadroBitmap: Bitmap? = null
    private var quadroTitulo = ""
    private var quadroUrl = ""
    private var quadroAte = 0L
    private var quadroPedido = 0
    private var quadroView: ImageView? = null
    @Volatile private var quadroVisivel = false
    private var capturando = false
    @JvmStatic fun morno(ligado: Int) {
        mornoLigado = ligado != 0
        if (!mornoLigado) principal.post { esquecerQuadro(); CacheMidia.descartar("warm resume off") }
    }
    private fun esconderQuadro() {
        quadroVisivel = false
        quadroView?.let { (it.parent as? FrameLayout)?.removeView(it) }
        quadroView = null
    }
    private fun esquecerQuadro() {
        quadroPedido++; esconderQuadro()
        quadroBitmap?.recycle(); quadroBitmap = null
        quadroTitulo = ""; quadroUrl = ""
    }
    // PixelCopy reads the hardware Surface, not SDL's transparent framebuffer.
    // Never wait for it on exit. One small bitmap and one request, no decoder held.
    @JvmStatic fun quadro(acao: Int): Int {
        if (acao == 3) return if (quadroVisivel || retidoVisivel) 1 else 0
        principal.post {
            if (acao == 0) { esquecerQuadro(); return@post }
            if (acao == 2) { mostrarQuadro(quadroUrl, quadroTitulo); return@post }
            if (acao != 1 || !mornoLigado || capturando || tituloAberto.isEmpty() || !quadroVisto) return@post
            val sv = superficie ?: return@post
            if (!sv.holder.surface.isValid) return@post
            val titulo = tituloAberto; val url = urlAtual; val token = ++quadroPedido
            val b = try { Bitmap.createBitmap(480, (480 * videoH / videoW.coerceAtLeast(1)).coerceIn(1, 480), Bitmap.Config.ARGB_8888) }
                    catch (_: OutOfMemoryError) { return@post }
            capturando = true
            try {
                PixelCopy.request(sv, b, { resultado ->
                    capturando = false
                    if (resultado == PixelCopy.SUCCESS && token == quadroPedido && mornoLigado) {
                        quadroBitmap?.recycle(); quadroBitmap = b; quadroTitulo = titulo; quadroUrl = url
                        quadroAte = SystemClock.elapsedRealtime() + CacheSessao.LIMITE_MS
                    } else b.recycle()
                }, principal)
            } catch (_: Exception) { capturando = false; b.recycle() }
        }
        return 0
    }
    private fun mostrarQuadro(url: String, titulo: String) {
        esconderQuadro()
        if (titulo.isEmpty()) return // Home trailer leaves the last title snapshot alone.
        if (!mornoLigado || SystemClock.elapsedRealtime() >= quadroAte || titulo != quadroTitulo || url != quadroUrl) { esquecerQuadro(); return }
        val b = quadroBitmap ?: return
        val act = activity ?: return; val c = camada ?: return
        val v = ImageView(act)
        v.setBackgroundColor(android.graphics.Color.BLACK)
        v.scaleType = ImageView.ScaleType.FIT_CENTER
        v.setImageBitmap(b); v.isFocusable = false
        c.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        quadroView = v; quadroVisivel = true
    }

    // P2-B transient hold is NOT the warm-resume cache. It exists solely for
    // the current Media3 session or a native-authorized SAME-source recovery.
    // Never retain an SDR PixelCopy across a manual source/episode change.
    private var retidoBitmap: Bitmap? = null
    private var retidoView: ImageView? = null
    @Volatile private var retidoVisivel = false
    private var retidoPedido = 0
    // A PixelCopy callback cannot apply an older seek after a newer D-pad seek.
    private var appliedSeekRevision = 0
    private var buscaSerial = 0
    private fun limparRetido() {
        retidoPedido++
        retidoVisivel = false
        retidoView?.let { (it.parent as? FrameLayout)?.removeView(it) }
        retidoView = null
        retidoBitmap?.recycle()
        retidoBitmap = null
    }
    private fun mostrarRetido(bitmap: Bitmap, sv: SurfaceView) {
        val c = camada
        if (c == null) { bitmap.recycle(); return }
        limparRetido()
        val v = ImageView(c.context)
        v.isFocusable = false
        v.setBackgroundColor(android.graphics.Color.BLACK)
        v.scaleType = ImageView.ScaleType.FIT_XY
        v.setImageBitmap(bitmap)
        // Copy the actual video-plane geometry (crop/aspect/PiP), not the
        // full screen; otherwise a zoomed or letterboxed frame is stretched.
        val lp = (sv.layoutParams as? FrameLayout.LayoutParams)?.let {
            FrameLayout.LayoutParams(it)
        } ?: FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                      FrameLayout.LayoutParams.MATCH_PARENT)
        c.addView(v, lp)
        v.bringToFront()
        retidoBitmap = bitmap
        retidoView = v
        retidoVisivel = true
    }
    private fun podeCapturarRetido(p: ExoPlayer, sv: SurfaceView): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 26 || retidoVisivel) return false
        val f = p.videoFormat
        val transfer = f?.colorInfo?.colorTransfer
        val hdrTransfer = transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG
        return FrameHoldP2B.mayCopy(
            quadroVisto, sv.holder.surface.isValid, player === p && superficie === sv,
            ultHdr.isNotEmpty() && !ultHdr.equals("none", ignoreCase = true),
            decoderDv || f?.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION,
            hdrTransfer
        )
    }
    // PixelCopy is asynchronous: bounded capture, NO main-thread wait for
    // GPU/decoder, and fail-open if unavailable (secure video, memory, HAL).
    // Only the original request/session/surface may publish the bitmap.
    private fun capturarRetido(p: ExoPlayer, sv: SurfaceView, pedido: Int, seguir: () -> Unit) {
        if (!podeCapturarRetido(p, sv)) { seguir(); return }
        val origem = sessao
        val token = ++retidoPedido
        val width = sv.width.coerceAtLeast(1)
        val height = sv.height.coerceAtLeast(1)
        // At most 1920x1080 RGBA (about 8 MiB), not a full 4K copy on low-RAM TVs.
        val scale = minOf(1.0, kotlin.math.sqrt(2073600.0 / (width.toDouble() * height)))
        val bitmap = try {
            Bitmap.createBitmap((width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) { seguir(); return }
        var terminou = false
        val prazo = Runnable {
            if (!terminou) { terminou = true; seguir() }
        }
        principal.postDelayed(prazo, 160L)
        try {
            PixelCopy.request(sv, bitmap, { resultado ->
                if (terminou) { bitmap.recycle(); return@request }
                terminou = true
                principal.removeCallbacks(prazo)
                if (resultado == PixelCopy.SUCCESS && !videoOutputFailed && token == retidoPedido &&
                    FrameHoldP2B.ownsResult(pedido, pedidos.get(), origem, sessao,
                        player === p, superficie === sv)) {
                    mostrarRetido(bitmap, sv)
                } else bitmap.recycle()
                seguir()
            }, principal)
        } catch (_: Exception) {
            principal.removeCallbacks(prazo)
            if (!terminou) { terminou = true; bitmap.recycle(); seguir() }
            else bitmap.recycle()
        }
    }
    private var cacheMidia: CacheMidia? = null
    // Snapshot native provenance before queuing the open.
    @Volatile private var cacheIdentidadeArmada: CacheProvaP4C.Identidade? = null
    private var cacheEstado = CacheSessao.DESLIGADO
    private var cacheTiques = 0
    // Volume of this open (0..200) and the processor of its audio sink.
    private var ganhoPct = 100
    @Volatile private var amplificacaoDb = 0
    @Volatile private var baixoDb = 0
    @Volatile private var centroDb = 0
    @Volatile private var noiteNivel = 0
    // With bitstream (passthrough) the processor is bypassed by the sink, so
    // its gain needs no passthrough state here; C caps the row at 100%.
    private var processador: GanhoAudioProcessor? = null
    private const val CACHE_RELATO_TIQUES = 8      // 8 x 250 ms: usage every 2 s

    private fun relatarCache(minha: Int) {
        if (!atual(minha)) return
        val c = cacheMidia
        val ativo = c != null && cacheEstado == CacheSessao.ATIVO
        try {
            nativeCache(cacheEstado, cacheMbPedido, if (ativo) c!!.limiteMb else 0, if (ativo) c!!.usadoMb() else 0)
        } catch (e: UnsatisfiedLinkError) { Log.w(TAG, "cache sem lib: $e") }
    }

    // P2 first-frame handoff: keep Media3's video/audio decoding and clock
    // running together, but hold only audible output until the first video
    // frame AND the surface are stable. Muting via player volume does not
    // transcode, downmix, reselect tracks or suppress network buffering.
    private var somLiberado = false
    private var volumeRequisitado = 1f
    private fun aplicarVolume() {
        player?.volume = if (somLiberado && !videoOutputFailed && afrGate == null &&
            recursosAtuais?.routeMuted != true && recursosAtuais?.routeFailed != true) volumeRequisitado else 0f
    }
    private fun liberarSom() {
        if (!videoOutputFailed && !somLiberado) { somLiberado = true; aplicarVolume() }
    }
    private fun aplicarGanho() {
        volumeRequisitado = GanhoMath.volumePlayer(ganhoPct)
        aplicarVolume()
        processador?.ganho = GanhoMath.reforco(ganhoPct)
        processador?.amplificacaoDb = amplificacaoDb
        processador?.baixoDb = baixoDb
        processador?.centroDb = centroDb
        processador?.noiteNivel = noiteNivel
    }

    // StreamFit passivo (PassivoMedidor.kt). Entrega de qualquer fio (os
    // pedacos baixam em fios proprios); o C so trava um mutex curto.
    private val medidor = PassivoMedidor(
        { SystemClock.elapsedRealtime() }, { System.currentTimeMillis() }, { PassivoMedidor.redeGlobal }
    ) { e ->
        try { nativeFitPassiva(e.rede, e.geracao, e.origem, e.kbps, e.fimMs) }
        catch (t: UnsatisfiedLinkError) { Log.w(TAG, "passivo sem lib: $t") }
    }
    private var medidorToken = 0L
    private fun medirEstado(p: ExoPlayer) {
        val st = p.playbackState
        medidor.estado(medidorToken,
            p.playWhenReady && (st == Player.STATE_READY || st == Player.STATE_BUFFERING), pronto)
    }

    private fun atual(minha: Int) = minha == sessao && pedidoAtivo == pedidos.get()

    // One opt-in request per visible session; never let a preloaded player vote.
    private var afrLease: FrameRateP5C.PreferenceLease<android.view.Window>? = null
    private var afrEvaluated = false
    private var afrSuspended = false
    private var afrGate: FrameRateP5C.SwitchGate? = null
    private var afrPoll: Runnable? = null

    private fun clearAfrGate() {
        afrPoll?.let { principal.removeCallbacks(it) }
        afrPoll = null
        afrGate = null
    }

    private fun restoreAfr() {
        val w = activity?.window
        val lease = afrLease
        clearAfrGate()
        afrLease = null
        afrEvaluated = false
        if (w != null && lease != null) try {
            val a = w.attributes
            val prev = lease.restoreTarget(w, a.preferredDisplayModeId)
            if (prev != null) {
                a.preferredDisplayModeId = prev
                w.attributes = a
            }
        } catch (e: Exception) { Log.w(TAG, "AFR restore: $e") }
    }

    private fun observeAfr(minha: Int, p: ExoPlayer, act: Activity, target: Int,
                           gate: FrameRateP5C.SwitchGate) {
        val poll = object : Runnable {
            override fun run() {
                if (afrGate !== gate || !atual(minha) || player !== p) return
                val stillOwned = try {
                    activity === act && act.window.attributes.preferredDisplayModeId == target && nativeAfr()
                } catch (_: Exception) { false }
                val active = if (stillOwned) try {
                    act.windowManager.defaultDisplay.mode.modeId
                } catch (_: Exception) { null } else null
                val status = if (stillOwned) gate.observe(active, SystemClock.elapsedRealtime())
                    else FrameRateP5C.SwitchStatus.TIMED_OUT
                if (status == FrameRateP5C.SwitchStatus.WAITING) {
                    principal.postDelayed(this, 100L)
                    return
                }
                if (status == FrameRateP5C.SwitchStatus.TIMED_OUT) {
                    Log.w(TAG, "AFR mode not confirmed; releasing only our display preference")
                    restoreAfr()
                    afrEvaluated = true // no repeated mode requests in this session
                } else {
                    clearAfrGate()
                    Log.i(TAG, "AFR display mode confirmed: $target")
                }
                aplicarVolume()
                hdrNaSuperficie()
                avaliarEstavel()
            }
        }
        afrPoll = poll
        principal.post(poll)
    }

    private fun applyAfr(minha: Int, format: Format?, resumed: Boolean = false) {
        if (!atual(minha) || videoOutputFailed || afrEvaluated || afrSuspended || recriando ||
            (!resumed && estavelEmitido) || !nativeAfr()) return
        val p = player ?: return
        val f = format ?: return
        if (!f.frameRate.isFinite() || f.frameRate <= 0f) return
        val act = activity ?: return
        val required = when {
            f.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION -> android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION
            f.colorInfo?.colorTransfer == C.COLOR_TRANSFER_ST2084 -> android.view.Display.HdrCapabilities.HDR_TYPE_HDR10
            f.colorInfo?.colorTransfer == C.COLOR_TRANSFER_HLG -> android.view.Display.HdrCapabilities.HDR_TYPE_HLG
            else -> null
        }
        try {
            val display = act.windowManager.defaultDisplay
            val known = android.os.Build.VERSION.SDK_INT >= 34
            fun mode(m: android.view.Display.Mode) = FrameRateP5C.Mode(m.modeId, m.physicalWidth,
                m.physicalHeight, m.refreshRate, if (known) m.supportedHdrTypes.toSet() else emptySet())
            val current = mode(display.mode)
            val knownSdr = f.colorInfo?.colorTransfer == C.COLOR_TRANSFER_SDR
            // PQ alone does not distinguish HDR10 from dynamic HDR10+ metadata.
            // Preserve HDR10+ if the current mode advertises it; for unknown
            // color transfer, retain all current advertised HDR capabilities.
            // On older APIs do not switch an unclassified video mode.
            if (!known && required == null && !knownSdr) return
            // Do not change a non-DV output into a DV mode merely because the
            // source advertises DV. The alternate HDR10 path has its own proof.
            if (required == android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION &&
                required !in current.hdrTypes) return
            val retainHdr = when {
                required == android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 ->
                    current.hdrTypes.intersect(setOf(android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS))
                required == null && !knownSdr -> current.hdrTypes
                else -> emptySet()
            }
            val target = FrameRateP5C.choose(true, f.frameRate, current,
                display.supportedModes.map(::mode), required, known, retainHdr)
            afrEvaluated = true
            if (target != null) {
                val w = act.window
                val attrs = w.attributes
                val previous = attrs.preferredDisplayModeId
                if (previous != target) {
                    val lease = FrameRateP5C.PreferenceLease(w, previous, target)
                    val gate = FrameRateP5C.SwitchGate(target, SystemClock.elapsedRealtime())
                    afrGate = gate
                    principal.removeCallbacks(estavelRun)
                    if (recriaAgendada && !estavelEmitido) hdrRecriadoPara = ""
                    principal.removeCallbacks(recriar)
                    recriaAgendada = false
                    aplicarVolume()
                    // Install the lease before submitting, so a partial
                    // failure cannot strand our requested window preference.
                    afrLease = lease
                    attrs.preferredDisplayModeId = target
                    w.attributes = attrs
                    observeAfr(minha, p, act, target, gate)
                    Log.i(TAG, "AFR requested supported mode=$target fps=${f.frameRate}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AFR mode query/request: $e")
            restoreAfr()
            afrEvaluated = true
            aplicarVolume()
            avaliarEstavel()
        }
    }

    // All video-terminal paths share the same one-shot latch. A READY,
    // first-frame or delayed renderer callback cannot unmute after this.
    private fun falhaVideo(minha: Int, motivo: Int, codigo: Int, geracao: Int) {
        if (!atual(minha) || videoOutputFailed) return
        videoOutputFailed = true
        recursosAtuais?.let { cancelAudioRoute(it, restore = false) }
        recuperacaoLive?.cancel()
        principal.removeCallbacks(recriar)
        recriaAgendada = false
        cancelarDetach()
        cancelarStartupSeek()
        invalidarQuadroSuperficie()
        restoreAfr()
        try { player?.pause() } catch (e: Exception) { Log.w(TAG, "pause after video failure: $e") }
        ev(EV_VIDEO_FALHA, motivo, codigo, geracao)
    }

    private fun confirmarRetomada(geracao: Int, aceita: Boolean) {
        if (geracao != 0) try { nativeRetomada(geracao, if (aceita) 1 else 0) }
        catch (e: UnsatisfiedLinkError) { Log.w(TAG, "retomada sem lib: $e") }
    }

    // #345: uploaded app logs do not include Kotlin's logcat diagnostics.
    private fun relatarAudio(minha: Int, texto: String) {
        if (!atual(minha)) return
        val linha = "sessao=$minha $texto"
        Log.i(TAG, "[audio] $linha")
        try { nativeAudioLog(linha) } catch (_: UnsatisfiedLinkError) { }
    }

    // Exception messages may include stream Format.id/label or URLs (#345).
    private fun camposErroAudio(erro: Throwable): String {
        val formato = when (erro) {
            is AudioSink.InitializationException -> erro.format
            is AudioSink.WriteException -> erro.format
            is AudioSink.ConfigurationException -> erro.format
            else -> null
        }
        val codigo = when (erro) {
            is AudioSink.WriteException -> erro.errorCode
            is MediaCodecDecoderException -> erro.errorCode
            is android.media.MediaCodec.CodecException -> erro.errorCode
            else -> null
        }
        val recuperavel = when (erro) {
            is AudioSink.InitializationException -> erro.isRecoverable
            is AudioSink.WriteException -> erro.isRecoverable
            is android.media.MediaCodec.CodecException -> erro.isRecoverable
            else -> null
        }
        return "erro=${erro.javaClass.simpleName}" +
            (if (erro is AudioSink.UnexpectedDiscontinuityException)
                " expectedUs=${erro.expectedPresentationTimeUs} actualUs=${erro.actualPresentationTimeUs}" else "") +
            (codigo?.let { " errorCode=$it" } ?: "") +
            (formato?.let { " hz=${it.sampleRate} canais=${it.channelCount} encoding=${it.pcmEncoding} mime=${it.sampleMimeType}" } ?: "") +
            (recuperavel?.let { " isRecoverable=$it" } ?: "")
    }

    // Fatal cause chains retain only class names and allowlisted numeric fields.
    private fun camposErroCausa(erro: Throwable): String {
        return erro.javaClass.simpleName + when (erro) {
            is ExoPlaybackException -> " errorCode=${erro.errorCode} type=${erro.type} rendererIndex=${erro.rendererIndex}"
            is PlaybackException -> " errorCode=${erro.errorCode}"
            is HttpDataSource.InvalidResponseCodeException -> " responseCode=${erro.responseCode} errorCode=${erro.reason}"
            is DataSourceException -> " errorCode=${erro.reason}"
            is AudioSink.WriteException -> " errorCode=${erro.errorCode}"
            is MediaCodecDecoderException -> " errorCode=${erro.errorCode}"
            is android.media.MediaCodec.CodecException -> " errorCode=${erro.errorCode}"
            else -> ""
        }
    }

    private fun ev(tipo: Int, a: Int = 0, b: Int = 0, geracao: Int = geracaoNative) {
        try { nativeEvento(tipo, a, b, geracao) }
        catch (e: UnsatisfiedLinkError) { Log.w(TAG, "evento $tipo sem lib: $e") }
    }

    // One silent, surface-free player. Its request counter is independent of
    // the visible session: even a queued prepare is invalidated by Back/stop.
    private data class ConsultaPreparo(val token: Int, val estado: Int)
    private val travaPreparo = Any()
    private val pedidosPreparo = AtomicInteger()
    @Volatile private var consultaPreparo = ConsultaPreparo(0, 0)
    @Volatile private var preparo: Preparo? = null
    // Main-looper ownership of a selected preload detached from the speculative slot.
    private val pendingAdoptionP6 = PendingAdoptionP6<Preparo> { s ->
        s.recursos?.let { soltarRecursosPreparo(it) }
    }
    private const val PREPARO_MAX_US = 5_000_000L
    private const val PREPARO_ESPERA_MS = 30_000L

    private class CargaPlayer(val minMs: Int, maxMs: Int, val bytes: Int, preparando: Boolean, val profile: RamBufferProfile) :
        DefaultLoadControl(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
            minMs, maxMs, profile.startMs, profile.rebufferMs, bytes, profile.preferTime,
            profile.backMs, profile.keepKeyframe) {
        @Volatile var adotada = !preparando
        @Volatile var cancelada = false
        // At most 1/16 of the heap (1/32 on low-RAM, where the active player
        // and the parallel downloader already press the limit), with the same
        // heap ceiling as normal play.
        private val tetoPreparo = (Runtime.getRuntime().maxMemory() / (if (poucaRamAparelho) 32 else 16))
            .coerceIn(8L shl 20, (if (poucaRamAparelho) 8L else 16L) shl 20).coerceAtMost(bytes.toLong()).toInt()
        fun esperarCapacidade() {
            // HLS/DASH can load a whole large segment between LoadControl
            // checks. Pause reads at the same allocator ceiling as progressive
            // media, retaining the paused player instead of letting that chunk
            // overrun the preparation budget. Loader cancellation interrupts it.
            while (!adotada && !cancelada && allocator.totalBytesAllocated >= tetoPreparo) {
                try { Thread.sleep(20) }
                catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException("preparation canceled")
                }
            }
            if (cancelada) throw InterruptedIOException("preparation canceled")
        }
        fun cheio() = allocator.totalBytesAllocated >= tetoPreparo
        override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean =
            if (adotada) allocator.totalBytesAllocated < bytes && super.shouldContinueLoading(parameters)
            else parameters.bufferedDurationUs < PREPARO_MAX_US && allocator.totalBytesAllocated < tetoPreparo
        override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean =
            if (adotada) super.shouldStartPlayback(parameters) ||
                (parameters.bufferedDurationUs > 0 && allocator.totalBytesAllocated >= bytes)
            else parameters.bufferedDurationUs >= 2_500_000L || allocator.totalBytesAllocated >= tetoPreparo
    }

    // Adoption keeps the samples already buffered. At its next progressive
    // read, the loader continues at the exact unread byte through the ordinary
    // parallel/cache chain; it never downloads the initial range a second time.
    // Starting with ParaleloDataSource would allocate its 4 MB prefetch chunks
    // outside LoadControl's allocator, defeating the preparation memory cap.
    // P2: count actual network payload transfer events, including HLS/DASH segment
    // reads and serial HTTP. Do NOT count Media3 cache reads or parallel data
    // delivery: ParaleloDataSource.Estat already owns real socket payloads.
    // The counter belongs to RecursosPlayer, so a stale released downloader cannot
    // contaminate the generation selected for nativeStats.
    private class ContadorHttp {
        private val lidos = AtomicLong()
        private val redeAberta = AtomicBoolean(false)
        val listener = object : TransferListener {
            override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
            override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                if (isNetwork && source !is ParaleloDataSource) redeAberta.set(true)
            }
            override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec,
                                            isNetwork: Boolean, bytesTransferred: Int) {
                if (isNetwork && source !is ParaleloDataSource && bytesTransferred > 0) {
                    redeAberta.set(true)
                    lidos.addAndGet(bytesTransferred.toLong())
                }
            }
            override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {}
        }
        fun bytes(): Long = if (redeAberta.get()) lidos.get() else -1L
        fun medir(f: DataSource.Factory): DataSource.Factory = object : DataSource.Factory {
            override fun createDataSource(): DataSource = f.createDataSource().also { it.addTransferListener(listener) }
        }
    }

    private class OrigemPlayer(@Volatile var fabrica: DataSource.Factory,
                               private val carga: CargaPlayer, private val contador: ContadorHttp) : DataSource.Factory {
        override fun createDataSource(): DataSource = object : DataSource {
            private var atual: DataSource? = null
            private var fabricaAberta: DataSource.Factory? = null
            private var spec: DataSpec? = null
            private var lidos = 0L
            private val ouvintes = ArrayList<TransferListener>()
            override fun open(dataSpec: DataSpec): Long {
                spec = dataSpec; lidos = 0L
                return abrirFonte(dataSpec, fabrica)
            }
            private fun abrirFonte(dataSpec: DataSpec, fonte: DataSource.Factory): Long {
                val d = fonte.createDataSource()
                d.addTransferListener(contador.listener)
                for (o in ouvintes) d.addTransferListener(o)
                atual = d
                fabricaAberta = fonte
                return d.open(dataSpec)
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                carga.esperarCapacidade()
                val s = spec
                val fonte = fabrica
                if (s != null && fonte !== fabricaAberta && ParaleloDataSource.serve(s.uri) &&
                    (s.length == C.LENGTH_UNSET.toLong() || lidos < s.length)) {
                    // AIO/debrid resolution already happened during preparation.
                    // Continue at the redirected media URL, with the same headers.
                    val finalUri = atual?.uri ?: s.uri
                    val restante = s.buildUpon().setUri(finalUri)
                        .setKey(s.key ?: s.uri.toString()).build().subrange(lidos)
                    atual?.close()
                    abrirFonte(restante, fonte)
                }
                val n = atual?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
                if (n > 0) lidos += n
                return n
            }
            override fun getUri(): Uri? = atual?.uri
            override fun getResponseHeaders(): Map<String, List<String>> = atual?.responseHeaders ?: emptyMap()
            override fun close() {
                try { atual?.close() } finally { atual = null; spec = null; fabricaAberta = null }
            }
            override fun addTransferListener(transferListener: TransferListener) {
                ouvintes.add(transferListener)
                atual?.addTransferListener(transferListener)
            }
        }
    }

    private class SeletorAudioPreparo(private val context: Context, private val preferencia: String) :
        DefaultTrackSelector(context) {

        private data class FaixaAudio(
            val renderer: Int,
            val grupo: androidx.media3.common.TrackGroup,
            val indice: Int,
            val idioma: String,
            val selecionada: Boolean,
            val qualidade: Long,
            val fiel: Boolean
        )

        override fun selectAllTracks(
            mappedTrackInfo: androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo,
            rendererFormatSupports: Array<Array<IntArray>>,
            rendererMixedMimeTypeAdaptationSupports: IntArray,
            params: DefaultTrackSelector.Parameters
        ): Array<androidx.media3.exoplayer.trackselection.ExoTrackSelection.Definition?> {
            val definicoes = super.selectAllTracks(mappedTrackInfo, rendererFormatSupports,
                rendererMixedMimeTypeAdaptationSupports, params)
            // Explicit Q49/user override and temporary audio disable own selection.
            if (preferencia.isEmpty() || params.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO) ||
                params.overrides.keys.any { it.type == C.TRACK_TYPE_AUDIO }) return definicoes

            // TrackSelectionUtil.buildTracks and publicarFaixas use this exact
            // renderer/group/track order. C receives only FORMAT_HANDLED audio,
            // capped at FAIXAS_MAX, not every format the selector can try playing.
            val faixas = ArrayList<FaixaAudio>(FAIXAS_MAX)
            publicar@ for (r in 0 until mappedTrackInfo.rendererCount) {
                if (mappedTrackInfo.getRendererType(r) != C.TRACK_TYPE_AUDIO) continue
                val grupos = mappedTrackInfo.getTrackGroups(r)
                for (g in 0 until grupos.length) {
                    val grupo = grupos[g]
                    if (grupo.type != C.TRACK_TYPE_AUDIO) continue
                    for (i in 0 until grupo.length) {
                        if (mappedTrackInfo.getTrackSupport(r, g, i) != C.FORMAT_HANDLED) continue
                        val selecao = definicoes[r]
                        val f = grupo.getFormat(i)
                        val cap = AudioOutputProbeP5A.inspect(context, f, decoder = true, dsp = false).result
                        faixas.add(FaixaAudio(r, grupo, i, codigoPublicado(f.language),
                            selecao != null && selecao.group == grupo && i in selecao.tracks,
                            cap.sourceQualityScore, cap.outputFaithfulCandidate))
                        if (faixas.size == FAIXAS_MAX) break@publicar
                    }
                }
            }
            if (faixas.size < 2) return definicoes

            // Tracks marks every member of an adaptive definition selected. C
            // keeps the FIRST such published member, or audioAtual=0 if none was
            // published as selected. Preserve the full definition in either case.
            val atual = faixas.firstOrNull { it.selecionada } ?: faixas[0]
            // Highest route-compatible original-language source, not simply
            // the first renderer-decodable codec. Unknown is NOT 'lossless'.
            val alvo = faixas.filter { it.fiel && it.idioma.isNotEmpty() &&
                casa(it.idioma, preferencia) }.maxByOrNull { it.qualidade } ?: return definicoes
            if (atual.renderer == alvo.renderer && atual.grupo == alvo.grupo &&
                atual.indice == alvo.indice) return definicoes

            for (r in definicoes.indices) {
                if (mappedTrackInfo.getRendererType(r) == C.TRACK_TYPE_AUDIO) definicoes[r] = null
            }
            definicoes[alvo.renderer] = androidx.media3.exoplayer.trackselection.ExoTrackSelection.Definition(
                alvo.grupo, alvo.indice)
            return definicoes
        }

        // The native path first copies Format.language through deJString's 16-byte
        // buffer, rejects ONLY these exact lower-case sentinels, then snprintf
        // copies at most seven UTF-8 bytes into VideoFaixa.idioma[8]. Latin-1 here
        // is a lossless byte key: truncation may end inside a UTF-8 character.
        private fun codigoPublicado(raw: String?): String {
            if (raw == null) return ""
            val bytes = ByteArray(15)
            var usados = 0
            var i = 0
            while (i < raw.length && usados + 5 < 16) {
                var c = raw[i++].code
                if (c in 0xD800 until 0xDC00 && i < raw.length && raw[i].code in 0xDC00 until 0xE000) {
                    c = 0x10000 + ((c - 0xD800) shl 10) + (raw[i++].code - 0xDC00)
                }
                // Later strcmp/snprintf stop at the first NUL in the native buffer.
                if (c == 0) break
                when {
                    c < 0x80 -> bytes[usados++] = c.toByte()
                    c < 0x800 -> {
                        bytes[usados++] = (0xC0 or (c shr 6)).toByte()
                        bytes[usados++] = (0x80 or (c and 0x3F)).toByte()
                    }
                    c < 0x10000 -> {
                        bytes[usados++] = (0xE0 or (c shr 12)).toByte()
                        bytes[usados++] = (0x80 or ((c shr 6) and 0x3F)).toByte()
                        bytes[usados++] = (0x80 or (c and 0x3F)).toByte()
                    }
                    else -> {
                        bytes[usados++] = (0xF0 or (c shr 18)).toByte()
                        bytes[usados++] = (0x80 or ((c shr 12) and 0x3F)).toByte()
                        bytes[usados++] = (0x80 or ((c shr 6) and 0x3F)).toByte()
                        bytes[usados++] = (0x80 or (c and 0x3F)).toByte()
                    }
                }
            }
            val recebido = String(bytes, 0, usados, Charsets.ISO_8859_1)
            if (recebido == "und" || recebido == "unknown" || recebido == "zxx") return ""
            return recebido.take(7)
        }

        // Match the native byte-wise ASCII language codes without locale-dependent
        // Unicode folding, trimming, ranking or Media3 preferred-language scoring.
        private fun minusculasAscii(codigo: String): String = buildString(codigo.length) {
            for (c in codigo) append(if (c in 'A'..'Z') (c.code + 32).toChar() else c)
        }

        private fun familia(codigo: String): String {
            val baixo = minusculasAscii(codigo)
            familias[baixo]?.let { return it }
            return baixo.takeWhile { it != '-' && it != '_' }.take(7)
        }

        // codigo is already a native published byte key. The request preference
        // comes from C's ling_audio(); encode it as the same UTF-8 byte key.
        private fun casa(codigo: String, pref: String): Boolean {
            if (pref.isEmpty()) return true
            if (codigo.isEmpty()) return false
            val p = String(pref.toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1).substringBefore('\u0000')
            if (p.isEmpty()) return true
            if (minusculasAscii(codigo) == minusculasAscii(p)) return true
            val a = familia(codigo)
            val b = familia(p)
            if (a == b) return true
            if (a == "sh" || b == "sh") {
                val outro = if (a == "sh") b else a
                return outro == "hr" || outro == "sr" || outro == "bs"
            }
            return false
        }

        companion object {
            // Exact immutable familia() aliases from src/linguas.c. No selector,
            // player, selection parameters or mutable lifecycle state is shared.
            private val familias = mapOf(
                "pob" to "pt", "por" to "pt", "ptb" to "pt", "pt-br" to "pt", "pt_br" to "pt", "br" to "pt",
                "eng" to "en", "en-us" to "en", "en_us" to "en", "en-gb" to "en", "en_gb" to "en",
                "spa" to "es", "esp" to "es", "fre" to "fr", "fra" to "fr", "ger" to "de", "deu" to "de",
                "ita" to "it", "jpn" to "ja", "kor" to "ko", "chi" to "zh", "zho" to "zh",
                "rus" to "ru", "ara" to "ar", "hin" to "hi", "dut" to "nl", "nld" to "nl",
                "swe" to "sv", "nor" to "no", "dan" to "da", "fin" to "fi", "pol" to "pl",
                "tur" to "tr", "heb" to "he", "tha" to "th", "cze" to "cs", "gre" to "el",
                "hun" to "hu", "rum" to "ro", "ron" to "ro", "ukr" to "uk", "vie" to "vi", "ind" to "id",
                "ces" to "cs", "ell" to "el", "slk" to "sk", "slo" to "sk", "bul" to "bg",
                "msa" to "ms", "may" to "ms", "srp" to "sr", "scc" to "sr", "hrv" to "hr",
                "scr" to "hr", "slv" to "sl", "est" to "et", "lav" to "lv", "lit" to "lt",
                "cat" to "ca", "eus" to "eu", "baq" to "eu", "glg" to "gl", "isl" to "is",
                "ice" to "is", "fas" to "fa", "per" to "fa", "fil" to "tl", "tgl" to "tl",
                "nob" to "no", "nno" to "no", "nb" to "no", "nn" to "no", "iw" to "he",
                "chs" to "zh", "cht" to "zh", "zht" to "zh", "zhe" to "zh", "ze" to "zh",
                "pb" to "pt", "ptbr" to "pt", "spn" to "es", "ea" to "es",
                "jp" to "ja", "cz" to "cs", "du" to "nl", "gr" to "el", "in" to "id", "ma" to "ms",
                "bos" to "bs", "hbs" to "sh"
            )
        }
    }

    private class RecursosPlayer(val carga: CargaPlayer, val alvoMs: Int, val idiomaAudio: String = "") {
        lateinit var p: ExoPlayer
        lateinit var origem: OrigemPlayer
        val contadorHttp = ContadorHttp()
        val parallelStats = ParaleloDataSource.Estat()
        lateinit var rede: (Long) -> DataSource.Factory
        lateinit var renderizadores: androidx.media3.exoplayer.DefaultRenderersFactory
        lateinit var fabricaMidia: DefaultMediaSourceFactory
        var fontePrecarregada: PreloadMediaSource? = null
        var fioPreparo: HandlerThread? = null
        var seletorPreparo: DefaultTrackSelector? = null
        var capacidadesPreparo: androidx.media3.exoplayer.RendererCapabilitiesList? = null
        var bandaPreparo: DefaultBandwidthMeter? = null
        var liberado = false
        @Volatile var adotado = carga.adotada
        @Volatile var videoConfig: HdrCodecFallbackP5B.VideoConfig? = null
        @Volatile var runtimeVideoRecovery: HdrCodecFallbackP5B.RuntimeRecovery? = null
        @Volatile var audioFormato = AudioSyncTap.FMT_NENHUM
        @Volatile var velocidadePcm = false
        @Volatile var ajustePcm = false // DSP requires decoded PCM, never lossy AC-3 transcoding
        @Volatile var routePcm = false
        lateinit var routeObserver: AudioRouteObserverP5D
        val routeDebounce = AudioRouteP5D.Debounce()
        val routeTx = AudioRouteP5D.Transaction()
        var routeMuted = true // Q36 validation must precede audible handoff
        var routeFailed = false
        var routeNativeOwned = false
        var routeChanged = false
        var routeCheck = true
        var routeFatal = false
        var routePhase = 0 // 1 = disable acknowledged asynchronously; 2 = wait for new output
        var routeDisabled = false
        var routeOutputBefore = 0L
        var routeSelected: Pair<Tracks.Group, Int>? = null
        var routeTarget: Pair<Tracks.Group, Int>? = null
        var routeAwaitSince = 0L
        var routeHasVideo = false
        var routeInput: Format? = null
        var routeValidatedSerial = 0L
        var routeAwaitingPlay = false
        val proc = GanhoAudioProcessor()
        val tap = AudioSyncTap(
            { pcm, n, pts -> if (adotado) try { nativeAudioPcm(pcm, n, pts) } catch (_: UnsatisfiedLinkError) {} },
            { fmt ->
                audioFormato = fmt
                if (adotado) try { nativeAudioEstado(fmt) } catch (_: UnsatisfiedLinkError) {}
            }
        )
        var cache: CacheMidia? = null
        var cacheEstado = CacheSessao.DESLIGADO
        fun construido() = ::p.isInitialized
        fun promover(token: Long) {
            origem.fabrica = rede(token)
            carga.adotada = true
            adotado = true
        }
    }

    private class Preparo(val token: Int, val pedido: Int, val url: String, val cab: String,
                          val inicioMs: Int, val fracao: Int, val buffer: Int, val cacheMb: Int, val proximo: Boolean = false,
                          val idiomaAudio: String = "", val kbps: Int = 0, val profile: RamBufferProfile) {
        var recursos: RecursosPlayer? = null
        var ouvinte: Player.Listener? = null
        var analitico: AnalyticsListener? = null
        @Volatile var fracaoPendente = if (inicioMs <= 0) fracao else 0
        @Volatile var inicioAceito = false
        @Volatile var posicaoFonteMs = inicioMs.toLong()
        @Volatile var duracaoFonteMs = C.TIME_UNSET
        @Volatile var fonteBusca = false
        @Volatile var faixasFonteProntas = false
        var decoderDv = false
        var videoDecoderName: String? = null
    }

    private fun preparoAtual(s: Preparo) = preparo === s && s.pedido == pedidosPreparo.get()
    private fun publicarPreparo(s: Preparo, estado: Int) {
        synchronized(travaPreparo) {
            if (preparoAtual(s) && consultaPreparo.token == s.token)
                consultaPreparo = ConsultaPreparo(s.token, estado)
        }
    }

    private fun soltarPreparo() {
        val s = preparo ?: return
        preparo = null
        val r = s.recursos ?: return
        if (r.construido()) {
            try { s.ouvinte?.let { r.p.removeListener(it) } } catch (_: Exception) {}
            try { s.analitico?.let { r.p.removeAnalyticsListener(it) } } catch (_: Exception) {}
        }
        soltarRecursosPreparo(r)
    }

    private fun liberarFontePreparo(r: RecursosPlayer) {
        r.fontePrecarregada?.releasePreloadMediaSource()
        val fio = r.fioPreparo ?: return
        Handler(fio.looper).post {
            r.seletorPreparo?.release()
            r.capacidadesPreparo?.release()
            (r.carga.allocator as? DefaultAllocator)?.reset()
            fio.quitSafely()
        }
    }

    private fun soltarRecursosPreparo(r: RecursosPlayer) {
        if (r.liberado) return
        r.liberado = true
        r.adotado = false
        r.carga.cancelada = true
        if (r.construido()) liberarEmFundo(r.p, null) {
            // Media3 loader threads must be finished before SimpleCache release.
            try { liberarFontePreparo(r) } finally { r.cache?.liberar() }
        } else {
            try { liberarFontePreparo(r) } finally { r.cache?.liberar() }
        }
    }

    @JvmOverloads @JvmStatic fun preparar(url: String, cabecalhos: String, inicioMs: Int, fracao: Int,
                           token: Int, buffer: Int, cacheMb: Int, idiomaAudio: String = "", kbps: Int = 0) {
        enfileirarPreparo(url, cabecalhos, inicioMs, fracao, token, buffer, cacheMb, false, idiomaAudio, kbps)
    }

    @JvmOverloads @JvmStatic fun prepararProximo(url: String, cabecalhos: String, inicioMs: Int, fracao: Int,
                                  token: Int, buffer: Int, cacheMb: Int, idiomaAudio: String = "", kbps: Int = 0) {
        enfileirarPreparo(url, cabecalhos, inicioMs, fracao, token, buffer, cacheMb, true, idiomaAudio, kbps)
    }

    private fun enfileirarPreparo(url: String, cabecalhos: String, inicioMs: Int, fracao: Int,
                                 token: Int, buffer: Int, cacheMb: Int, proximo: Boolean, idiomaAudio: String, kbps: Int) {
        val profile = profileForCall()
        if (token <= 0 || url.isEmpty() || limitedNext) return
        val pedido = synchronized(travaPreparo) {
            val n = pedidosPreparo.incrementAndGet()
            consultaPreparo = ConsultaPreparo(token, 1)
            n
        }
        principal.post {
            if (pedido != pedidosPreparo.get()) return@post
            soltarPreparo()
            val s = Preparo(token, pedido, url, cabecalhos, inicioMs.coerceAtLeast(0),
                if (inicioMs > 0) 0 else fracao.coerceIn(0, 9999), buffer.coerceIn(0, 256), cacheMb.coerceIn(0, CacheSessao.AUTOMATICO_MB), proximo, idiomaAudio, kbps.coerceIn(0, 400_000), profile)
            preparo = s
            prepararMain(s, SystemClock.elapsedRealtime())
        }
    }

    private fun prepararMain(s: Preparo, espera: Long) {
        if (!preparoAtual(s)) return
        val act = activity
        if (!s.proximo && liberacoesEmCurso.get() > 0 && SystemClock.elapsedRealtime() - espera < 3000L) {
            principal.postDelayed({ prepararMain(s, espera) }, 25)
            return
        }
        if (act == null || (!s.proximo && (player != null || liberacoesEmCurso.get() > 0))) {
            publicarPreparo(s, -1)
            return
        }
        try {
            val r = criarPlayer(act, s.url, s.cab, s.buffer, s.cacheMb, true, 0, !s.proximo, s.idiomaAudio, kbps = s.kbps, profile = s.profile)
            s.recursos = r
            if (s.proximo) {
                prepararFonteProxima(act, s, r)
                agendarPrazoPreparo(s)
                return
            }
            val p = r.p
            p.volume = 0f
            p.playWhenReady = false
            val ouv = object : Player.Listener {
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (!preparoAtual(s) || s.fracaoPendente <= 0) return
                    if (p.isCurrentMediaItemLive) { s.fracaoPendente = 0; return }
                    val d = p.duration
                    if (d == C.TIME_UNSET || d <= 1000L || !p.isCurrentMediaItemSeekable) return
                    val f = s.fracaoPendente
                    s.fracaoPendente = 0
                    try { p.seekTo(d * f / 10000L); s.inicioAceito = true }
                    catch (e: Exception) { Log.w(TAG, "preparo: retomada recusada: $e") }
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (!preparoAtual(s)) return
                    if (state == Player.STATE_READY) {
                        if (s.fracao > 0 && !s.inicioAceito) {
                            publicarPreparo(s, -1)
                            soltarPreparo()
                        } else publicarPreparo(s, 2)
                    } else if (state == Player.STATE_BUFFERING) publicarPreparo(s, 1)
                    else if (state == Player.STATE_ENDED) {
                        publicarPreparo(s, -1)
                        soltarPreparo()
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    if (!preparoAtual(s)) return
                    Log.w(TAG, "[preparo] falhou: ${error.errorCodeName}")
                    publicarPreparo(s, -1)
                    soltarPreparo()
                }
            }
            val ana = object : AnalyticsListener {
                override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String,
                                                       initializedTimestampMs: Long, initializationDurationMs: Long) {
                    if (preparoAtual(s)) {
                        s.videoDecoderName = decoderName
                        s.decoderDv = decoderDolby(decoderName, s.recursos)
                    }
                }
            }
            s.ouvinte = ouv; s.analitico = ana
            p.addListener(ouv); p.addAnalyticsListener(ana)
            val item = itemMidia(s.url)
            if (s.inicioMs > 0) { p.setMediaItem(item, s.inicioMs.toLong()); s.inicioAceito = true }
            else p.setMediaItem(item)
            p.prepare()
            Log.i(TAG, "[preparo] iniciado token=${s.token} pos=${s.inicioMs} fracao=${s.fracao}")
            agendarPrazoPreparo(s)
        } catch (e: Exception) {
            Log.w(TAG, "[preparo] criar: $e")
            publicarPreparo(s, -1)
            soltarPreparo()
        }
    }

    private fun agendarPrazoPreparo(s: Preparo) {
        principal.postDelayed({
            if (preparoAtual(s) && consultaPreparo.estado == 1) {
                publicarPreparo(s, -1)
                soltarPreparo()
            }
        }, PREPARO_ESPERA_MS)
    }

    private class FonteComDuracao(fonte: MediaSource, private val receber: (Timeline) -> Unit) : WrappingMediaSource(fonte) {
        override fun onChildSourceInfoRefreshed(newTimeline: Timeline) {
            receber(newTimeline)
            super.onChildSourceInfoRefreshed(newTimeline)
        }
    }

    private fun prepararFonteProxima(act: Activity, s: Preparo, r: RecursosPlayer) {
        // Sample queues only. Capabilities must have their own descriptors:
        // querying a live audio renderer would enter the current AudioSink from
        // this other looper. These renderers are never enabled, so they create
        // no MediaCodec, AudioTrack, Surface, focus or playback callbacks.
        val fio = HandlerThread("nv-next-preload").apply { start() }
        r.fioPreparo = fio
        val h = Handler(fio.looper)
        val banda = DefaultBandwidthMeter.Builder(act).build() // do not change current playback's ABR meter
        r.bandaPreparo = banda
        val seletor = SeletorAudioPreparo(act, s.idiomaAudio)
        seletor.init({}, banda)
        seletor.parameters = seletor.parameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
        r.seletorPreparo = seletor
        val descritores = androidx.media3.exoplayer.DefaultRenderersFactory(act)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setMediaCodecSelector(videoCodecSelector)
            .setEnableDecoderFallback(true)
        val lista = DefaultRendererCapabilitiesList.Factory(descritores).createRendererCapabilitiesList()
        r.capacidadesPreparo = lista
        val capacidades: Array<RendererCapabilities> = lista.rendererCapabilities
        val fonte = FonteComDuracao(r.fabricaMidia.createMediaSource(itemMidia(s.url))) { timeline ->
            if (!timeline.isEmpty) {
                val w = timeline.getWindow(0, Timeline.Window())
                s.duracaoFonteMs = w.durationMs
                s.fonteBusca = w.isSeekable && !w.isLive
            }
        }
        fun falhou() {
            principal.post {
                if (preparoAtual(s)) { publicarPreparo(s, -1); soltarPreparo() }
            }
        }
        fun retomar(mediaSource: PreloadMediaSource): Boolean {
            val d = s.duracaoFonteMs
            if (!s.fonteBusca || d == C.TIME_UNSET || d <= 1000L) { falhou(); return false }
            s.posicaoFonteMs = d * s.fracaoPendente / 10000L
            s.fracaoPendente = 0
            s.inicioAceito = true
            s.faixasFonteProntas = false
            // The preliminary progressive period has read the container. Stop
            // its preload callback first, then create the exact-position period
            // against the same prepared source on an independent handler.
            h.post { if (preparoAtual(s) && !r.adotado) mediaSource.preload(s.posicaoFonteMs * 1000L) }
            return false
        }
        val controle = object : PreloadMediaSource.PreloadControl {
            override fun onSourcePrepared(mediaSource: PreloadMediaSource): Boolean {
                if (!preparoAtual(s)) return false
                return if (s.fracaoPendente > 0 && s.duracaoFonteMs != C.TIME_UNSET) retomar(mediaSource) else true
            }
            override fun onTracksSelected(mediaSource: PreloadMediaSource): Boolean {
                if (!preparoAtual(s)) return false
                if (s.fracaoPendente > 0) return retomar(mediaSource)
                s.faixasFonteProntas = true
                if (s.inicioMs > 0) s.inicioAceito = s.fonteBusca
                return true
            }
            override fun onContinueLoadingRequested(mediaSource: PreloadMediaSource, bufferedDurationUs: Long): Boolean {
                if (!preparoAtual(s)) return false
                val cheio = r.carga.cheio()
                if (s.faixasFonteProntas && s.fracaoPendente == 0 && (bufferedDurationUs >= 2_500_000L || cheio))
                    publicarPreparo(s, 2)
                return !cheio && bufferedDurationUs < PREPARO_MAX_US
            }
            override fun onLoadedToTheEndOfSource(mediaSource: PreloadMediaSource) {
                if (s.faixasFonteProntas && s.fracaoPendente == 0) publicarPreparo(s, 2) else falhou()
            }
            override fun onUsedByPlayer(mediaSource: PreloadMediaSource) { /* retain queues until player creates its period */ }
            override fun onPreloadError(error: PreloadException, mediaSource: PreloadMediaSource) {
                Log.w(TAG, "[preparo] proximo falhou: ${error.javaClass.simpleName}")
                falhou()
            }
        }
        r.fontePrecarregada = PreloadMediaSource.Factory(r.fabricaMidia, controle, seletor, banda,
            capacidades, r.carga.allocator, fio.looper).createMediaSource(fonte)
        r.fontePrecarregada!!.preload(s.inicioMs.toLong() * 1000L)
        // A large adaptive chunk may be paused at the byte ceiling before its
        // loading callback returns. The selected sample queues still qualify.
        val observar = object : Runnable {
            override fun run() {
                if (!preparoAtual(s)) return
                if (s.faixasFonteProntas && s.fracaoPendente == 0 && r.carga.cheio()) publicarPreparo(s, 2)
                principal.postDelayed(this, TIQUE_MS)
            }
        }
        principal.postDelayed(observar, TIQUE_MS)
        Log.i(TAG, "[preparo] proximo iniciado token=${s.token} pos=${s.inicioMs} fracao=${s.fracao}")
    }

    @JvmStatic fun prepararEstado(token: Int): Int {
        val c = consultaPreparo
        return if (token > 0 && c.token == token) c.estado else 0
    }

    @JvmStatic fun cancelarPreparo(token: Int) {
        val pedido = synchronized(travaPreparo) {
            if (token != 0 && token != consultaPreparo.token) return
            val n = pedidosPreparo.incrementAndGet()
            consultaPreparo = ConsultaPreparo(0, 0)
            n
        }
        principal.post { if (pedido == pedidosPreparo.get()) soltarPreparo() }
    }

    @JvmOverloads @JvmStatic fun abrirPreparada(url: String, cabecalhos: String, inicioMs: Int, fracao: Int,
                                 geracao: Int, token: Int, idiomaAudio: String = "") {
        val pedido = p6OpenRequest()
        val preparoPedido = synchronized(travaPreparo) {
            val n = pedidosPreparo.incrementAndGet()
            consultaPreparo = ConsultaPreparo(0, 0) // a token can be consumed once
            n
        }
        val profile = profileForCall()
        val identidade = cacheIdentidadeArmada
        principal.post {
            if (pedido != pedidos.get()) return@post
            activeBufferProfile = profile
            val s = preparo
            val inicio = inicioMs.coerceAtLeast(0)
            val f = if (inicio > 0) 0 else fracao.coerceIn(0, 9999)
            val r = s?.recursos
            if (preparoPedido == pedidosPreparo.get() && s != null && r != null && s.token == token &&
                s.url == url && s.cab == cabecalhos && s.inicioMs == inicio && s.fracao == f &&
                s.idiomaAudio == idiomaAudio && s.profile == profile &&
                s.buffer == bufferPreset && s.cacheMb == cacheMbPedido && (!r.construido() || r.p.playerError == null) &&
                (f == 0 || s.inicioAceito || s.fracaoPendente > 0)) {
                preparo = null
                pendingAdoptionP6.offer(pedido, s)
                if (r.construido()) {
                    try { s.ouvinte?.let { r.p.removeListener(it) } } catch (_: Exception) {}
                    try { s.analitico?.let { r.p.removeAnalyticsListener(it) } } catch (_: Exception) {}
                }
                abrirMain(url, cabecalhos, false, inicio, geracao, pedido, f, preparada = s,
                    identidadeCache = identidade)
            } else {
                soltarPreparo()
                abrirMain(url, cabecalhos, false, inicio, geracao, pedido, f, identidadeCache = identidade)
            }
        }
    }

    // --- ciclo de vida (NuvioActivity) ---------------------------------------

    // UI-thread mode changes use the same coordinate policy as the native canvas.
    fun interfaceModo(mobile: Boolean) {
        interfaceMobile = mobile
        reaplicarJanela()
    }

    // onCreate, DEPOIS do super.onCreate (a libmain.so ja carregada pelo SDL).
    @JvmStatic
    fun iniciar(activity: Activity, camada: FrameLayout) {
        this.activity = activity
        this.camada = camada
        // F07: seek cache folders of a process that died (crash, kill) go now.
        CacheMidia.limparSobras(activity.cacheDir)
        novaSuperficie(activity)
        // A camada so tem tamanho depois do layout: reaplica a janela quando mudar.
        camada.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or2, ob ->
            if (r - l != or2 - ol || b - t != ob - ot) reaplicarJanela()
        }
        try { nativeIniciar() } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeIniciar sem a lib ainda; o C acha a classe pelo ClassLoader da Activity: $e")
        }
        informarTela(activity)
        val d = decoders4k
        try { nativeDecoder4k(d[0], d[1], d[2], d[3]) }
        catch (e: UnsatisfiedLinkError) { Log.w(TAG, "decoder4k sem lib: $e") }
    }

    // Capacidade HDR/Dolby Vision da tela, para o automatico de fontes. Lista
    // vazia ou erro = -1 (desconhecido); nunca assume que a tela NAO tem HDR.
    private fun informarTela(activity: Activity) {
        try {
            @Suppress("DEPRECATION")
            val tipos = activity.windowManager.defaultDisplay.hdrCapabilities?.supportedHdrTypes
            if (tipos == null || tipos.isEmpty()) { nativeTela(-1, -1); return }
            val dv = if (tipos.contains(android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)) 1 else 0
            nativeTela(1, dv)
        } catch (e: Throwable) { Log.w(TAG, "informarTela: $e") }
    }

    // #409: uma consulta por processo, independente da resolucao da tela.
    // 24 fps e o piso UHD de filme; nao promete 4K60 nem suporte a todo perfil.
    private val decoders4k: IntArray by lazy {
        val mimes = arrayOf("video/hevc", "video/avc", "video/x-vnd.on2.vp9", "video/av01")
        val resultado = IntArray(4) { -1 }
        try {
            val codecs = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos
            if (codecs.isEmpty()) return@lazy resultado
            for ((i, mime) in mimes.withIndex()) {
                var suporta = false
                var incerto = false
                for (codec in codecs) {
                    try {
                        val nome = codec.name.lowercase(java.util.Locale.ROOT)
                        if (codec.isEncoder || nome.endsWith(".secure") ||
                            nome.startsWith("omx.google.") || nome.startsWith("c2.android.") ||
                            nome.startsWith("c2.google.") || nome.contains(".sw.") ||
                            (android.os.Build.VERSION.SDK_INT >= 29 && codec.isSoftwareOnly)) continue
                        if (!codec.supportedTypes.any { it.equals(mime, ignoreCase = true) }) continue
                        val v = codec.getCapabilitiesForType(mime).videoCapabilities
                        if (v == null) { incerto = true; continue }
                        if (v.isSizeSupported(3840, 2160) && v.areSizeAndRateSupported(3840, 2160, 24.0)) {
                            suporta = true
                            break
                        }
                    } catch (e: Exception) { incerto = true; Log.w(TAG, "decoder4k $mime: $e") }
                }
                resultado[i] = if (suporta) 1 else if (incerto) -1 else 0
            }
        } catch (e: Exception) { Log.w(TAG, "decoder4k: $e") }
        resultado
    }

    // onPause: pausa (o C fica sabendo pelo evento 3) e guarda a posicao; o
    // player continua vivo para o onResume nao precisar de nada.
    @JvmStatic
    fun pausarPeloSistema() {
        cancelarPreparo(0)
        // Keep queued opens/stops owned by their original request. A system
        // pause parks a pending open instead of invalidating it and stranding
        // the native loading session.
        ++p6LifecycleEpoch
        playbackIntentP6.onBackground(pedidos.get())
        pausaPedida = true
        afrSuspended = true
        routeForeground = false
        recursosAtuais?.let { cancelAudioRoute(it, restore = true, recheck = true) }
        // Do not destroy a SurfaceView as the Activity is leaving foreground.
        // An already sent detach must still acknowledge and reattach safely.
        detachTicket?.pause()
        if (recriaAgendada && !estavelEmitido) hdrRecriadoPara = ""
        principal.removeCallbacks(recriar)
        recriaAgendada = false
        restoreAfr()
        try { player?.pause() } catch (e: Exception) { Log.w(TAG, "pausarPeloSistema: $e") }
    }

    // Foreground does not resume playback; route validation is independent of AFR.
    fun retomarRotaPeloSistema() {
        if (!routeForeground) ++p6LifecycleEpoch
        playbackIntentP6.onForeground(pedidos.get())
        routeForeground = true
        recursosAtuais?.let { audioRouteTick(sessao, it) }
    }

    // onDestroy: solta player e superficie.
    @JvmStatic
    fun encerrar() {
        cancelarPreparo(0)
        soltarPreparo()
        pedidos.incrementAndGet()
        pendingAdoptionP6.cancelAll()
        liberar()
        ++p6LifecycleEpoch
        playbackIntentP6.reset()
        p6NextPaused.set(false); p6PausedRequest.set(-1)
        esquecerQuadro(); limparRetido(); CacheMidia.descartar("app exit")
        val sv = superficie
        if (sv != null) (sv.parent as? FrameLayout)?.removeView(sv)
        superficie = null
        camada = null
        activity = null
    }

    // --- chamadas do C (qualquer fio) -----------------------------------------

    @JvmStatic fun abrir(url: String, cabecalhos: String) { abrirPosicao(url, cabecalhos, 0, 0) }
    @JvmStatic fun abrirPosicao(url: String, cabecalhos: String, inicioMs: Int, geracao: Int) {
        cancelarPreparo(0)
        val pedido = p6OpenRequest()
        val profile = profileForCall()
        val identidade = cacheIdentidadeArmada
        principal.post {
            if (pedido != pedidos.get()) return@post
            activeBufferProfile = profile
            abrirMain(url, cabecalhos, false, inicioMs.coerceAtLeast(0), geracao, pedido,
                identidadeCache = identidade)
        }
    }
    // Only called by Android native's bounded SAME-source rebuffer/network
    // restart. No user-selected release/episode may call this entry point.
    @JvmStatic fun abrirRecuperacao(url: String, cabecalhos: String, inicioMs: Int, geracao: Int) {
        cancelarPreparo(0)
        val pedido = p6OpenRequest()
        val profile = profileForCall()
        val identidade = cacheIdentidadeArmada
        principal.post {
            if (pedido != pedidos.get()) return@post
            activeBufferProfile = profile
            abrirMain(url, cabecalhos, false, inicioMs.coerceAtLeast(0), geracao,
                    pedido, mesmaFonte = true, identidadeCache = identidade)
        }
    }
    @JvmStatic fun abrirRetomada(url: String, cabecalhos: String, inicioMs: Int, fracao: Int, geracao: Int) {
        cancelarPreparo(0)
        val pedido = p6OpenRequest()
        val profile = profileForCall()
        val identidade = cacheIdentidadeArmada
        principal.post {
            if (pedido != pedidos.get()) return@post
            activeBufferProfile = profile
            abrirMain(url, cabecalhos, false, inicioMs.coerceAtLeast(0), geracao, pedido, fracao.coerceIn(0, 9999),
                identidadeCache = identidade)
        }
    }
    @JvmStatic fun parar() {
        cancelarPreparo(0)
        val pedido = pedidos.incrementAndGet()
        principal.post { if (pedido == pedidos.get()) {
            pendingAdoptionP6.cancelAll()
            limparRetido()
            liberar(guardarCache = true)
        } }
    }
    private val playbackIntentP6 = PlaybackIntentP6()
    private val p6NextPaused = java.util.concurrent.atomic.AtomicBoolean(false)
    private val p6PausedRequest = java.util.concurrent.atomic.AtomicInteger(-1)
    @JvmStatic fun p6ArmPausedOpen() { p6NextPaused.set(true) }
    // Exact raw source fingerprint, not a URL, header, or reusable credential.
    @JvmStatic fun p6SourceFingerprint(url: ByteArray, headers: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        fun field(bytes: ByteArray) {
            md.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.size).array())
            md.update(bytes)
        }
        field(url); field(headers)
        val hex = "0123456789abcdef"
        return buildString(64) {
            for (b in md.digest()) {
                val x = b.toInt() and 255
                append(hex[x ushr 4]); append(hex[x and 15])
            }
        }
    }
    private fun p6OpenRequest(): Int {
        val request = pedidos.incrementAndGet()
        if (p6NextPaused.getAndSet(false)) p6PausedRequest.set(request)
        return request
    }
    @Volatile private var p6LifecycleEpoch = 0
    private var pausaPedida = false
    @JvmStatic fun pausar(p: Int) {
        val pedido = pedidos.get()
        val lifecycle = p6LifecycleEpoch
        // Source and lifecycle ownership: a pre-Home queued Play must not
        // override the system pause or a newer selected stream.
        principal.post {
            if (pedido != pedidos.get() || lifecycle != p6LifecycleEpoch) return@post
            if ((videoOutputFailed || recursosAtuais?.routeFailed == true) && p == 0) return@post
            pausaPedida = playbackIntentP6.command(pedido, p != 0)
            if (!pausaPedida && afrSuspended) {
                afrSuspended = false
                applyAfr(sessao, player?.videoFormat, resumed = true)
                aplicarVolume()
                hdrNaSuperficie()
                avaliarEstavel()
            }
            player?.playWhenReady = !pausaPedida
            recursosAtuais?.let { audioRouteTick(sessao, it) }
        }
    }
    // Episode handoff arms this token before the old visible session closes.
    // Stop only that session; the exact selected preload keeps its queues.
    @JvmStatic fun pararMantendoPreparo(token: Int) {
        val preparoPedido = synchronized(travaPreparo) {
            if (token > 0 && consultaPreparo.token == token) pedidosPreparo.get() else -1
        }
        if (preparoPedido < 0) cancelarPreparo(0)
        val pedido = pedidos.incrementAndGet()
        principal.post {
            if (pedido != pedidos.get()) return@post
            if (preparoPedido < 0 || preparoPedido != pedidosPreparo.get()) {
                cancelarPreparo(0)
                soltarPreparo()
            }
            pendingAdoptionP6.cancelAll()
            liberar()
        }
    }
    @JvmStatic fun buscar(ms: Int, revision: Int) {
        val pedido = pedidos.get()
        principal.post {
            if (pedido != pedidos.get() || pedidoAtivo != pedido || videoOutputFailed) return@post
            val p = player ?: return@post
            val busca = ++buscaSerial
            ++retidoPedido // invalidate an older PixelCopy even for an in-buffer seek
            val alvo = ms.coerceAtLeast(0).toLong()
            relatarAudio(sessao, "seek alvoMs=$ms")
            // In-buffer seeks and ordinary buffering keep the SAME hardware
            // SurfaceView. For a provably forward out-of-buffer seek, make a
            // best-effort SDR hold before the decoder may flush its output.
            // Native D-pad scrubbing already commits on release. Collapse any
            // other queued commits before starting a PixelCopy or a range load.
            principal.post applySeek@{
                if (pedido != pedidos.get() || player !== p || busca != buscaSerial) return@applySeek
                val commit = {
                    if (pedido == pedidos.get() && player === p && busca == buscaSerial && !videoOutputFailed) {
                        buscarComHandoff(p, alvo)
                        appliedSeekRevision = revision
                    }
                }
                val sv = superficie
                if (sv != null && FrameHoldP2B.beyondKnownBuffer(
                        alvo, p.currentPosition, p.bufferedPosition, p.isCurrentMediaItemLive)) {
                    capturarRetido(p, sv, pedido, commit)
                } else commit()
            }
        }
    }
    @JvmStatic fun volume(pct: Int) { principal.post {
        volumeRequisitado = pct.coerceIn(0, 100) / 100f
        aplicarVolume()
    } }
    // F07: cache limit for the NEXT opens (MB, 0 = off) and the session volume
    // 0..200 (above 100 only while the audio is PCM).
    // #338: buffer size in MiB (0 Auto; C already clamped it to free RAM), sticky, read at open.
    private data class HttpConfig(val enabled: Boolean, val count: Int, val chunkMb: Int)
    @Volatile private var httpSettings = HttpConfig(true, 4, 4)
    @JvmStatic fun httpConfig(enabled: Int, count: Int, chunkMb: Int) {
        httpSettings = HttpConfig(enabled != 0, count.coerceIn(2, 6), chunkMb.coerceIn(1, 4))
    }
    private val nextProfile = ThreadLocal<RamBufferProfile>()
    private var activeBufferProfile = RamBufferProfile()
    private fun profileForCall(): RamBufferProfile {
        val snapshot = nextProfile.get() ?: RamBufferProfile()
        nextProfile.remove()
        return snapshot
    }
    @JvmStatic fun bufferProfile(min: Int, max: Int, start: Int, rebuffer: Int, back: Int,
                                 keyframe: Int, time: Int, mb: Int) {
        nextProfile.set(RamBufferProfile(min, max, start, rebuffer, back, keyframe != 0, time != 0, mb))
    }
    @Volatile private var bufferPreset = 0
    @JvmStatic fun buffer(mb: Int) { bufferPreset = mb.coerceIn(0, 256) }
    // Declared kbps of the next open (0 = unknown/trailer), sticky like buffer().
    @Volatile private var kbpsDeclarado = 0
    @JvmStatic fun kbps(k: Int) { kbpsDeclarado = k.coerceIn(0, 400_000) }
    @JvmStatic fun cache(mb: Int) { cacheMbPedido = mb.coerceIn(0, CacheSessao.AUTOMATICO_MB) }
    @JvmStatic fun cacheTitle(id: String) { tituloPedido = id }
    @JvmStatic fun cacheIdentity(conta: String, perfil: Int, catalogo: String,
                                  addon: String, torrent: String, arquivo: Int, bytes: Long) {
        cacheIdentidadeArmada = CacheProvaP4C.Identidade(
            conta, perfil, catalogo, addon, torrent, arquivo, bytes).takeIf { it.forte() }
    }
    // Profile switch (C side): the kept cache belongs to the old profile.
    @JvmStatic fun descartarGuardado() { principal.post { CacheMidia.descartar("profile switch"); esquecerQuadro() } }
    @JvmStatic fun ganho(pct: Int) {
        principal.post { ganhoPct = GanhoMath.pct(pct); aplicarGanho() }
    }

    // Live adjustments persist only while requested by the native Audio panel.
    // The processor is bypassed entirely for bitstream passthrough.
    @JvmStatic fun audioAdjustments(amp: Int, bass: Int, center: Int, night: Int) {
        val pedido = pedidos.get()
        principal.post {
            if (pedido != pedidos.get()) return@post
            amplificacaoDb = amp.coerceIn(0, 10)
            baixoDb = bass.coerceIn(-12, 6)
            centroDb = center.coerceIn(-10, 30)
            noiteNivel = night.coerceIn(0, 3)
            aplicarGanho()
            // FFmpeg/MediaCodec decode the ORIGINAL compressed track into PCM.
            // Re-negotiate the sink only when DSP crosses on/off: never encode
            // PCM back to lossy AC-3 or downgrade the selected source track.
            val requirePcm = amplificacaoDb != 0 || baixoDb != 0 ||
                centroDb != 0 || noiteNivel != 0
            val r = recursosAtuais ?: return@post
            val p = player ?: return@post
            if (r.ajustePcm != requirePcm) {
                cancelAudioRoute(r, restore = true, recheck = true)
                val pos = p.currentPosition
                val resume = p.playWhenReady
                r.ajustePcm = requirePcm
                p.stop()
                p.seekTo(pos)
                p.prepare()
                p.playWhenReady = resume
                Log.i(TAG, "[audio] PCM processing=" + requirePcm + " (PCM route requested; output fidelity separately verified)")
            }
        }
    }
    // C reapplies speed after each open; keep PCM policy with that player.
    @JvmStatic fun velocidade(centesimos: Int) {
        val v = centesimos.coerceIn(25, 400) / 100f
        val pedido = pedidos.get()
        principal.post {
            if (pedido != pedidos.get()) return@post
            val p = player ?: return@post
            val r = recursosAtuais ?: return@post
            val pcm = v != 1f
            if (r.velocidadePcm != pcm) {
                cancelAudioRoute(r, restore = true, recheck = true)
                val pos = p.currentPosition
                r.velocidadePcm = pcm
                // Re-select the audio decoder against the new sink capabilities.
                // stop/prepare keeps the media source, track overrides and pause.
                p.stop()
                p.seekTo(pos)
                p.setPlaybackSpeed(v)
                p.prepare()
            } else p.setPlaybackSpeed(v)
            Log.i(TAG, "[speed] requested=$v parameters=${p.playbackParameters.speed} pcm=$pcm")
        }
    }
    private val janelaPedido = AtomicInteger()
    // Request -> applied on the UI thread (definida) -> acknowledged after a
    // traversal on a valid surface (aplicada). Lifecycle/layout events drop the
    // acknowledgement and rearm it; the SDL-side query never mutates state.
    @Volatile private var janelaDefinida = -1
    @Volatile private var janelaAplicada = -1
    @Volatile private var janelaSuperficieAplicada: SurfaceView? = null
    private var janelaUltimoPedido: List<Any?>? = null
    @JvmStatic fun janelaPronta(): Boolean {
        val sv = superficie ?: return false
        return !sv.isLayoutRequested && sv.visibility == View.VISIBLE &&
            sv.holder.surface.isValid && !recriando && !recriaAgendada &&
            janelaAplicada == janelaPedido.get() && janelaSuperficieAplicada === sv
    }
    private fun observarJanela(sv: SurfaceView) {
        sv.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or2, ob ->
            if (superficie === sv && (l != ol || t != ot || r != or2 || b != ob))
                recuperarJanela(sv)
        }
    }
    private fun recuperarJanela(sv: SurfaceView) {
        janelaSuperficieAplicada = null
        // A request still queued confirms itself once applied.
        val pedido = janelaPedido.get()
        if (janelaDefinida == pedido) confirmarJanela(sv, pedido)
    }
    private fun confirmarJanela(sv: SurfaceView, pedido: Int) {
        // A queued request is not a laid-out SurfaceView. Acknowledge only
        // after traversal, and never let an old request acknowledge a new one.
        sv.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (pedido != janelaPedido.get() || superficie !== sv || player == null) {
                    sv.viewTreeObserver.removeOnPreDrawListener(this)
                } else if (!sv.isLayoutRequested && sv.visibility == View.VISIBLE &&
                           sv.holder.surface.isValid && !recriando && !recriaAgendada) {
                    janelaSuperficieAplicada = sv
                    janelaAplicada = pedido
                    sv.viewTreeObserver.removeOnPreDrawListener(this)
                }
                return true
            }
        })
        sv.invalidate()
    }
    @Synchronized @JvmStatic fun janela(x: Int, y: Int, w: Int, h: Int, encaixa: Int) {
        val geometria = listOf(x, y, w, h, encaixa, superficie)
        // Crop retries still apply layout, but unchanged requests keep their acknowledgement.
        val pedido = if (geometria == janelaUltimoPedido) janelaPedido.get() else {
            janelaUltimoPedido = geometria
            janelaPedido.incrementAndGet()
        }
        principal.post {
            definirJanela(x, y, w, h, encaixa != 0)
            janelaDefinida = pedido
            val sv = superficie ?: return@post
            confirmarJanela(sv, pedido)
        }
    }
    @JvmStatic fun escolher(tipo: Int, idx: Int, geracao: Int) {
        val pedido = pedidos.get()
        principal.post {
            val latest = pedidos.get()
            if (pedido != latest) return@post
            // Track ordinals only belong to the opening that published them.
            // The request counter can already refer to a queued replacement
            // while the old native callback is still returning.
            if ((tipo == 0 || tipo == 1) &&
                !trackCommandOwnedP7(pedido, latest, pedidoAtivo, geracao, geracaoNative)) return@post
            escolherMain(tipo, idx)
        }
    }

    // --- abrir / liberar ------------------------------------------------------

    private fun itemMidia(url: String): MediaItem {
        val baixa = url.lowercase()
        val item = MediaItem.Builder().setUri(url)
        if (baixa.contains(".m3u8") || baixa.contains("m3u8?")) item.setMimeType(MimeTypes.APPLICATION_M3U8)
        else if (baixa.contains(".mpd")) item.setMimeType(MimeTypes.APPLICATION_MPD)
        return item.build()
    }

    private fun decoderDolby(nome: String, recursos: RecursosPlayer?): Boolean =
        recursos?.videoConfig?.info?.let {
            it.name == nome && it.mimeType == MimeTypes.VIDEO_DOLBY_VISION
        } == true

    private fun displayHdr(act: Activity, type: Int): Boolean = try {
        val display = act.windowManager.defaultDisplay
        val types = if (android.os.Build.VERSION.SDK_INT >= 34) display.mode.supportedHdrTypes
            else display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        type in types
    } catch (_: Exception) { false }

    private fun verifiedBase(f: Format): HdrCodecFallbackP5B.VideoConfig? {
        val config = recursosAtuais?.videoConfig ?: return null
        val act = activity ?: return null
        return config.takeIf {
            it.matches(f, videoDecoderName) && it.info.mimeType == MimeTypes.VIDEO_H265 &&
                it.base?.ready() == true &&
                displayHdr(act, android.view.Display.HdrCapabilities.HDR_TYPE_HDR10)
        }
    }

    // Source metadata alone cannot establish DV/HDR10 playback. Check the
    // actual chosen decoder, exact parsed DV profile/level and output mode.
    private fun compatibleDolby(f: Format): Boolean {
        val config = recursosAtuais?.videoConfig ?: return false
        val act = activity ?: return false
        if (!config.matches(f, videoDecoderName) || config.info.mimeType != MimeTypes.VIDEO_DOLBY_VISION) return false
        return try {
            val profile = androidx.media3.common.util.CodecSpecificDataUtil.getCodecProfileAndLevel(f)
                ?: return false
            val caps = config.info.capabilities ?: return false
            val exactProfile = caps.profileLevels.any { it.profile == profile.first && it.level >= profile.second }
            val vc = caps.videoCapabilities
            val size = f.width > 0 && f.height > 0 && vc != null &&
                if (f.frameRate.isFinite() && f.frameRate > 0f)
                    vc.areSizeAndRateSupported(f.width, f.height, f.frameRate.toDouble())
                else vc.isSizeSupported(f.width, f.height)
            HdrPolicyP5.compatibleDolby(displayHdr(act, android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION),
                exactProfile, size)
        } catch (_: Exception) { false }
    }

    // Cold opens and prepared opens share renderers, headers, cache, decoder
    // fallback and memory policy. Only preload's audio callbacks and source
    // factory stay dormant until this same player becomes the active session.
    private fun criarPlayer(act: Activity, url: String, cabecalhos: String, preset: Int,
                            pedidoCache: Int, preparando: Boolean, geracao: Int, construir: Boolean = true,
                            idiomaAudio: String = "", titulo: String = "", kbps: Int = kbpsDeclarado,
                            profile: RamBufferProfile = activeBufferProfile,
                             identidadeCache: CacheProvaP4C.Identidade? = null): RecursosPlayer {
        // Em aparelho de pouca RAM (ActivityManager: isLowRamDevice,
        // memoryClass <= 128 MB ou totalMem <= 3 GiB; a TCL de 2,4 GB tem
        // memoryClass 192 e so cai pelo totalMem)
        // o teto do buffer cai para a metade: gpu-cache + buffer + pedacos
        // paralelos + preparo do proximo titulo estouraram uma TV de 2,4 GB
        // (PSS ~575 MB, lmkd matou o app). Aparelho com RAM sobrando nao muda.
        val heap = Runtime.getRuntime().maxMemory()
        val am = act.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val totalMem = am?.let { a -> android.app.ActivityManager.MemoryInfo().also { a.getMemoryInfo(it) }.totalMem } ?: 0L
        val poucaRam = (am != null && (am.isLowRamDevice || am.memoryClass <= 128 ||
                        (totalMem in 1L..(3L shl 30)))).also { poucaRamAparelho = it }
        val tetoBuffer = if (poucaRam) (heap / 8).coerceIn(24L shl 20, 48L shl 20).toInt()
                         else (heap / 4).coerceIn(32L shl 20, 96L shl 20).toInt()
        // preset = MiB from Settings (0 Auto). ExoPlayer holds it in the Java heap: never past heap/3.
        // Auto with a high declared bitrate: 48 MB is ~7 s of a 52.8 Mbps remux
        // (TCL rebuffered every 12-30 s). Aim at 30 s of it, never past heap/5
        // and 80 MB on low RAM (the 575 MB PSS kill), heap/3 and 192 MB otherwise.
        val porBitrate = kbps.toLong() * 1000 / 8 * 30
        val tetoAlto = if (poucaRam) minOf(heap / 5, 80L shl 20) else minOf(heap / 3, 192L shl 20)
        val autoBytes = porBitrate.coerceAtMost(tetoAlto).coerceAtLeast(tetoBuffer.toLong()).toInt()
        val targetMb = if (profile.targetMb >= 0) profile.targetMb else preset
        val (minMs, maxMs) = profile.durations(targetMb > 0)
        val bytes = if (targetMb > 0) minOf(targetMb.toLong() shl 20, tetoAlto).toInt()
                    else minOf(autoBytes.toLong(), tetoAlto).toInt()
        val r = RecursosPlayer(CargaPlayer(minMs, maxMs, bytes, preparando, profile), maxMs,
            if (preparando) idiomaAudio else "")
        r.routeObserver = AudioRouteObserverP5D(act, principal)
        try {
            val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
            val props = HashMap<String, String>()
            var ua: String? = null
            for (linha in cabecalhos.split('\n')) {
                val i = linha.indexOf(':')
                if (i <= 0) continue
                val nome = linha.substring(0, i).trim()
                val valor = linha.substring(i + 1).trim()
                if (nome.isEmpty()) continue
                if (nome.equals("User-Agent", ignoreCase = true)) { http.setUserAgent(valor); ua = valor }
                else props[nome] = valor
            }
            http.setDefaultRequestProperties(props)
            val limited = !preparando && limitedNext
            // Check only once per player. HLS segments share the same connection gate.
            val checked = java.util.concurrent.atomic.AtomicBoolean()
            val serialHttp: DataSource.Factory = if (limited) LimitedDataSource.Factory(http, { r.carga.cancelada }) {
                if (checked.compareAndSet(false, true)) nativeLimitedStatus() else 0
            } else http
            val baixa = url.lowercase()
            val adaptativo = baixa.contains(".m3u8") || baixa.contains("m3u8?") || baixa.contains(".mpd")
            // Cache off or not applicable for a VOD: whatever was kept is stale.
            if (pedidoCache <= 0 || limited) { r.cacheEstado = CacheSessao.DESLIGADO; if (titulo.isNotEmpty()) CacheMidia.descartar("setting off") }
            else if (adaptativo || !ParaleloDataSource.serve(Uri.parse(url))) {
                r.cacheEstado = CacheSessao.NAO_SE_APLICA
                if (titulo.isNotEmpty()) CacheMidia.descartar("not progressive")
            } else {
                val aoFalhar: (Boolean) -> Unit = { semEspaco ->
                    principal.post {
                        r.cacheEstado = if (semEspaco) CacheSessao.DISCO_CHEIO else CacheSessao.FALHOU
                        if (r.adotado && recursosAtuais === r && cacheEstado == CacheSessao.ATIVO) {
                            cacheEstado = r.cacheEstado
                            Log.w(TAG, "seek cache off for this session (${if (semEspaco) "ENOSPC" else "write error"})")
                            relatarCache(sessao)
                        }
                    }
                }
                // Only exact native provenance and a fresh strong HTTP 206
                // may authorize last-title reuse. Unknown releases remain
                // isolated to the current player session.
                val escopo = CacheProvaP4C.escopoCabecalhos(
                    if (ua != null) props + ("User-Agent" to ua) else props)
                val prova = identidadeCache?.takeIf { it.forte() && it.catalogo == titulo }?.assinatura()
                val (c, estado) = CacheMidia.criar(act.cacheDir, pedidoCache, aoFalhar,
                    listOf(prova ?: "unverified", escopo).joinToString(":"),
                    prova = prova, titulo = titulo, url = url,
                    totalDeclarado = identidadeCache?.bytes ?: 0L)
                r.cache = c; r.cacheEstado = estado
            }
            r.rede = { token ->
                val config = httpSettings
                val budget = minOf(heap / 12, if (poucaRam) 12L shl 20 else 32L shl 20)
                val chunk = minOf(config.chunkMb shl 20, (budget / (1 + config.count)).toInt()).coerceAtLeast(128 shl 10)
                val rede: DataSource.Factory = if (limited) serialHttp
                    else ParaleloDataSource.Factory(ua, props, http, medidor, token, r.parallelStats,
                        if (config.enabled) config.count else 1, chunk)
                r.cache?.fabrica(rede) { r.carga.cancelada } ?: rede
            }
            val origemPreparo = r.cache?.fabrica(serialHttp) { r.carga.cancelada } ?: serialHttp
            val token = if (preparando) 0L else medidor.sessao(geracao).also { medidorToken = it }
            r.origem = OrigemPlayer(if (preparando) origemPreparo else r.rede(token), r.carga, r.contadorHttp)
            // ON: platform decoder/passthrough first, FFmpeg only as fallback.
            // F06 tap is at the sink input; F07 gain stays in its processors.
            r.renderizadores = object : androidx.media3.exoplayer.DefaultRenderersFactory(act) {
                override fun getCodecAdapterFactory(): androidx.media3.exoplayer.mediacodec.MediaCodecAdapter.Factory =
                    HdrCodecFallbackP5B.Factory(super.getCodecAdapterFactory(),
                        { displayHdr(act, android.view.Display.HdrCapabilities.HDR_TYPE_HDR10) },
                        { displayHdr(act, android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION) },
                        { r.runtimeVideoRecovery },
                        { config -> r.videoConfig = config })
                override fun buildVideoRenderers(context: Context, extensionRendererMode: Int,
                                                  mediaCodecSelector: MediaCodecSelector, enableDecoderFallback: Boolean,
                                                  eventHandler: Handler, eventListener: VideoRendererEventListener,
                                                  allowedVideoJoiningTimeMs: Long, out: ArrayList<Renderer>) {
                    val start = out.size
                    // Keep the stock extension enumeration and preference order.
                    super.buildVideoRenderers(context, extensionRendererMode, mediaCodecSelector,
                        enableDecoderFallback, eventHandler, eventListener, allowedVideoJoiningTimeMs, out)
                    val slot = (start until out.size).firstOrNull {
                        out[it].javaClass == MediaCodecVideoRenderer::class.java
                    } ?: return
                    // These are the unchanged pinned Media3 1.8 default video
                    // builder settings used by this factory (no experimental toggles).
                    val builder = MediaCodecVideoRenderer.Builder(context)
                        .setCodecAdapterFactory(getCodecAdapterFactory())
                        .setMediaCodecSelector(mediaCodecSelector)
                        .setEnableDecoderFallback(enableDecoderFallback)
                        .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
                        .setEventHandler(eventHandler).setEventListener(eventListener)
                        .setMaxDroppedFramesToNotify(50)
                    out[slot] = HdrCodecFallbackP5B.GuardedRenderer(builder, { r.videoConfig },
                        { r.runtimeVideoRecovery },
                        { displayHdr(act, android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION) },
                        { displayHdr(act, android.view.Display.HdrCapabilities.HDR_TYPE_HDR10) },
                        { at ->
                            principal.post {
                                if (recursosAtuais === r && atual(sessao) && !videoOutputFailed &&
                                    !estavelEmitido && at >= superficieInvalidadaEm)
                                    invalidarQuadroSuperficie(at)
                            }
                        })
                }
                override fun buildAudioSink(context: Context, enableFloatOutput: Boolean,
                                            enableAudioTrackPlaybackParams: Boolean): AudioSink =
                    // Sonic needs integer PCM; float output bypasses audio processors.
                    AudioSyncSink(DefaultAudioSink.Builder(context).setEnableFloatOutput(false)
                        .setEnableAudioTrackPlaybackParams(false)
                        .setAudioTrackProvider(r.routeObserver)
                        .setAudioProcessors(arrayOf(r.proc)).build(), r.tap,
                        { r.velocidadePcm || r.ajustePcm || r.routePcm })
            }.setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setMediaCodecSelector(MediaCodecSelector { mime, secure, tunneling ->
                    val candidates = videoCodecSelector.getDecoderInfos(mime, secure, tunneling)
                    val failed = r.runtimeVideoRecovery?.failed
                    if (failed != null && MimeTypes.isVideo(mime))
                        candidates.filter { it.name != failed } else candidates
                })
                .setEnableDecoderFallback(true)
            r.fabricaMidia = DefaultMediaSourceFactory(act)
                // ParaleloDataSource faz as tres tentativas de transporte;
                // nao empilhar outra rodada invisivel do Loader antes da
                // maquina de reconexao 3/10/25 s do nucleo.
                .setLoadErrorHandlingPolicy(PoliticaErroParalelo())
                .setDataSourceFactory(if (limited) DefaultDataSource.Factory(act, r.origem) else LivePlaylistDataSource.Factory(
                    DefaultDataSource.Factory(act, r.origem),
                    r.contadorHttp.medir(DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(2000).setReadTimeoutMs(2000)
                        .setUserAgent(ua).setDefaultRequestProperties(props.filterKeys {
                            !it.equals("If-None-Match", true) && !it.equals("If-Modified-Since", true)
                        })),
                    report = { Log.i(TAG, it) },
                    liveTsUri = Uri.parse(url).takeIf { LivePlaylistDataSource.isLiveTs(it) },
                ))
            if (construir) construirPlayer(act, r)
            Log.i(TAG, "[player] buffer preset=$preset min=${minMs / 1000}s max=${maxMs / 1000}s teto=${bytes shr 20}MB preparo=$preparando")
            return r
        } catch (e: Exception) {
            soltarRecursosPreparo(r)
            throw e
        }
    }

    private fun construirPlayer(act: Activity, r: RecursosPlayer) {
        val builder = ExoPlayer.Builder(act, r.renderizadores).setLoadControl(r.carga).setMediaSourceFactory(r.fabricaMidia)
            // Renderer teardown runs on Media3's playback thread. Bound UI waits.
            .setReleaseTimeoutMs(200)
            .setDetachSurfaceTimeoutMs(200)
            .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF)
        val seletor = if (r.idiomaAudio.isNotEmpty()) SeletorAudioPreparo(act, r.idiomaAudio)
                      else DefaultTrackSelector(act)
        seletor.parameters = seletor.parameters.buildUpon()
            .setTunnelingEnabled(nativeTunneling()).build()
        builder.setTrackSelector(seletor)
        r.fioPreparo?.let { builder.setPlaybackLooper(it.looper) }
        r.bandaPreparo?.let { builder.setBandwidthMeter(it) }
        r.p = builder.build()
        if (liberacoesEmCurso.get() > 0) {
            try { r.p.audioSessionId = androidx.media3.common.util.Util.generateAudioSessionIdV21(act) }
            catch (e: Exception) { Log.w(TAG, "sessao de audio nova: $e") }
        }
        // Sem foco de audio: o volume da soundbar/HDMI nao pode pausar (#581).
        r.p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), false)
        r.p.trackSelectionParameters = r.p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
    }

    private fun abrirMain(url: String, cabecalhos: String, reabrindo: Boolean,
                          inicioMs: Int, geracao: Int, pedido: Int, fracao: Int = 0, esperaInicio: Long = -1L,
                          preparada: Preparo? = null, mesmaFonte: Boolean = false,
                          capturaTentada: Boolean = false,
                          identidadeCache: CacheProvaP4C.Identidade? = null) {
        if (pedido != pedidos.get()) {
            pendingAdoptionP6.cancel(pedido)
            return
        }
        pendingAdoptionP6.cancelExcept(pedido)
        // A delayed continuation cannot resurrect a canceled preload.
        if (preparada != null && !pendingAdoptionP6.owns(pedido, preparada)) return
        val act = activity
        if (act == null) {
            pendingAdoptionP6.cancel(pedido)
            confirmarRetomada(geracao, false)
            return
        }
        // A full source switch always drops an old temporary frame even when
        // the URL happens to look similar. Native recovery explicitly opts in.
        if (!mesmaFonte && esperaInicio < 0) limparRetido()
        if (mesmaFonte && !capturaTentada && esperaInicio < 0) {
            val antigo = player
            val sv = superficie
            if (antigo != null && sv != null) {
                capturarRetido(antigo, sv, pedido) {
                    if (pedido == pedidos.get())
                        abrirMain(url, cabecalhos, reabrindo, inicioMs, geracao, pedido,
                            fracao, esperaInicio, preparada, mesmaFonte, true, identidadeCache)
                }
                return
            }
        }
        val espera = if (esperaInicio < 0) SystemClock.elapsedRealtime() else esperaInicio
        if (esperaInicio < 0) {
            liberar(preservarRetido = mesmaFonte)
            // O C envia ganho/pausa depois de abrir; a espera nao pode apaga-los.
            ganhoPct = 100
            pausaPedida = playbackIntentP6.opening(pedido, mesmaFonte, reabrindo, pausaPedida, p6PausedRequest.get() == pedido)
            afrSuspended = playbackIntentP6.systemPaused
        }
        // MStar/Amlogic: o overlay e o decoder antigos precisam sair antes do novo.
        // Poll no Handler deixa Back/parar funcionar enquanto o release corre em fundo.
        if (limitedNext || limitedSession || preparada?.proximo == true || (semRecriar && preparada == null)) {
            val ms = SystemClock.elapsedRealtime() - espera
            if (liberacoesEmCurso.get() > 0 && (limitedNext || limitedSession || ms < 3000)) {
                principal.postDelayed({
                    abrirMain(url, cabecalhos, reabrindo, inicioMs, geracao, pedido, fracao, espera,
                        preparada, mesmaFonte, true, identidadeCache)
                }, 25)
                return
            }
            Log.i(TAG, "[player] esperou o release anterior $ms ms (pendentes=${liberacoesEmCurso.get()})")
        }
        // Only the request reaching construction takes ownership out of the slot.
        if (preparada != null && pendingAdoptionP6.take(pedido) !== preparada) return
        try {
            limitedSession = limitedNext
            novaSuperficie(act)
            mostrarQuadro(url, tituloPedido)
            // A more recent SAME-source SDR hold takes precedence over the
            // older warm-resume thumbnail during a bounded recovery reopen.
            retidoView?.bringToFront()
            pedidoAtivo = pedido
            hdrRecriadoPara = ""; quadroVisto = false
            superficieInvalidadaEm = SystemClock.elapsedRealtime()
            somLiberado = false
            recriando = false; recriaAgendada = false; recuperouSuperficie = false; estavelEmitido = false
            principal.removeCallbacks(recriar)
            principal.removeCallbacks(estavelRun)
            val minha = sessao
            urlAtual = url
            cabAtual = cabecalhos
            inicioAtualMs = inicioMs
            geracaoNative = geracao
            appliedSeekRevision = 0
            ev(EV_CARGA_FASE, 1, geracao = geracao) // building Media3 player
            fracaoAtual = if (inicioMs > 0) 0 else fracao
            fracaoPendente = 0
            abriuEm = SystemClock.elapsedRealtime()
            if (!reabrindo) retentou = false
            quadrosPerdidos = 0L; statsTiques = 0
            val titulo = tituloPedido
            tituloAberto = ""
            val r = preparada?.recursos ?: criarPlayer(act, url, cabecalhos, bufferPreset, cacheMbPedido, false, geracao,
                titulo = titulo, identidadeCache = identidadeCache)
            if (preparada != null) {
                medidorToken = medidor.sessao(geracao)
                r.promover(medidorToken)
                if (!r.construido()) construirPlayer(act, r)
            }
            val p = r.p
            recursosAtuais = r
            player = p
            // Publish cache ownership before any fallible route/listener setup.
            cacheMidia = r.cache; cacheEstado = r.cacheEstado
            r.routeDebounce.seed(r.routeObserver.key(r.routeObserver.route()))
            // An adopted warm player has already created its actual AudioTrack.
            if (preparada != null) r.routeValidatedSerial = r.routeObserver.output?.serial ?: 0L
            r.routeObserver.start { audioRouteTick(minha, r) }
            processador = r.proc
            r.tap.ligado = tap.ligado
            tap = r.tap
            if (preparada == null && r.cache != null) { tituloAberto = titulo; chaveAberta = url.substringBefore('#') }
            bufferAlvoMs = r.alvoMs; bufferMinMs = r.carga.minMs; bufferBytes = r.carga.bytes
            if (preparada != null) {
                videoDecoderName = preparada.videoDecoderName
                decoderDv = preparada.decoderDv
                p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), false)
                try { nativeAudioEstado(r.audioFormato) } catch (_: UnsatisfiedLinkError) {}
            }
            aplicarGanho()
            relatarCache(minha)
            val ouv = ouvinte(minha, geracao); val ana = analitico(minha, geracao)
            ouvinteAtual = ouv; analiticoAtual = ana
            p.addListener(ouv)
            p.addAnalyticsListener(ana)

            val sv = superficie
            if (sv != null) {
                sv.visibility = View.VISIBLE
                p.setVideoSurfaceView(sv)
                sv.holder.addCallback(depoisDoMedia3)
            }
            temJanela = false
            aplicarEncaixe()

            if (preparada == null) {
                val mediaItem = itemMidia(url)
                var inicioAceito = false
                if (inicioMs > 0) {
                    try {
                        // Set position before prepare; no redundant initial Range.
                        p.setMediaItem(mediaItem, inicioMs.toLong())
                        inicioAceito = true
                    } catch (e: Exception) {
                        Log.w(TAG, "posicao inicial recusada; usando retomada normal: $e")
                        p.setMediaItem(mediaItem)
                    }
                } else p.setMediaItem(mediaItem)
                if (inicioMs <= 0 && fracaoAtual > 0) fracaoPendente = fracaoAtual
                else confirmarRetomada(geracao, inicioAceito)
                p.playWhenReady = !pausaPedida
                ev(EV_CARGA_FASE, 2, geracao = geracao) // prepare resolves media format
                // false -> false nao gera callback quando a pausa chegou sem player.
                if (pausaPedida) ev(EV_PAUSADO)
                p.prepare()
            } else if (preparada.proximo) {
                // The next-episode source owns the warmed sample queues. Its
                // new player prepares that existing source at the exact same
                // period position, after the previous decoder has been released.
                fracaoPendente = preparada.fracaoPendente
                p.setMediaSource(r.fontePrecarregada!!, preparada.posicaoFonteMs)
                if (fracaoPendente <= 0) confirmarRetomada(geracao, inicioMs > 0 || preparada.inicioAceito)
                p.playWhenReady = !pausaPedida
                if (pausaPedida) ev(EV_PAUSADO)
                ev(EV_CARGA_FASE, 2, geracao = geracao) // warmed next-episode prepare
                p.prepare()
                Log.i(TAG, "[preparo] proximo adotado token=${preparada.token} pos=${preparada.posicaoFonteMs}")
            } else {
                // Transfer timeline/resume state before publishing READY. No new
                // media item, prepare or initial request on this adoption path.
                fracaoPendente = preparada.fracaoPendente
                if (fracaoPendente > 0) aplicarFracao(p)
                else confirmarRetomada(geracao, preparada.inicioAceito)
                ouv.onTracksChanged(p.currentTracks)
                ouv.onVideoSizeChanged(p.videoSize)
                ouv.onPlaybackStateChanged(p.playbackState)
                p.playWhenReady = !pausaPedida
                if (pausaPedida) ev(EV_PAUSADO)
                Log.i(TAG, "[preparo] adotado token=${preparada.token} estado=${p.playbackState}")
            }
            principal.postDelayed(tique(minha), TIQUE_MS)
        } catch (e: Exception) {
            preparada?.recursos?.let { if (recursosAtuais !== it) soltarRecursosPreparo(it) }
            // A partially attached player must not retain its route observer,
            // audio output, cache or renderer after a failed opening.
            if (recursosAtuais != null) liberar(preservarRetido = mesmaFonte)
            confirmarRetomada(geracao, false)
            Log.w(TAG, "abrir: ${e.javaClass.simpleName}")
            ev(EV_ERRO, -1, 0, geracao = geracao)
        }
    }

    // Sobe a sessao e solta tudo; qualquer callback pendente do player velho
    // morre na conferencia do numero.
    private fun liberar(preservarRetido: Boolean = false, guardarCache: Boolean = false) {
        // A stopped VOD may retain exactly one proven SimpleCache, but source
        // replacement, app destruction and prepared-next-player adoption may not.
        val pedidoNoFechamento = pedidos.get()
        if (!preservarRetido) limparRetido()
        buscaSerial++ // cancel any outstanding pre-seek PixelCopy continuation
        recuperacaoLive?.cancel()
        recuperacaoLive = null
        sessao++
        restoreAfr()
        afrSuspended = false
        principal.removeCallbacks(recriar)
        principal.removeCallbacks(estavelRun)
        recriaAgendada = false
        medidor.encerrar()
        val recursos = recursosAtuais
        recursos?.let {
            // Session was invalidated above. Do not send stale ownership events.
            it.routeObserver.stop()
            it.routeTx.cancel()
        }
        val cacheParaLiberar = cacheMidia
        cacheMidia = null
        recursos?.adotado = false
        recursos?.carga?.cancelada = true
        recursos?.liberado = true
        recursosAtuais = null
        val p = player
        player = null
        pronto = false
        somLiberado = false
        duracaoEnviada = -1
        videoW = 0; videoH = 0
        temJanela = false
        assinatura = ""
        audios = ArrayList(); capacidadesAudio = ArrayList(); legendas = ArrayList()
        decoderDv = false
        videoDecoderName = null
        videoOutputFailed = false
        baseLayerRendered = null
        ultHdr = ""; ultDv = -1; ultAtmos = -1
        avisouSemAudio = false
        val sv = superficie
        val ouv = ouvinteAtual; val ana = analiticoAtual
        ouvinteAtual = null; analiticoAtual = null
        cancelarDetach()
        cancelarStartupSeek()
        if (p != null) {
            // Stop audible playback on the main looper before async release:
            // an old decoder must never overlap the newly chosen source.
            try { p.playWhenReady = false } catch (e: Exception) {
                Log.w(TAG, "pause before async release: ${e.javaClass.simpleName}")
            }
            // Remove listeners before posting stop/release to the application looper.
            try { if (ouv != null) p.removeListener(ouv) } catch (e: Exception) { }
            try { if (ana != null) p.removeAnalyticsListener(ana) } catch (e: Exception) { }
            // Esconde sem destruir a superficie (destruir acorda o callback do Media3).
            try { sv?.layoutParams = FrameLayout.LayoutParams(1, 1) } catch (e: Exception) { }
            // A superficie velha fica com o player velho; a proxima abertura cria outra.
            superficie = null
            liberarEmFundo(p, sv) {
                recursos?.let { liberarFontePreparo(it) }
                cacheParaLiberar?.let { c ->
                    if (guardarCache && pedidoNoFechamento == pedidos.get() &&
                        activity != null && recursosAtuais == null) CacheMidia.guardar(c)
                    else c.liberar()
                }
            }
        } else cacheParaLiberar?.liberar()
        // P4-C: last Media3 loader/decoder has stopped before the 1500 ms
        // delayed cache cleanup starts; never release while readers run.
        // PixelCopy's visual warm frame remains separate and unchanged.
        tituloAberto = ""
        cacheEstado = CacheSessao.DESLIGADO
        cacheTiques = 0
        processador = null
        // Depois do release (o fio de reproducao ja parou): sem audio decodificado.
        tap.encerrar()
        try { nativeAudioEstado(AudioSyncTap.FMT_NENHUM) } catch (_: UnsatisfiedLinkError) {}
        superficie?.visibility = View.GONE
    }

    private fun novaSuperficie(act: Activity) {
        if (superficie != null) return
        val c = camada ?: return
        val sv = SurfaceView(act)
        sv.visibility = View.GONE
        // A superficie de video nunca pega o foco do controle: as teclas chegam
        // ao app pelo foco da SDLSurface (SDLSurface.onKey), e foco em outra
        // View deixa o processo vivo e a tela sem resposta (#318).
        sv.isFocusable = false
        sv.isFocusableInTouchMode = false
        sv.holder.addCallback(antesDoMedia3)
        observarJanela(sv)
        c.addView(sv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        superficie = sv
        // The newly created hardware layer must stay UNDER any snapshot
        // retained from this same source, including the existing warm cache.
        quadroView?.bringToFront()
        retidoView?.bringToFront()
    }

    // QUANTO O MEDIA3 SEGUROU O FIO PRINCIPAL ao perder a superficie (recriar,
    // ou o sistema ao esconder o app: tecla de ajustes da TV, Home). O callback
    // dele entra na lista entre estes dois (setVideoSurfaceView, em abrirMain).
    private var destruirIni = 0L
    private val antesDoMedia3 = object : SurfaceHolder.Callback {
        override fun surfaceCreated(h: SurfaceHolder) {
            superficie?.let { if (h === it.holder) recuperarJanela(it) }
            Log.i(TAG, "[dvtrace] created sessao=$sessao holder=${System.identityHashCode(h)} atual=${h === superficie?.holder} hdr=$ultHdr frame=${h.surfaceFrame}")
        }
        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, a: Int) {
            superficie?.let { if (h === it.holder) recuperarJanela(it) }
            Log.i(TAG, "[dvtrace] changed sessao=$sessao holder=${System.identityHashCode(h)} atual=${h === superficie?.holder} tamanho=${w}x$a formato=$f hdr=$ultHdr")
        }
        override fun surfaceDestroyed(h: SurfaceHolder) {
            superficie?.let {
                if (h === it.holder && player != null && atual(sessao)) {
                    recuperarJanela(it)
                    // Audio-only playback has no video-frame dependency.
                    if (quadroVisto || player?.currentTracks?.groups?.any { g ->
                            g.type == C.TRACK_TYPE_VIDEO
                        } == true) invalidarQuadroSuperficie()
                }
            }
            destruirIni = SystemClock.elapsedRealtime()
            Log.i(TAG, "[dvtrace] destroyed sessao=$sessao holder=${System.identityHashCode(h)} atual=${h === superficie?.holder} hdr=$ultHdr")
        }
    }
    private val depoisDoMedia3 = object : SurfaceHolder.Callback {
        override fun surfaceCreated(h: SurfaceHolder) {}
        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, a: Int) {}
        override fun surfaceDestroyed(h: SurfaceHolder) {
            Log.i(TAG, "[player] superficie destruida: Media3 segurou o fio principal por ${SystemClock.elapsedRealtime() - destruirIni} ms")
        }
    }

    // Media3 requires both calls on its application looper. Heavy renderer
    // teardown stays on its playback thread; release waits at most 200 ms.
    private fun liberarEmFundo(p: ExoPlayer, sv: SurfaceView?, depois: () -> Unit = {}) {
        liberacoesEmCurso.incrementAndGet()
        val corpo = Runnable {
            val ini = SystemClock.elapsedRealtime()
            try { p.stop() } catch (e: Exception) { Log.w(TAG, "stop: $e") }
            try { p.release() } catch (e: Exception) { Log.w(TAG, "release: $e") }
            try { depois() } catch (e: Exception) { Log.w(TAG, "release preload source: $e") }
            val ms = SystemClock.elapsedRealtime() - ini
            if (ms > 5000) Log.w(TAG, "[player] release anterior levou $ms ms (>5 s: HAL de audio preso?)")
            else Log.i(TAG, "[player] release anterior levou $ms ms")
            liberacoesEmCurso.decrementAndGet()
            principal.post {
                if (sv != null && sv !== superficie) (sv.parent as? FrameLayout)?.removeView(sv)
            }
        }
        Handler(p.applicationLooper).post(corpo)
    }

    // Tique de 250 ms: a posicao que o C le sem esperar ninguem.
    private fun tique(minha: Int): Runnable = object : Runnable {
        override fun run() {
            if (!atual(minha)) return
            val p = player ?: return
            recursosAtuais?.let { audioRouteTick(minha, it) }
            // Keep the signed live-window clock for stall detection. Only the
            // native UI position is clamped; a moving negative position is valid.
            val live = p.isCurrentMediaItemLive
            val validWindow = !p.currentTimeline.isEmpty && !p.isPlayingAd &&
                (p.playbackState == Player.STATE_READY || p.playbackState == Player.STATE_BUFFERING)
            val end = if (live || validWindow) p.bufferedPosition else C.TIME_UNSET
            try { nativePos(p.currentPosition, end, live,
                recursosAtuais?.carga?.allocator?.totalBytesAllocated ?: -1, geracaoNative, appliedSeekRevision)
            } catch (e: UnsatisfiedLinkError) { }
            if (++statsTiques >= 4) { statsTiques = 0; relatarStats(p) }
            if (cacheMidia != null && cacheEstado == CacheSessao.ATIVO && ++cacheTiques >= CACHE_RELATO_TIQUES) {
                cacheTiques = 0
                relatarCache(minha)
            }
            principal.postDelayed(this, TIQUE_MS)
        }
    }

    // Uma vez por segundo, do fio principal: leituras baratas, nada alocado
    // alem da string do host (que o JNI copia).
    private fun relatarStats(p: ExoPlayer) {
        val resources = recursosAtuais ?: return
        val e = resources.parallelStats
        val buf = (p.bufferedPosition - p.currentPosition).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        var quadros = -1L
        var perdidos = -1L
        p.videoDecoderCounters?.let { c ->
            c.ensureUpdated()
            quadros = (c.renderedOutputBufferCount + c.droppedBufferCount).toLong()
            perdidos = quadrosPerdidos
        }
        try {
            // Parallel requests measure their own physical socket transfers.
            // Otherwise use only HTTP transfer events (adaptive/serial/revalidation),
            // never Media3's estimated bitrate or cached reads.
            val serial = resources.contadorHttp.bytes()
            val bytesRede = if (e.usado) e.bytes.get() + serial.coerceAtLeast(0) else serial
            nativeStats(bytesRede, buf,
                bufferAlvoMs,
                if (e.usado) e.conexoes.get().coerceAtLeast(0) else -1,
                e.primeiroByteMs, perdidos, quadros, e.host,
                bufferMinMs, bufferBytes shr 20, if (p.isLoading) 1 else 0, geracaoNative)
        } catch (t: UnsatisfiedLinkError) { }
    }

    // --- janela ---------------------------------------------------------------

    // Retangulo em coordenadas de layout 1920x1080; aceita origem NEGATIVA e
    // tamanho maior que a tela (zoom): a camada recorta o excedente.
    // `encaixa`: janela LISA do nucleo — o quadro encaixa no retangulo com
    // tarja, como o plano da LG e o LetterBox da Samsung. Sem isso o trailer
    // em "Original" (Apple 2,4:1) saia esticado em 16:9. Recorte (encaixa =
    // false) ja vem com a proporcao certa e e aplicado exato.
    private var pedX = 0; private var pedY = 0; private var pedW = 0; private var pedH = 0
    private var pedEncaixa = true

    private fun definirJanela(x: Int, y: Int, w: Int, h: Int, encaixa: Boolean) {
        if (player == null) return   // como o Video.cs: o C repete depois do videoInfo
        pedX = x; pedY = y; pedW = w; pedH = h; pedEncaixa = encaixa
        temJanela = true
        calcularJanela()
        Log.i(TAG, "[dvtrace] janela sessao=$sessao holder=${System.identityHashCode(superficie?.holder)} pedido=$x,$y ${w}x$h encaixa=$encaixa calculada=$jx,$jy ${jw}x$jh hdr=$ultHdr semRecriar=$semRecriar")
    }

    private fun calcularJanela() {
        if (pedEncaixa && videoW > 0 && videoH > 0 && pedW > 0 && pedH > 0) {
            val esc = minOf(pedW.toFloat() / videoW, pedH.toFloat() / videoH)
            jw = (videoW * esc + 0.5f).toInt()
            jh = (videoH * esc + 0.5f).toInt()
            jx = pedX + (pedW - jw) / 2
            jy = pedY + (pedH - jh) / 2
        } else {
            jx = pedX; jy = pedY; jw = pedW; jh = pedH
        }
        aplicarJanela()
    }

    private fun larguraLogica(): Float {
        val c = camada
        return if (interfaceMobile && c != null && c.width > 0 && c.height > 0)
            TELA_H.toFloat() * c.width / minOf(c.width, c.height) else TELA_W.toFloat()
    }

    private fun alturaLogica(): Float {
        val c = camada
        return if (interfaceMobile && c != null && c.width > 0 && c.height > 0)
            TELA_H.toFloat() * c.height / minOf(c.width, c.height) else TELA_H.toFloat()
    }

    private fun reaplicarJanela() {
        if (!temJanela) aplicarEncaixe() else aplicarJanela()
    }

    // Sem janela pedida ainda: o quadro inteiro encaixado (letterbox) na tela.
    private fun aplicarEncaixe() {
        if (temJanela) return
        val largura = larguraLogica()
        val altura = alturaLogica()
        if (videoW > 0 && videoH > 0) {
            val esc = minOf(largura / videoW, altura / videoH)
            jw = (videoW * esc + 0.5f).toInt()
            jh = (videoH * esc + 0.5f).toInt()
            jx = ((largura - jw) / 2).toInt()
            jy = ((altura - jh) / 2).toInt()
        } else {
            jx = 0; jy = 0; jw = Math.round(largura); jh = Math.round(altura)
        }
        aplicarJanela()
    }

    private fun aplicarJanela() {
        val c = camada ?: return
        val sv = superficie ?: return
        var cw = c.width
        var ch = c.height
        if (cw < 1 || ch < 1) {
            val m = c.resources.displayMetrics
            cw = m.widthPixels; ch = m.heightPixels
        }
        val ex = cw.toFloat() / larguraLogica()
        val ey = ch.toFloat() / alturaLogica()
        // As BORDAS arredondam e o tamanho sai da diferenca: tela cheia cai
        // exatamente em 0,0,cw,ch, sem fresta.
        val x0 = Math.round(jx * ex)
        val y0 = Math.round(jy * ey)
        val x1 = Math.round((jx + jw) * ex)
        val y1 = Math.round((jy + jh) * ey)
        val largura = maxOf(1, x1 - x0)
        val altura = maxOf(1, y1 - y0)
        val gravidade = Gravity.TOP or Gravity.START
        val antes = sv.layoutParams as? FrameLayout.LayoutParams
        // Inspect the current SurfaceView, not a cached rectangle: resize or
        // replacing the surface must still apply its actual layout. Avoid both
        // allocation and requestLayout when repeated requests change nothing.
        if (antes != null && antes.width == largura && antes.height == altura &&
            antes.leftMargin == x0 && antes.topMargin == y0 &&
            antes.rightMargin == 0 && antes.bottomMargin == 0 && antes.gravity == gravidade) return
        val lp = FrameLayout.LayoutParams(largura, altura)
        lp.gravity = gravidade
        lp.leftMargin = x0
        lp.topMargin = y0
        sv.layoutParams = lp
        // A TCL PRENDE A GEOMETRIA DO PLANO DE VIDEO: aplica o primeiro tamanho
        // e posicao da superficie e ignora as mudancas seguintes (o dono: "entra
        // recortado e nao sai; entra no esticar e fica esticado"). Recriar a
        // Surface (GONE -> VISIBLE) faz o compositor montar camada nova com a
        // geometria nova; o ExoPlayer troca a saida do decoder sem recarregar.
        //
        // SO EM SDR ATE A SUPERFICIE ESTAR ESTAVEL. No HDR/DV cada recriacao
        // aqui obrigava o Media3 a soltar a saida do decoder e o C2MtkVdecTv a
        // religar o DispLink: o aspecto salvo aplicado apos o primeiro quadro,
        // e cada troca manual de modo, mudava o tamanho e recriava (~12 vezes
        // em 30 s; TCL 2160p DV, 11/10), com o fio principal parado no glClear
        // ("renderer soltou em 7057 ms", pior quadro 6612.7 ms). Antes de
        // estavel a recriacao fica so nas TRANSICOES DE MODO (hdrNaSuperficie);
        // DEPOIS dela, uma recriacao por mudanca com a espera longa, senao o
        // plano da TCL fica preso na geometria de abertura e a troca Crop ->
        // Ajuste nao aparece (revisao 14/10). DV nativo dispensa a recriacao
        // da ABERTURA (ver recriarSuperficie); depois de estavel, a troca de
        // modo e um pedido do dono e o veto nao se aplica — o plano da TCL so
        // acompanha a geometria nova com a camada nascendo de novo.
        if (ultHdr.isEmpty() || ultHdr == "none") recriarSuperficie(RECRIA_ESPERA_MS)
        else if (estavelEmitido) recriarSuperficie(RECRIA_ESPERA_HDR_MS)
    }

    // Recriar a Surface custa um quadro preto e, em rajada (aspecto apertado
    // varias vezes), travava: as trocas JUNTAM numa so, RECRIA_ESPERA_MS depois
    // da ultima. A mesma recriacao liga o HDR da TCL (ver onRenderedFirstFrame).
    private const val RECRIA_ESPERA_MS = 350L
    // HDR/DV: recriar e caro (DispLink religa), so depois de a superficie parar.
    private const val RECRIA_ESPERA_HDR_MS = 600L
    private var recriando = false
    private var recriaAgendada = false
    private var recuperouSuperficie = false
    private var superficiePos = 0L
    private var quadroVisto = false
    // Media3's analytics event includes the actual output and elapsed-realtime render time.
    // An earlier surface incarnation must not authorize the next video/audio handoff.
    private var superficieInvalidadaEm = 0L
    private val startupSeekGate = RecriaHdr.StartupSeekGate()
    private var startupSeekTimeout: Runnable? = null

    private fun cancelarStartupSeek() {
        startupSeekTimeout?.let { principal.removeCallbacks(it) }
        startupSeekTimeout = null
        startupSeekGate.cancel()
    }

    private fun buscarComHandoff(p: ExoPlayer, alvo: Long) {
        if (estavelEmitido || !p.isCurrentMediaItemSeekable) {
            p.seekTo(alvo)
            return
        }
        // Media3 can adjust a seek to the current position and elide its
        // renderer reset. Hold the startup gate until its playback thread
        // processes the command; only an actual reset invalidates a frame.
        val origem = sessao
        val ticket = startupSeekGate.begin()
        startupSeekTimeout?.let { principal.removeCallbacks(it) }
        principal.removeCallbacks(estavelRun)
        val timeout = Runnable {
            if (atual(origem) && player === p && startupSeekGate.finish(ticket)) {
                startupSeekTimeout = null
                falhaVideo(origem, HdrPolicyP5.FAIL_SURFACE, 1003, geracaoNative)
            }
        }
        startupSeekTimeout = timeout
        principal.postDelayed(timeout, 10000L)
        try {
            p.seekTo(alvo)
            p.createMessage { _, _ ->
                principal.post {
                    if (atual(origem) && player === p && startupSeekGate.finish(ticket)) {
                        principal.removeCallbacks(timeout)
                        startupSeekTimeout = null
                        avaliarEstavel()
                    }
                }
            }.send()
        } catch (e: Exception) {
            if (startupSeekGate.finish(ticket)) {
                principal.removeCallbacks(timeout)
                startupSeekTimeout = null
                avaliarEstavel()
            }
            throw e
        }
    }

    private fun invalidarQuadroSuperficie(at: Long = SystemClock.elapsedRealtime()) {
        superficieInvalidadaEm = maxOf(superficieInvalidadaEm, at)
        baseLayerRendered = null
        quadroVisto = false
        estavelEmitido = false
        somLiberado = false
        aplicarVolume()
        principal.removeCallbacks(estavelRun)
        ev(EV_SUPERFICIE_INVALIDADA)
    }
    // SOLTAR A SAIDA SEM ESPERAR (2.0.3). Destruir a Surface com o decoder
    // ainda ligado nela faz o Media3 esperar o fio de reproducao DENTRO do
    // surfaceDestroyed, no fio principal (setVideoOutputInternal: ate 2 s, e
    // no estouro derruba o player com timeout). Na TCL do dono o `recriar`
    // segurou o fio principal por 7470 ms (logcat 08/10 00:49:48, "Slow
    // dispatch"): controle morto, e tecla nesse meio = ANR. O pedido vai como
    // mensagem ao fio de reproducao e `depois` so roda, de volta no fio
    // principal, com o renderer ja sem a superficie: a espera do Media3 que
    // vem em seguida encontra o trabalho feito. Player liberado no meio = a
    // mensagem e descartada e `depois` nao roda (a superficie foi com ele).
    private var soltando: ExoPlayer? = null
    private var detachTicket: RecriaHdr.DetachTicket? = null
    private var detachTimeout: Runnable? = null
    private const val DETACH_ACK_MS = 2500L

    private fun cancelarDetach() {
        detachTicket?.cancel()
        detachTicket = null
        detachTimeout?.let { principal.removeCallbacks(it) }
        detachTimeout = null
        soltando = null
        recriando = false
    }

    private fun detachDono(ticket: RecriaHdr.DetachTicket, origem: Int,
                           p: ExoPlayer, sv: SurfaceView): Boolean =
        detachTicket === ticket && atual(origem) && !videoOutputFailed &&
            player === p && superficie === sv

    private fun terminarDetach(ticket: RecriaHdr.DetachTicket, origem: Int,
                               p: ExoPlayer, sv: SurfaceView) {
        if (!detachDono(ticket, origem, p, sv)) return
        cancelarDetach()
        applyAfr(origem, p.videoFormat)
        avaliarEstavel()
    }

    private fun soltarSaida(p: ExoPlayer, sv: SurfaceView, origem: Int,
                            ticket: RecriaHdr.DetachTicket) {
        if (!detachDono(ticket, origem, p, sv)) return
        if (!ticket.send()) {
            // Paused before the renderer was touched: keep the existing plane.
            terminarDetach(ticket, origem, p, sv)
            return
        }
        invalidarQuadroSuperficie()
        val ini = SystemClock.elapsedRealtime()
        val timeout = Runnable {
            if (detachDono(ticket, origem, p, sv) && ticket.expire()) {
                // Do not destroy an output when detach has not been acknowledged.
                // A late renderer message cannot reopen the failed transaction.
                falhaVideo(origem, HdrPolicyP5.FAIL_SURFACE, 1003, geracaoNative)
            }
        }
        detachTimeout = timeout
        principal.postDelayed(timeout, DETACH_ACK_MS)
        try {
            for (i in 0 until p.rendererCount) {
                if (p.getRendererType(i) != C.TRACK_TYPE_VIDEO) continue
                p.createMessage(p.getRenderer(i)).setType(Renderer.MSG_SET_VIDEO_OUTPUT).setPayload(null).send()
            }
            p.createMessage { _, _ ->
                Log.i(TAG, "[player] superficie: renderer soltou em ${SystemClock.elapsedRealtime() - ini} ms")
                principal.post {
                    if (!detachDono(ticket, origem, p, sv)) return@post
                    val action = ticket.acknowledge()
                    if (action == RecriaHdr.DetachAction.IGNORE) return@post
                    principal.removeCallbacks(timeout)
                    detachTimeout = null
                    invalidarQuadroSuperficie()
                    if (action == RecriaHdr.DetachAction.RETAIN || afrSuspended) {
                        // Media3 1.8 re-sends the output even for the same holder.
                        // Never force GONE/VISIBLE as the Activity is backgrounded.
                        try {
                            ticket.attaching()
                            p.setVideoSurfaceView(sv)
                            terminarDetach(ticket, origem, p, sv)
                        } catch (e: Exception) {
                            Log.w(TAG, "reattach retained surface: $e")
                            falhaVideo(origem, HdrPolicyP5.FAIL_SURFACE, 1003, geracaoNative)
                        }
                        return@post
                    }
                    sv.visibility = View.GONE
                    principal.post visible@{
                        if (!detachDono(ticket, origem, p, sv)) return@visible
                        ticket.attaching()
                        sv.visibility = View.VISIBLE
                        principal.post {
                            terminarDetach(ticket, origem, p, sv)
                        }
                        if (!afrSuspended) (activity as? NuvioActivity)?.devolverFoco()
                    }
                }
            }.send()
        } catch (e: Exception) {
            Log.w(TAG, "soltarSaida: $e")
            falhaVideo(origem, HdrPolicyP5.FAIL_SURFACE, 1003, geracaoNative)
        }
    }

    private val recriar = Runnable {
        recriaAgendada = false
        if (!atual(sessao) || videoOutputFailed) return@Runnable
        if (afrSuspended || afrGate != null || !recriaTcl || semRecriar ||
            (decoderDv && !estavelEmitido) || recriando) {
            avaliarEstavel()
            return@Runnable
        }
        val p = player
        val sv = superficie
        if (p == null || sv == null || sv.visibility != View.VISIBLE ||
            sv.holder.surface?.isValid != true) {
            avaliarEstavel()
            return@Runnable
        }
        val ticket = RecriaHdr.DetachTicket()
        detachTicket = ticket
        soltando = p
        recriando = true
        superficiePos = p.currentPosition
        val pedido = pedidoAtivo
        val origem = sessao
        capturarRetido(p, sv, pedido) {
            if (!detachDono(ticket, origem, p, sv)) return@capturarRetido
            soltarSaida(p, sv, origem, ticket)
        }
    }
    // Familias que nao recriam a superficie no HDR (RecriaHdr.kt): MStar e
    // Amlogic. A recriacao existe por causa da TCL; nelas so custa.
    private val semRecriar: Boolean by lazy {
        try {
            val nomes = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { !it.isEncoder }.map { it.name }
            val f = RecriaHdr.dispensa(nomes)
            Log.i(TAG, "[player] recriar superficie no HDR: ${f?.let { "nao ($it)" } ?: "sim"}")
            f != null
        } catch (e: Throwable) { false }
    }
    // Keep the existing MStar/Amlogic release policy separate from the
    // proactive panel workaround. Unknown hardware must retain its Surface.
    private val recriaTcl: Boolean by lazy {
        try {
            val nomes = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { !it.isEncoder }.map { it.name }
            RecriaHdr.needsTclWorkaround(android.os.Build.MANUFACTURER,
                android.os.Build.BRAND, nomes)
        } catch (e: Exception) { false }
    }
    private fun recriarSuperficie(atrasoMs: Long) {
        Log.i(TAG, "[dvtrace] pedido sessao=$sessao holder=${System.identityHashCode(superficie?.holder)} atrasoMs=$atrasoMs hdr=$ultHdr quadro=$quadroVisto semRecriar=$semRecriar soltando=${soltando != null}")
        // DV nativo so dispensa a recriacao da ABERTURA (a que nasce antes de
        // a superficie estabilizar): depois de estavel, a troca de aspecto e o
        // gesto que o dono fazia na mao para o Dolby ligar — vetar aqui deixava
        // o plano preso na geometria de abertura e nenhum modo mudava (TCL,
        // 14/10). O veto do HDR sozinho ja cobre a transicao (hdrNaSuperficie).
        if (videoOutputFailed || afrSuspended || afrGate != null || recriando || !recriaTcl || semRecriar || (decoderDv && !estavelEmitido)) return
        principal.removeCallbacks(estavelRun)
        principal.removeCallbacks(recriar)
        recriaAgendada = true
        principal.postDelayed(recriar, atrasoMs)
    }
    // A TCL so liga o modo HDR do painel quando a Surface nasce com o decoder
    // ja em HDR: na abertura ela nasceu antes (SDR) e o HDR so aparecia depois
    // de trocar o aspecto (dono, 30/09). Uma recriacao no primeiro quadro HDR.
    // Native DV already configures the panel. Destruction forces TCL DispLink
    // to reconnect (15 s in the captured log). Other HDR gets one recreation.
    private var hdrRecriadoPara = ""
    private var estavelEmitido = false
    private val estavelRun = Runnable {
        if (atual(sessao) && !afrSuspended && afrGate == null && !startupSeekGate.waiting() && !videoOutputFailed && player != null && quadroVisto && !estavelEmitido && !recriando && !recriaAgendada &&
            superficie?.holder?.surface?.isValid == true) {
            estavelEmitido = true
            Log.i(TAG, "[aspect] superficie estavel (hdr=$ultHdr) sessao=$sessao soltando=${soltando != null} holder=${System.identityHashCode(superficie?.holder)}")
            ev(EV_SUPERFICIE_ESTAVEL)
            esconderQuadro() // retain the previous frame until the new hardware plane is safe
            limparRetido()
            liberarSom() // audio-only is separately confirmed by selected Media3 tracks
        }
    }
    private fun avaliarEstavel() {
        if (videoOutputFailed || afrSuspended || afrGate != null || startupSeekGate.waiting() || !quadroVisto || estavelEmitido) return
        principal.removeCallbacks(estavelRun)
        if (recriando || recriaAgendada) return
        principal.postDelayed(estavelRun, 150L)
    }
    // Evidencia para "a TV nao acompanha o fps do filme": fps do video, modos que o
    // painel oferece e o ativo. TCL do dono (06/10): so existe 3840x2160@60, entao
    // nao ha modo para trocar; o Media3 ja vota Surface.setFrameRate sozinho.
    private fun logTaxaDeQuadros() {
        try {
            val fps = player?.videoFormat?.frameRate ?: -1f
            val d = activity?.windowManager?.defaultDisplay ?: return
            val modos = d.supportedModes.joinToString(",") { "${it.modeId}:${it.physicalWidth}x${it.physicalHeight}@${it.refreshRate}" }
            Log.i(TAG, "[quadro] video=${fps}fps tela=${d.mode.modeId}@${d.refreshRate} modos=[$modos]")
            val a = player?.audioFormat
            Log.i(TAG, "[audio] ${a?.sampleMimeType} ${a?.channelCount}ch ${a?.sampleRate}Hz atraso=${atrasoMs}ms")
        } catch (e: Exception) { Log.w(TAG, "logTaxaDeQuadros: $e") }
    }
    private fun hdrNaSuperficie() {
        // Native DV handles the panel transition without destroying its output.
        if (afrSuspended || afrGate != null || recriando || !recriaTcl || semRecriar || decoderDv) return
        if (!quadroVisto || ultHdr.isEmpty() || ultHdr == "none" || ultHdr == hdrRecriadoPara) return
        hdrRecriadoPara = ultHdr
        Log.i(TAG, "HDR ($ultHdr): recria a superficie para a TV ligar o modo HDR")
        recriarSuperficie(200)
    }


    // --- Q49 audio-route ownership; main looper, visible session only --------
    private var routeForeground = true

    private fun audioDsp(r: RecursosPlayer?) = amplificacaoDb != 0 || baixoDb != 0 ||
        centroDb != 0 || noiteNivel != 0 || r?.velocidadePcm == true

    private fun selectedAudio(p: ExoPlayer?): Pair<Tracks.Group, Int>? {
        p ?: return null
        val format = p.audioFormat
        var first: Pair<Tracks.Group, Int>? = null
        for (g in p.currentTracks.groups) if (g.type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until g.length) if (g.isTrackSelected(i)) {
                if (first == null) first = g to i
                if (format != null && format == g.getTrackFormat(i)) return g to i
            }
        }
        return first
    }

    private fun publishRouteCaps(caps: List<AudioOutputProbeP5A.Observation>) {
        if (!atual(sessao)) return
        for ((i, cap) in caps.withIndex()) {
            val d = cap.result
            try {
                nativeAudioRouteCapacidade(geracaoNative, i, cap.sampleRate, cap.bitDepth, cap.sourceBitrate,
                    if (d.decoder) 1 else 0, if (d.pcmCompatible) 1 else 0,
                    if (d.encodedPassthrough) 1 else 0, if (d.outputFaithfulCandidate) 1 else 0,
                    if (d.unknown) 1 else 0)
            } catch (_: UnsatisfiedLinkError) { }
        }
    }

    private fun refreshRouteCaps(r: RecursosPlayer, route: android.media.AudioDeviceInfo?) {
        // Keep native track ordinals and subtitle selection intact. A route
        // update is not a new list of tracks, even if renderer support changed.
        if (audios.isEmpty()) { publicarFaixas(r.p.currentTracks); return }
        capacidadesAudio = ArrayList(audios.map { (g, i) ->
            AudioOutputProbeP5A.inspect(activity, g.getTrackFormat(i), g.isTrackSupported(i),
                audioDsp(r), route, true)
        })
        publishRouteCaps(capacidadesAudio)
    }

    private fun routeOwner(r: RecursosPlayer, active: Boolean) {
        if (r.routeNativeOwned == active) return
        r.routeNativeOwned = active
        if (recursosAtuais === r && atual(sessao))
            ev(EV_AUDIO_ROUTE_RECOVERY, if (active) 1 else 0)
    }

    private fun cancelAudioRoute(r: RecursosPlayer, restore: Boolean, recheck: Boolean = false) {
        val pending = recheck && (r.routeChanged || r.routeTx.active || r.routeDebounce.pending || r.routeFatal)
        r.routeTx.cancel()
        r.routePhase = 0
        r.routeTarget = null
        r.routeFatal = false
        r.routeChanged = pending
        r.routeAwaitSince = 0L
        r.routeAwaitingPlay = false
        if (restore && r.routeDisabled && recursosAtuais === r && atual(sessao)) {
            try {
                r.p.trackSelectionParameters = r.p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false).build()
            } catch (_: RuntimeException) { }
        }
        r.routeDisabled = false
        routeOwner(r, false)
        r.routeMuted = pending
        if (recheck) r.routeCheck = true
        if (recursosAtuais === r) aplicarVolume()
    }

    private fun failAudioRoute(minha: Int, r: RecursosPlayer, reason: String) {
        if (!atual(minha) || recursosAtuais !== r || r.routeFailed || videoOutputFailed) return
        r.routeFailed = true
        cancelAudioRoute(r, restore = false)
        r.routeMuted = true
        aplicarVolume()
        try { r.p.pause() } catch (_: RuntimeException) {}
        relatarAudio(minha, "Q49 terminal: " + reason)
        ev(EV_AUDIO_SAIDA_INCOMPATIVEL)
    }

    private fun pcmOutput(enc: Int) = enc == android.media.AudioFormat.ENCODING_PCM_16BIT ||
        enc == android.media.AudioFormat.ENCODING_PCM_8BIT ||
        enc == android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED ||
        enc == android.media.AudioFormat.ENCODING_PCM_32BIT ||
        enc == android.media.AudioFormat.ENCODING_PCM_FLOAT

    private fun faithfulOutput(fmt: Format, sink: AudioSink.AudioTrackConfig,
                               cap: AudioOutputProbeP5A.Observation): Boolean =
        AudioRouteP5D.validate(pcmOutput(sink.encoding), cap.pcmFaithful, cap.result.encodedPassthrough,
            fmt.sampleRate, fmt.channelCount, cap.bitDepth, sink.sampleRate,
            Integer.bitCount(sink.channelConfig), AudioOutputProbeP5A.bitDepth(sink.encoding),
            sink.encoding == AudioOutputProbeP5A.encoding(fmt.sampleMimeType.orEmpty()))

    private fun routeBlocked(r: RecursosPlayer) = recriando || recriaAgendada || afrGate != null ||
        startupSeekGate.waiting() || r.runtimeVideoRecovery?.completed == false ||
        (retentou && !estavelEmitido) || recuperacaoLive?.recovering() == true

    private fun routeSelection(p: ExoPlayer, target: Pair<Tracks.Group, Int>, disabled: Boolean) {
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(target.first.mediaTrackGroup, target.second))
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, disabled).build()
    }

    private fun startAudioRoute(minha: Int, r: RecursosPlayer, target: Pair<Tracks.Group, Int>,
                                forcePcm: Boolean, fatal: Boolean, now: Long) {
        if (!atual(minha) || recursosAtuais !== r || r.routeTx.active) return
        val p = r.p
        val ticket = r.routeTx.begin(now)
        r.routeTarget = target
        r.routePcm = forcePcm
        r.routeMuted = true
        r.routeOutputBefore = r.routeObserver.output?.serial ?: 0L
        routeOwner(r, true)
        aplicarVolume()
        try {
            if (fatal) {
                // Media3 has already stopped on a fatal audio renderer error.
                // One same-player prepare; never stop, seek, change item or URL.
                if (!r.routeTx.allowPrepare(ticket)) { failAudioRoute(minha, r, "retry exhausted"); return }
                r.routePhase = 2
                r.routeFatal = false
                routeSelection(p, target, false)
                if (r.routeHasVideo || p.videoFormat != null)
                    invalidarQuadroSuperficie()
                p.prepare()
                return
            }
            reselectAudioRoute(minha, r, target, ticket)
        } catch (_: RuntimeException) { failAudioRoute(minha, r, "audio renderer recovery failed") }
    }

    private fun reselectAudioRoute(minha: Int, r: RecursosPlayer, target: Pair<Tracks.Group, Int>,
                                   ticket: Long) {
        if (!r.routeTx.allowReselect(ticket)) { failAudioRoute(minha, r, "audio reselect exhausted"); return }
        val p = r.p
        r.routeOutputBefore = r.routeObserver.output?.serial ?: 0L
        // In ordinary (non-tunneled) playback only the audio renderer needs
        // changing. If tunneling was actually negotiated, changing its paired
        // audio renderer necessarily changes the video renderer configuration;
        // retain the SurfaceView and re-arm the accepted first-frame gate.
        if (r.routeObserver.output?.config?.tunneling == true && r.routeHasVideo)
            invalidarQuadroSuperficie()
        // Playback-looper barrier prevents disable/enable conflation.
        // No new source, seek, pause snapshot or automatic audio focus.
        r.routePhase = 1
        r.routeDisabled = true
        routeSelection(p, target, true)
        p.createMessage { _, _ ->
            principal.post {
                if (!atual(minha) || recursosAtuais !== r || player !== p ||
                    !r.routeTx.owns(ticket) || r.routePhase != 1 || !r.routeDisabled ||
                    r.routeFailed || videoOutputFailed) return@post
                try {
                    r.routeDisabled = false
                    r.routePhase = 2
                    routeSelection(p, target, false)
                    audioRouteTick(minha, r)
                } catch (_: RuntimeException) { failAudioRoute(minha, r, "audio renderer re-enable failed") }
            }
        }.send()
    }

    private fun audioRouteTick(minha: Int, r: RecursosPlayer) {
        if (!atual(minha) || recursosAtuais !== r || player !== r.p || !routeForeground ||
            videoOutputFailed || r.routeFailed) return
        val p = r.p
        if (p.playbackState == Player.STATE_ENDED) { cancelAudioRoute(r, restore = false); return }
        val now = SystemClock.elapsedRealtime()
        val selected = selectedAudio(p)
        if (selected != null && !r.routeTx.active && !r.routeFatal) r.routeSelected = selected
        val target = r.routeTarget ?: selected ?: r.routeSelected
        val route = r.routeObserver.route()
        val observed = r.routeDebounce.observe(r.routeObserver.key(route), now)
        if (routeBlocked(r)) {
            if (r.routeTx.active) cancelAudioRoute(r, restore = true, recheck = true)
            if (observed == AudioRouteP5D.Debounce.Result.CHANGED || observed == AudioRouteP5D.Debounce.Result.UNSTABLE)
                r.routeChanged = true
            if (observed != AudioRouteP5D.Debounce.Result.NONE || r.routeChanged) {
                r.routeCheck = true
                r.routeMuted = true
                aplicarVolume()
            }
            routeOwner(r, false)
            return
        }
        if (r.routeTx.expired(now)) { failAudioRoute(minha, r, "audio route recovery timeout"); return }
        if (observed == AudioRouteP5D.Debounce.Result.WAIT) {
            if (target != null) {
                r.routeMuted = true
                aplicarVolume()
                if (!routeBlocked(r)) routeOwner(r, true)
            }
            return
        }
        if (observed == AudioRouteP5D.Debounce.Result.UNSTABLE) {
            if (target != null && !routeBlocked(r)) failAudioRoute(minha, r, "route did not stabilize")
            return
        }
        if (observed == AudioRouteP5D.Debounce.Result.CHANGED) {
            r.routeChanged = true
            r.routeCheck = true
        }
        if (target == null) return
        if (r.routeAwaitingPlay) {
            if (!p.playWhenReady) { routeOwner(r, false); return }
            r.routeAwaitingPlay = false
            r.routeChanged = true
        }
        if (!r.routeCheck && !r.routeChanged && !r.routeTx.active && !r.routeFatal) {
            if (r.routeNativeOwned || r.routeMuted) cancelAudioRoute(r, restore = false)
            return
        }
        val fmt = target.first.getTrackFormat(target.second)
        val cap = AudioOutputProbeP5A.inspect(activity, fmt, true, audioDsp(r), route, true)
        val decision = cap.result
        val output = r.routeObserver.output?.takeIf {
            try { it.serial == r.routeValidatedSerial && it.track.state == android.media.AudioTrack.STATE_INITIALIZED }
            catch (_: RuntimeException) { false }
        }
        val input = r.routeInput ?: p.audioFormat
        val formatMatches = input != null && input.sampleMimeType == fmt.sampleMimeType &&
            input.channelCount == fmt.channelCount && input.sampleRate == fmt.sampleRate
        val compatible = output != null && formatMatches && faithfulOutput(fmt, output.config, cap)
        if (r.routeTx.active) {
            val ticket = r.routeTx.ticket
            if (r.routeFatal && r.routeTx.allowPrepare(ticket) && decision.outputFaithfulCandidate) {
                // Fatal during the audio-only transaction: same player, one
                // prepare, no source replacement. Keep the existing deadline.
                r.routeFatal = false
                r.routePhase = 2
                r.routeDisabled = false
                r.routeOutputBefore = output?.serial ?: r.routeOutputBefore
                try {
                    routeSelection(p, target, false)
                    if (r.routeHasVideo || p.videoFormat != null) invalidarQuadroSuperficie()
                    p.prepare()
                } catch (_: RuntimeException) { failAudioRoute(minha, r, "same-player prepare failed") }
                return
            }
            if (r.routeFatal) { failAudioRoute(minha, r, "audio renderer failed after bounded retry"); return }
            if (observed == AudioRouteP5D.Debounce.Result.CHANGED) {
                // A newly stable device may require a different output mode
                // than the device for which this transaction began.
                val next = AudioRouteP5D.plan(decision.outputFaithfulCandidate, cap.pcmFaithful,
                    decision.encodedPassthrough, compatible, output?.let { pcmOutput(it.config.encoding) } == true)
                if (next == AudioRouteP5D.Action.ERROR) {
                    failAudioRoute(minha, r, "new route has no faithful path"); return
                }
                if (next == AudioRouteP5D.Action.RESELECT_PCM || next == AudioRouteP5D.Action.RESELECT_ENCODED) {
                    val pcm = next == AudioRouteP5D.Action.RESELECT_PCM
                    if (pcm != r.routePcm) {
                        r.routePcm = pcm
                        if (r.routePhase == 2) {
                            try { reselectAudioRoute(minha, r, target, ticket) }
                            catch (_: RuntimeException) { failAudioRoute(minha, r, "new route reselection failed") }
                            return
                        }
                        // Phase 1 is still disabled: the pending ack can use
                        // the new mode without a redundant renderer reset.
                    }
                }
            }
            val selectedSame = selected?.let {
                it.first.mediaTrackGroup == target.first.mediaTrackGroup && it.second == target.second
            } == true
            if (r.routePhase == 2 && compatible && selectedSame &&
                output!!.serial > r.routeOutputBefore && !r.routeDebounce.pending) {
                cancelAudioRoute(r, restore = false)
                r.routeCheck = false
                refreshRouteCaps(r, route)
                relatarAudio(minha, "Q49 verified replacement AudioTrack route=" + route?.type)
                return
            }
            // Paused renderers may not create a new AudioTrack until Play.
            // Finish ownership without auto-resuming; validate before unmute.
            if (r.routePhase == 2 && selectedSame && !p.playWhenReady && decision.outputFaithfulCandidate) {
                cancelAudioRoute(r, restore = false)
                r.routeMuted = true
                r.routeCheck = true
                r.routeChanged = true
                r.routeAwaitingPlay = true
                aplicarVolume()
                return
            }
            if (r.routeTx.expired(now)) failAudioRoute(minha, r, "audio route recovery timeout")
            return
        }
        if (output == null && !r.routeFatal) {
            if (r.routeChanged) {
                r.routeMuted = true
                aplicarVolume()
                if (p.playWhenReady) {
                    routeOwner(r, true)
                    if (r.routeAwaitSince == 0L) r.routeAwaitSince = now
                    if (now - r.routeAwaitSince >= AudioRouteP5D.RECOVERY_LIMIT_MS)
                        failAudioRoute(minha, r, "no new AudioTrack")
                } else { routeOwner(r, false); r.routeAwaitSince = 0L }
            }
            return
        }
        if (!r.routeChanged && !r.routeFatal) {
            if (!formatMatches) return
            // Initial and ordinary audio-format validation remains Q36; a
            // stable compatible route is not an excuse to restart playback.
            if (!compatible) failAudioRoute(minha, r, "incompatible AudioTrack configuration")
            else {
                r.routeCheck = false
                r.routeMuted = false
                routeOwner(r, false)
                aplicarVolume()
            }
            return
        }
        val action = AudioRouteP5D.plan(decision.outputFaithfulCandidate, cap.pcmFaithful,
            decision.encodedPassthrough, compatible && !r.routeFatal, output?.let { pcmOutput(it.config.encoding) } == true)
        when (action) {
            AudioRouteP5D.Action.ERROR -> failAudioRoute(minha, r, "no faithful path on current route")
            AudioRouteP5D.Action.KEEP -> {
                cancelAudioRoute(r, restore = false)
                r.routeCheck = false
                refreshRouteCaps(r, route)
                relatarAudio(minha, "Q49 retained compatible AudioTrack route=" + route?.type)
            }
            else -> startAudioRoute(minha, r, target, action == AudioRouteP5D.Action.RESELECT_PCM, r.routeFatal, now)
        }
    }

    // --- escolha de faixa ----------------------------------------------------

    private fun escolherMain(tipo: Int, idx: Int) {
        if (tipo == 2) { atrasoMs = idx; return }
        if (tipo == 3) { tap.ligado = idx != 0; return }   // F06: escuta de PCM liga/desliga
        val p = player ?: return
        try {
            if (tipo == 0) recursosAtuais?.let {
                cancelAudioRoute(it, restore = true)
                it.routeCheck = true
                it.routeMuted = true
                aplicarVolume()
            }
            val par = p.trackSelectionParameters.buildUpon()
            if (tipo == 0) {
                val (g, i) = audios.getOrNull(idx) ?: return
                val r = recursosAtuais
                val cap = if (r != null) AudioOutputProbeP5A.inspect(activity, g.getTrackFormat(i),
                    g.isTrackSupported(i), audioDsp(r), r.routeObserver.route(), true) else capacidadesAudio.getOrNull(idx)
                if (cap?.result?.outputFaithfulCandidate != true) {
                    relatarAudio(sessao, "seleção de áudio recusada: capacidade de saída não comprovada")
                    ev(EV_AUDIO_SAIDA_INCOMPATIVEL, 0, 0, geracaoNative)
                    return
                }
                relatarAudio(sessao, "escolha indice=$idx grupoIndice=$i mime=${g.getTrackFormat(i).sampleMimeType}")
                recursosAtuais?.routeSelected = g to i
                par.setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
            } else if (tipo == 1) {
                if (idx < 0) {
                    par.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).clearOverridesOfType(C.TRACK_TYPE_TEXT)
                } else {
                    val (g, i) = legendas.getOrNull(idx) ?: return
                    par.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                }
            } else return
            p.trackSelectionParameters = par.build()
        } catch (e: Exception) { Log.w(TAG, "escolher $tipo/$idx: $e") }
    }

    // --- eventos do player ----------------------------------------------------

    private fun ouvinte(minha: Int, geracaoDaAbertura: Int) = object : Player.Listener {
        // Capture the native generation when the listener is created; global
        // geracaoNative may already belong to a replacement by callback time.
        private fun ev(tipo: Int, a: Int = 0, b: Int = 0) =
            NvPlayer.ev(tipo, a, b, geracaoDaAbertura)
        private var liveSegmentMs = -1
        private val live = LiveWindowRecovery(
            schedule = { tarefa, atraso -> principal.postDelayed(tarefa, atraso) },
            unschedule = { tarefa -> principal.removeCallbacks(tarefa) },
            active = { atual(minha) },
            status = { prazoMs ->
                if (atual(minha)) ev(EV_LIVE_RECOVERY, prazoMs.toInt(), geracaoDaAbertura)
            },
        )
        init { recuperacaoLive = live }
        override fun onPlaybackStateChanged(state: Int) {
            if (!atual(minha) || videoOutputFailed) return
            val p = player ?: return
            Log.i(TAG, "[player] onPlaybackStateChanged state=$state renderer=audio(${p.audioFormat?.sampleMimeType}),video(${p.videoFormat?.sampleMimeType}) loadMs=${SystemClock.elapsedRealtime() - abriuEm}")
            when (state) {
                Player.STATE_BUFFERING -> ev(EV_BUFFER, 0)
                Player.STATE_READY -> {
                    val groups = p.currentTracks.groups
                    val selectedVideo = groups.any { g ->
                        g.type == C.TRACK_TYPE_VIDEO && (0 until g.length).any { i -> g.isTrackSelected(i) }
                    }
                    if (recursosAtuais?.runtimeVideoRecovery?.completed == false && !selectedVideo) {
                        falhaVideo(minha, HdrPolicyP5.FAIL_DECODER, 0, geracaoDaAbertura)
                        return
                    }
                    if (!pronto) {
                        /* Audio-only is a verified track outcome, not a 3s
                         * approximation of a missing video first frame. */
                        val selectedAudio = groups.any { g ->
                            g.type == C.TRACK_TYPE_AUDIO && (0 until g.length).any { i -> g.isTrackSelected(i) }
                        }
                        if (selectedAudio && groups.none { it.type == C.TRACK_TYPE_VIDEO }) {
                            ev(EV_SO_AUDIO, 1)
                            esconderQuadro()
                            liberarSom() // no video plane exists to synchronize with
                        }
                    }
                    ev(EV_CARGA_FASE, 3) // decoded tracks ready; awaiting rendered video frame
                    if (live.ready()) Log.i(TAG, "[live] janela recuperada na mesma sessao=$minha")
                    ev(EV_BUFFER, 100)
                    Log.i(TAG, "[speed] ready parameters=${p.playbackParameters.speed} pcm=${recursosAtuais?.velocidadePcm}; awaiting native clock measurement")
                    relatarAudio(minha, "pronto volume=${p.volume} playWhenReady=${p.playWhenReady} suppression=${p.playbackSuppressionReason}")
                    if (!pronto) {
                        // Antes do EV_PRONTO: o C le o ack ao ver o "pronto".
                        if (fracaoPendente > 0) {
                            fracaoPendente = 0
                            Log.w(TAG, "[player] retomada: sem duracao antes do pronto; o C busca depois")
                            confirmarRetomada(geracaoNative, false)
                        }
                        pronto = true
                        duracaoEnviada = duracaoMs(p)
                        ev(EV_PRONTO, duracaoEnviada)
                        if (p.isPlaying) ev(EV_TOCANDO)
                    }
                }
                Player.STATE_ENDED -> ev(EV_FIM)
                else -> {}
            }
            medirEstado(p)
        }

        // Pausa pedida com o player em buffer nao passa por onIsPlayingChanged
        // (ele ja estava parado): a confirmacao do C sai daqui.
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!atual(minha)) return
            val p = player ?: return
            relatarAudio(minha, "playWhenReady=$playWhenReady reason=$reason")
            if (!playWhenReady && !p.isPlaying && p.playbackState != Player.STATE_ENDED) ev(EV_PAUSADO)
            medirEstado(p)
        }

        override fun onVolumeChanged(volume: Float) {
            relatarAudio(minha, "volume=$volume")
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            relatarAudio(minha, "suppression=$playbackSuppressionReason")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!atual(minha)) return
            val p = player ?: return
            if (isPlaying) { if (pronto) ev(EV_TOCANDO) }
            // Parar de tocar por buffer nao e pausa (o evento 7 ja disse).
            else if (!p.playWhenReady && p.playbackState != Player.STATE_ENDED) ev(EV_PAUSADO)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (!atual(minha)) return
            val p = player ?: return
            val manifest = p.currentManifest as? HlsManifest
            val segmentMs = if (p.isCurrentMediaItemLive && manifest != null)
                (manifest.mediaPlaylist.targetDurationUs / 1000L).coerceIn(0L, 60_000L).toInt() else 0
            if (segmentMs != liveSegmentMs) {
                liveSegmentMs = segmentMs
                ev(EV_LIVE_SEGMENT, segmentMs, geracaoDaAbertura)
            }
            if (!pronto) { aplicarFracao(p); return }
            val d = duracaoMs(p)
            if (d != duracaoEnviada) { duracaoEnviada = d; ev(EV_PRONTO, d) }   // so a duracao muda
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo,
                                             newPosition: Player.PositionInfo, reason: Int) {
            if (!atual(minha)) return
            val p = player ?: return
            if (!p.isCurrentMediaItemLive) return
            // Numeric timing only: never log media IDs, manifest URLs or labels.
            val linha = "[live] discontinuity sessao=$minha reason=$reason oldMs=${oldPosition.positionMs} newMs=${newPosition.positionMs} offsetMs=${p.currentLiveOffset} state=${p.playbackState} bufferedMs=${p.totalBufferedDuration}"
            Log.i(TAG, linha)
            try { nativeAudioLog(linha) } catch (_: UnsatisfiedLinkError) { }
        }

        override fun onVideoSizeChanged(v: VideoSize) {
            if (!atual(minha)) return
            // Pixel anamorfico entra na largura: e a proporcao que o zoom do C usa.
            videoW = (v.width * v.pixelWidthHeightRatio + 0.5f).toInt()
            videoH = v.height
            ev(EV_TAMANHO, videoW, videoH)
            if (temJanela) calcularJanela() else aplicarEncaixe()
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!atual(minha) || videoOutputFailed) return
            val r = recursosAtuais
            if (tracks.groups.isNotEmpty()) r?.routeHasVideo = tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO }
            if (r?.routeTx?.active == true || r?.routeNativeOwned == true) r?.let { audioRouteTick(minha, it) }
            else {
                r?.let { selectedAudio(player)?.let { selected -> it.routeSelected = selected } }
                publicarFaixas(tracks)
            }
            for (g in tracks.groups) if (g.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until g.length) if (g.isTrackSelected(i)) applyAfr(minha, g.getTrackFormat(i))
            }
            for (g in tracks.groups) if (g.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until g.length) if (g.isTrackSelected(i)) {
                    val f = g.getTrackFormat(i)
                    relatarAudio(minha, "selecionada grupoIndice=$i idioma=${f.language} mime=${f.sampleMimeType} canais=${f.channelCount} hz=${f.sampleRate}")
                }
            }
            publicarHdr(tracks)
        }

        override fun onCues(cueGroup: CueGroup) {
            if (!atual(minha)) return
            val texto = cueGroup.cues.mapNotNull { it.text?.toString()?.trim() }
                .filter { it.isNotEmpty() }.joinToString("\n")
            // O Media3 nao diz quando o cue acaba: estimativa pelo tamanho, e o
            // proximo grupo (inclusive o vazio) substitui antes disso.
            val dur = if (texto.isEmpty()) 0 else (800 + texto.length * 60).coerceIn(2000, 7000)
            val entregar = Runnable {
                if (atual(minha)) try { nativeLegenda(texto, dur, geracaoDaAbertura) } catch (e: UnsatisfiedLinkError) { }
            }
            if (atrasoMs > 0) principal.postDelayed(entregar, atrasoMs.toLong()) else entregar.run()
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!atual(minha) || videoOutputFailed) return
            val exo = error as? ExoPlaybackException
            val p = player
            val rRoute = recursosAtuais
            val audioRenderer = exo?.type == ExoPlaybackException.TYPE_RENDERER && p != null &&
                exo.rendererIndex in 0 until p.rendererCount &&
                p.getRendererType(exo.rendererIndex) == C.TRACK_TYPE_AUDIO
            if (rRoute != null) {
                // Only a route-correlated audio failure can acquire Q49 recovery.
                // Other errors retain the existing network/video owner.
                if (audioRenderer && !rRoute.routeFailed) {
                    val observation = rRoute.routeDebounce.observe(rRoute.routeObserver.key(rRoute.routeObserver.route()), SystemClock.elapsedRealtime())
                    if (observation == AudioRouteP5D.Debounce.Result.CHANGED) rRoute.routeChanged = true
                    if (!routeBlocked(rRoute) && (rRoute.routeChanged || rRoute.routeTx.active || rRoute.routeDebounce.pending)) {
                        rRoute.routeFatal = true
                        rRoute.routeCheck = true
                        audioRouteTick(minha, rRoute)
                        return
                    }
                }
                if (rRoute.routeFailed) return
                cancelAudioRoute(rRoute, restore = true, recheck = true)
            }
            val status = generateSequence(error.cause) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode
            if (limitedSession && status in listOf(403, 458, 509)) {
                live.cancel()
                ev(EV_ERRO, status ?: 0, C.TRACK_TYPE_UNKNOWN)
                try { nativeErroTexto("HTTP $status") } catch (_: Throwable) {}
                return
            }
            val recoveryTimeout = LiveWindowRecovery.timeoutForSegment(liveSegmentMs)
            if (p != null && live.recover(p, error, SystemClock.elapsedRealtime(), recoveryTimeout) {
                    Log.w(TAG, "[live] recuperacao excedeu $recoveryTimeout ms sessao=$minha; seguindo erro normal")
                    try { nativeErroTexto("${error.errorCodeName} (${error.errorCode}): recovery timeout") } catch (_: Throwable) {}
                    ev(EV_ERRO, error.errorCode, C.TRACK_TYPE_UNKNOWN)
                }) {
                Log.i(TAG, "[live] janela/playlist invalida code=${error.errorCode}; buscando posicao ao vivo sem trocar URL sessao=$minha")
                return
            }
            live.cancel()
            val timeout = generateSequence(error.cause) { it.cause }
                .filterIsInstance<androidx.media3.exoplayer.ExoTimeoutException>().firstOrNull()
            if (p != null && RecriaHdr.timeoutRecuperavel(error.errorCode,
                    recriando || recuperouSuperficie, timeout?.timeoutOperation ?: -1)) {
                // Our detach timeout must never become a source-open error.
                val sv = superficie
                if (RecriaHdr.mayRecoverDetach(recuperouSuperficie, sv != null)) {
                    recuperouSuperficie = true
                    ev(EV_VIDEO_RECOVERY, HdrPolicyP5.FAIL_SURFACE)
                    cancelarDetach()
                    principal.post {
                        if (sv == null || !atual(minha) || videoOutputFailed ||
                            player !== p || superficie !== sv) return@post
                        invalidarQuadroSuperficie()
                        try {
                            sv.visibility = View.VISIBLE
                            p.setVideoSurfaceView(sv)
                            if (!p.isCurrentMediaItemLive) p.seekTo(superficiePos)
                            p.prepare()
                        } catch (e: Exception) {
                            Log.w(TAG, "bounded detach recovery: $e")
                            falhaVideo(minha, HdrPolicyP5.FAIL_SURFACE, error.errorCode, geracaoDaAbertura)
                        }
                    }
                    return
                }
                // A repeated detach failure is terminal, not an infinite wait.
                falhaVideo(minha, HdrPolicyP5.FAIL_SURFACE, error.errorCode, geracaoDaAbertura)
                return
            }
            val tipo = if (exo?.type == ExoPlaybackException.TYPE_RENDERER && p != null &&
                exo.rendererIndex in 0 until p.rendererCount) p.getRendererType(exo.rendererIndex)
                else C.TRACK_TYPE_UNKNOWN
            val renderer = when (tipo) {
                C.TRACK_TYPE_AUDIO -> "audio"
                C.TRACK_TYPE_VIDEO -> "video"
                else -> "desconhecido"
            }
            // Fatal errors can wrap the same sink/codec exception; never log raw messages.
            val causa = generateSequence(error.cause) { it.cause }.take(3)
                .joinToString(" <- ") { camposErroCausa(it) }
            val diagnostico = "${error.errorCodeName} (${error.errorCode}) renderer=$renderer erro=${camposErroCausa(error)} causa=$causa"
            Log.w(TAG, "[player] onPlayerError loadMs=${SystemClock.elapsedRealtime() - abriuEm} $diagnostico")
            try { nativeErroTexto(diagnostico) } catch (_: Throwable) {}
            // Decoder que falha nos primeiros 5 s: o recurso pode estar sendo
            // solto por outro app (ResourceConflict do Tizen); reabre uma vez.
            val cedo = SystemClock.elapsedRealtime() - abriuEm < RETRY_DECODER_MS
            val decoder = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
            val outputRejected = generateSequence(error.cause) { it.cause }
                .any { it is HdrCodecFallbackP5B.OutputRejected }
            val videoFailure = if (outputRejected && tipo == C.TRACK_TYPE_VIDEO) HdrPolicyP5.FAIL_OUTPUT
                else HdrPolicyP5.failure(error.errorCode, tipo == C.TRACK_TYPE_VIDEO,
                    tipo == C.TRACK_TYPE_AUDIO)
            if (videoFailure != 0) {
                val r = recursosAtuais
                // A real runtime codec failure is distinct from initialization
                // fallback (Media3 already tries that list). Exclude only the
                // failed actual decoder for one same-player reprepare. Keep
                // all original media, track parameters, position and effects.
                if (videoFailure == HdrPolicyP5.FAIL_DECODER &&
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED &&
                    p != null && r != null && r.runtimeVideoRecovery == null &&
                    !retentou) {
                    val failed = generateSequence(error.cause) { it.cause }
                        .filterIsInstance<MediaCodecDecoderException>().firstOrNull()?.codecInfo
                    val config = r.videoConfig
                    val format = exo?.rendererFormat ?: p.videoFormat
                    if (failed != null && config != null && format != null &&
                        config.matches(format, failed.name)) {
                        r.runtimeVideoRecovery = HdrCodecFallbackP5B.RuntimeRecovery(failed.name, format)
                        retentou = true
                        // Native ownership is installed before invalidation so
                        // its watchdog cannot take the opening in between.
                        ev(EV_VIDEO_RECOVERY, videoFailure)
                        invalidarQuadroSuperficie()
                        principal.postDelayed({
                            if (atual(minha) && !videoOutputFailed && player === p && recursosAtuais === r) {
                                try { p.prepare() } catch (_: Exception) {
                                    falhaVideo(minha, videoFailure, error.errorCode, geracaoDaAbertura)
                                }
                            }
                        }, 400)
                        return
                    }
                }
                // Media3 has already tried its compatible decoder list. One same-player
                // resource retry may help a transient init/decode failure; no new URL,
                // track-selection policy, cache identity, or source-quality choice.
                if (videoFailure == HdrPolicyP5.FAIL_DECODER && decoder && cedo && !retentou && p != null) {
                    retentou = true
                    ev(EV_VIDEO_RECOVERY, videoFailure)
                    invalidarQuadroSuperficie()
                    principal.postDelayed({
                        if (atual(minha) && !videoOutputFailed && player === p) {
                            try { p.prepare() } catch (_: Exception) {
                                falhaVideo(minha, videoFailure, error.errorCode, geracaoDaAbertura)
                            }
                        }
                    }, 400)
                    return
                }
                falhaVideo(minha, videoFailure, error.errorCode, geracaoDaAbertura)
                return
            }
            if (decoder && cedo && !retentou) {
                retentou = true
                val u = urlAtual
                val c = cabAtual
                val inicio = player?.currentPosition?.takeIf { it > 0 }?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
                    ?: inicioAtualMs
                val geracao = geracaoNative
                val pedido = pedidoAtivo
                val fracao = if (inicio > 0) 0 else fracaoAtual
                principal.postDelayed({ if (atual(minha)) abrirMain(u, c, true, inicio, geracao, pedido, fracao) }, 400)
                return
            }
            ev(EV_ERRO, error.errorCode, tipo)
        }
    }

    private fun analitico(minha: Int, geracaoDaAbertura: Int) = object : AnalyticsListener {
        private var ultimoFormatoEntrada: Format? = null
        // Per listener/session and callback/exception type, on the main looper.
        // First occurrence immediately, then at most once every 30 seconds.
        private val errosAudio = HashMap<Pair<String, Class<*>>, Pair<Long, Long>>()
        private fun relatarErroAudio(tipo: String, erro: Exception) {
            if (!atual(minha)) return
            val chave = tipo to erro.javaClass
            val anterior = errosAudio[chave]
            val agora = SystemClock.elapsedRealtime()
            val ocorrencias = (anterior?.second ?: 0L) + 1L
            if (anterior != null && agora - anterior.first < 30_000L) {
                errosAudio[chave] = anterior.first to ocorrencias
                return
            }
            errosAudio[chave] = agora to ocorrencias
            relatarAudio(minha, "$tipo ${camposErroAudio(erro)} repeticoes=${ocorrencias - 1L}")
        }

        override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String,
                                               initializedTimestampMs: Long, initializationDurationMs: Long) {
            relatarAudio(minha, "decoder=$decoderName initMs=$initializationDurationMs")
        }

        override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format,
                                               decoderReuseEvaluation: DecoderReuseEvaluation?) {
            if (!atual(minha)) return
            ultimoFormatoEntrada = format
            recursosAtuais?.let { it.routeInput = format; it.routeCheck = true }
            relatarAudio(minha, "entrada mime=${format.sampleMimeType} canais=${format.channelCount} hz=${format.sampleRate}")
        }

        override fun onAudioTrackInitialized(eventTime: AnalyticsListener.EventTime, audioTrackConfig: AudioSink.AudioTrackConfig) {
            if (!atual(minha)) return
            relatarAudio(minha, "AudioTrack encoding=${audioTrackConfig.encoding} hz=${audioTrackConfig.sampleRate} canaisMask=${audioTrackConfig.channelConfig} offload=${audioTrackConfig.offload} tunneling=${audioTrackConfig.tunneling}")
            // P5-D validates the actual AudioTrack configuration against the
            // debounced current route; no error from an intermediate handshake.
            recursosAtuais?.let { r ->
                val output = r.routeObserver.output
                if (output != null && eventTime.realtimeMs >= output.createdAt && output.config.encoding == audioTrackConfig.encoding &&
                    output.config.sampleRate == audioTrackConfig.sampleRate && output.config.channelConfig == audioTrackConfig.channelConfig) {
                    r.routeValidatedSerial = output.serial
                    r.routeCheck = true
                    audioRouteTick(minha, r)
                }
            }
        }

        override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
            relatarAudio(minha, "AudioTrack avancando inicioMs=$playoutStartSystemTimeMs")
        }

        override fun onAudioSinkError(eventTime: AnalyticsListener.EventTime, audioSinkError: Exception) {
            relatarErroAudio("sink", audioSinkError)
        }

        override fun onAudioCodecError(eventTime: AnalyticsListener.EventTime, audioCodecError: Exception) {
            relatarErroAudio("codec", audioCodecError)
        }

        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            if (atual(minha)) quadrosPerdidos += droppedFrames
        }

        override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
            val sv = superficie ?: return
            if (!atual(minha) || player == null || detachTicket?.blocksFrames() == true ||
                !RecriaHdr.ownsRenderedFrame(output === sv.holder.surface,
                    sv.holder.surface.isValid, renderTimeMs, superficieInvalidadaEm)) return
            Log.i(TAG, "[player] onRenderedFirstFrame renderer=video loadMs=${SystemClock.elapsedRealtime() - abriuEm} sessao=$sessao hdr=$ultHdr holder=${System.identityHashCode(sv.holder)} soltando=${soltando != null}")
            if (videoOutputFailed) return
            val p = player ?: return
            val f = p.videoFormat ?: recursosAtuais?.videoConfig?.source
            if (f?.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION) {
                val nativeDv = compatibleDolby(f)
                val base = if (nativeDv) null else verifiedBase(f)
                if (!nativeDv && base == null) {
                    falhaVideo(minha, HdrPolicyP5.FAIL_OUTPUT, 0, geracaoDaAbertura)
                    return
                }
                // Actual first frame on the current Surface completes the
                // independent same-source sample/decoder/output proof.
                baseLayerRendered = base
                decoderDv = nativeDv
                publicarHdr(p.currentTracks)
            }
            val r = recursosAtuais
            if (r?.runtimeVideoRecovery?.handoff(r.videoConfig, f, videoDecoderName) == false) {
                falhaVideo(minha, HdrPolicyP5.FAIL_DECODER, 0, geracaoDaAbertura)
                return
            }
            // The retained frame stays visible until the current surface is stable.
            if (estavelEmitido) { esconderQuadro(); limparRetido() }
            ev(EV_PRIMEIRO_QUADRO, geracao = geracaoDaAbertura)
            quadroVisto = true
            logTaxaDeQuadros()
            hdrNaSuperficie()
            avaliarEstavel()
        }

        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime, decoderName: String,
            initializedTimestampMs: Long, initializationDurationMs: Long
        ) {
            if (!atual(minha)) return
            // DV so conta com decoder DV de verdade: OMX.dolby.* / c2.dolby.* e,
            // na MediaTek, c2.mtk.dvhe.* / c2.mtk.dvav.* (TCL Smart TV Pro: o
            // painel engatou Dolby Vision e o selo dizia HDR10, 30/09/2026).
            Log.i(TAG, "[player] decoder de video: $decoderName")
            videoDecoderName = decoderName
            decoderDv = decoderDolby(decoderName, recursosAtuais)
            baseLayerRendered = null
            player?.let { publicarHdr(it.currentTracks) }
        }
    }

    // A duracao chega na timeline assim que o container e lido (MKV: Info +
    // Cues; MP4: moov), antes de o periodo preparar e decodificar. O seek
    // aqui troca o ponto de partida sem um primeiro quadro em 0.
    private fun aplicarFracao(p: ExoPlayer) {
        val f = fracaoPendente
        if (f <= 0) return
        if (p.isCurrentMediaItemLive) {
            fracaoPendente = 0; confirmarRetomada(geracaoNative, false); return
        }
        val d = p.duration
        if (d == C.TIME_UNSET || d <= 1000L || !p.isCurrentMediaItemSeekable) return
        val alvo = d * f / 10000L
        fracaoPendente = 0
        try {
            buscarComHandoff(p, alvo)
            Log.i(TAG, "[player] retomada na preparacao: ${alvo} ms de ${d} ms (${f / 100}.${"%02d".format(f % 100)}%)")
            confirmarRetomada(geracaoNative, true)
        } catch (e: Exception) {
            Log.w(TAG, "retomada por percentual recusada: $e")
            confirmarRetomada(geracaoNative, false)
        }
    }

    private fun duracaoMs(p: ExoPlayer): Int {
        val d = p.duration
        return if (d == C.TIME_UNSET || p.isCurrentMediaItemLive) 0 else d.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    // Lista para o C: audio com decoder e legenda de texto suportada. So manda
    // quando a LISTA muda; troca de selecao nao reenvia (o C reiniciaria a
    // legenda). Legenda sobe sempre desligada.
    private fun publicarFaixas(tracks: Tracks) {
        val owner = geracaoNative
        val a = ArrayList<Pair<Tracks.Group, Int>>()
        val caps = ArrayList<AudioOutputProbeP5A.Observation>()
        val l = ArrayList<Pair<Tracks.Group, Int>>()
        val r = recursosAtuais
        val dsp = audioDsp(r)
        val route = r?.routeObserver?.route()
        var temAudio = false
        for (g in tracks.groups) {
            for (i in 0 until g.length) {
                if (g.type == C.TRACK_TYPE_AUDIO) {
                    temAudio = true
                    if (g.isTrackSupported(i) && a.size < FAIXAS_MAX) {
                        a.add(g to i)
                        caps.add(AudioOutputProbeP5A.inspect(activity, g.getTrackFormat(i), true, dsp,
                            route, r != null))
                    }
                } else if (g.type == C.TRACK_TYPE_TEXT && g.isTrackSupported(i) && l.size < FAIXAS_MAX) {
                    l.add(g to i)
                }
            }
        }
        // Audio no arquivo e nenhum com decoder: o video segue mudo e o C avisa.
        if (temAudio && a.isEmpty() && !avisouSemAudio) { avisouSemAudio = true; ev(EV_AUDIO_SEM_DECODER) }

        val sig = StringBuilder()
        for ((n, par) in a.withIndex()) {
            val f = par.first.getTrackFormat(par.second)
            sig.append('a').append(f.id).append(f.language).append(':')
                .append(f.channelCount).append(':').append(f.sampleRate).append(':')
                .append(f.bitrate).append(';')
        }
        for ((g, i) in l) sig.append('l').append(g.getTrackFormat(i).id).append(g.getTrackFormat(i).language).append(';')
        val nova = sig.toString()
        if (nova == assinatura) {
            // A route-only capability update must not republish the list or
            // make native original-audio selection/subtitle reset run again.
            audios = a
            capacidadesAudio = caps
            legendas = l
            publishRouteCaps(caps)
            return
        }
        val tinhaLegenda = legendas.isNotEmpty()
        assinatura = nova
        audios = a
        capacidadesAudio = caps
        legendas = l
        if (a.isEmpty() && l.isEmpty()) return
        // Lista de legendas mudou: a escolha antiga nao vale (o C zera legAtual).
        if (tinhaLegenda || l.isNotEmpty()) {
            player?.let {
                it.trackSelectionParameters = it.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).clearOverridesOfType(C.TRACK_TYPE_TEXT).build()
            }
        }
        try {
            var sel = -1
            for ((n, par) in a.withIndex()) {
                val (g, i) = par
                if (sel < 0 && g.isTrackSelected(i)) sel = n
                val fmt = g.getTrackFormat(i)
                nativeFaixa(0, n, fmt.language ?: "", 0, fmt.label ?: "",
                    fmt.sampleMimeType ?: "", fmt.channelCount.coerceAtLeast(0), owner)
                val cap = caps[n]
                val d = cap.result
                nativeAudioCapacidade(n, cap.sampleRate, cap.bitDepth, cap.sourceBitrate,
                    if (d.decoder) 1 else 0, if (d.pcmCompatible) 1 else 0,
                    if (d.encodedPassthrough) 1 else 0,
                    if (d.outputFaithfulCandidate) 1 else 0, if (d.unknown) 1 else 0, owner)
            }
            for ((n, par) in l.withIndex()) {
                val f = par.first.getTrackFormat(par.second)
                val flags = (if (f.selectionFlags and C.SELECTION_FLAG_FORCED != 0) 1 else 0) or
                    (if (f.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND != 0) 2 else 0)
                nativeFaixa(1, n, f.language ?: "", flags, f.label ?: "", "", 0, owner)
            }
            nativeFaixasFim(sel, -1, owner)
        } catch (e: UnsatisfiedLinkError) { Log.w(TAG, "faixas sem lib: $e") }
    }

    // HDR do Format do video selecionado; Atmos do audio selecionado (E-AC-3
    // JOC). Dolby Vision so quando o decoder escolhido e DV. Manda so se mudou.
    private fun publicarHdr(tracks: Tracks) {
        if (!atual(sessao) || videoOutputFailed) return
        var hdr = "none"
        var dv = 0
        var atmos = 0
        for (g in tracks.groups) {
            for (i in 0 until g.length) {
                if (!g.isTrackSelected(i)) continue
                val f = g.getTrackFormat(i)
                if (g.type == C.TRACK_TYPE_VIDEO) {
                    val renderFormat = player?.videoFormat ?: recursosAtuais?.videoConfig?.source ?: f
                    val t = renderFormat.colorInfo?.colorTransfer
                    // Actual renderer format wins over an adaptive candidate.
                    // PQ alone still does not prove a compatible DV base layer.
                    val baseConfirmed = baseLayerRendered?.let { it === verifiedBase(renderFormat) } == true
                    val decisao = HdrPolicyP5.decide(
                        dolbyVisionMime = renderFormat.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION,
                        dolbyVisionDecoder = decoderDv && compatibleDolby(renderFormat),
                        st2084 = baseConfirmed || t == C.COLOR_TRANSFER_ST2084,
                        hlg = t == C.COLOR_TRANSFER_HLG,
                        verifiedSameFileHdr10Base = baseConfirmed)
                    hdr = decisao.mode; dv = decisao.dolbyVision
                } else if (g.type == C.TRACK_TYPE_AUDIO && f.sampleMimeType == MimeTypes.AUDIO_E_AC3_JOC) {
                    atmos = 1
                }
            }
        }
        if (hdr == ultHdr && dv == ultDv && atmos == ultAtmos) return
        Log.i(TAG, "[dvtrace] hdr sessao=$sessao $ultHdr->$hdr decoderDv=$decoderDv quadro=$quadroVisto estavel=$estavelEmitido soltando=${soltando != null}")
        ultHdr = hdr; ultDv = dv; ultAtmos = atmos
        try { nativeHdr(hdr, dv, atmos, geracaoNative) } catch (e: UnsatisfiedLinkError) { }
        hdrNaSuperficie()
    }
}
