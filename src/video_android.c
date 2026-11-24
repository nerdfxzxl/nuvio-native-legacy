// Player do Android TV. Quem toca e o Media3 ExoPlayer (NvPlayer.kt), numa
// SurfaceView ATRAS da SDLSurface do app; aqui fica so o estado que o resto do
// app le (video.h) e as chamadas ao Kotlin por JNI. Mesmo desenho do
// video_tpk.c (host .NET): o Kotlin avisa o que acontece pelos natives
// Java_space_nuvio_nativelegacy_NvPlayer_* de qualquer fio, o app le o estado
// no fio dele; por isso os campos sao volatile e nada aqui bloqueia.
//
// DIFERENCAS EM RELACAO AO .tpk
//  * Recorte de fonte de verdade (video_recorte_fonte = 1): o Kotlin posiciona
//    a SurfaceView por layout, entao um retangulo MAIOR que a tela e com origem
//    NEGATIVA e valido e o pai recorta o excedente. No Tizen 9 o ROI fora da
//    painel apagava o plano (#188/#195); aqui nao existe esse limite.
//  * video_pronto() requires a real onRenderedFirstFrame (event 8), or
//    positively identified audio-only media. There is no elapsed-time
//    fallback to a hardware video plane with no rendered image.
//  * HDR/Dolby Vision/Atmos vem do Format das faixas (nativeHdr).
//  * Audio sem decoder: evento 9 (extensao do contrato), nao erro.
//
// Faixas: o Kotlin manda a lista depois que o player as conhece (nativeFaixa e
// nativeFaixasFim) e o app escolhe por escolher(tipo, idx). Legenda EMBUTIDA de
// texto: o Kotlin entrega os cues (nativeLegenda) e o app desenha. Legenda
// EXTERNA (OpenSubtitles/addon) nem passa por aqui: legenda.c baixa e desenha.
//
// TODO: ASS com libass (NV_ASS_LIBASS nao esta definido nesta fase, entao a
// sonda MKV segue "nao ha sonda", como no .tpk); capitulos do MKV para
// video_creditos; passthrough fino de AC3/EAC3 por AudioCapabilities.
#ifdef NV_ANDROID
#include "marco.h"
#include "streams.h"
#include "player.h"
#include "video.h"
#include "mediaurl.h"
#include "ajustes.h"
#include "xtream.h"
#include "capmkv.h"
#include "audioinfo.h"
#include "video_reconexao.h"
#include "bufavanco.h"
#include "idioma.h"
#include "linguas.h"
#include "audioescolha.h"
#include "streamfitpassiva.h"
#include "audsync.h"
#include "cacheboost.h"
#include "tex_cache.h"
#include "velocidade.h"
#include "loading_telemetry_p2.h"
#include "loading_handoff_p2d.h"
#include "videoerro_p5b.h"
#include "vod_buffer.h"
#include "redefit.h"
#include <SDL2/SDL.h>
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <limits.h>
#include <stdint.h>
#include <pthread.h>

// --- ponte JNI ---------------------------------------------------------------
#define NV_CLASSE "space/nuvio/nativelegacy/NvPlayer"

static jclass    gCls;      // GlobalRef: FindClass de fio do SDL nao acha classe do app
static jmethodID mAbrir, mParar, mPausar, mBuscar, mVolume, mJanela, mEscolher;
static jmethodID mAbrirPosicao, mAbrirRecuperacao, mJanelaPronta, mLimitedConnections;
// 2.0.3 (opcional): abrirRetomada(url, cab, inicioMs, fracao, geracao). So o
// percentual conhecido (retomada da conta, sem registro local): o Kotlin
// aplica fracao/10000 da duracao do CONTAINER antes do primeiro quadro.
static jmethodID mAbrirRetomada;
static jmethodID mPreparar, mPrepararProximo, mPrepararEstado, mCancelarPreparo, mAbrirPreparada;
static jmethodID mPararMantendoPreparo;
static unsigned preparoAdotar;
// F07 (optional, like abrirPosicao): cache(mb) and ganho(pct). A shell without
// them simply has no seek cache and no boost.
static jmethodID mBufferProfile, mHttpConfig;
static jmethodID mCache, mGanho, mBuffer, mKbps, mAudioAjustes;
// Warm resume (#492, optional too): titulo(id) and descartarGuardado().
static jmethodID mTitulo, mIdentidade, mDescartar, mMorno, mQuadro;
// #202 (opcional, como os de cima): velocidade(centesimos). Casca sem ele =
// sem a linha da velocidade.
static jmethodID mVelocidade;

static int resolverMetodos(JNIEnv *env) {
  mAbrir    = (*env)->GetStaticMethodID(env, gCls, "abrir", "(Ljava/lang/String;Ljava/lang/String;)V");
  mParar    = (*env)->GetStaticMethodID(env, gCls, "parar", "()V");
  mPausar   = (*env)->GetStaticMethodID(env, gCls, "pausar", "(I)V");
  mBuscar   = (*env)->GetStaticMethodID(env, gCls, "buscar", "(II)V");
  mVolume   = (*env)->GetStaticMethodID(env, gCls, "volume", "(I)V");
  mJanela   = (*env)->GetStaticMethodID(env, gCls, "janela", "(IIIII)V");
  mJanelaPronta = (*env)->GetStaticMethodID(env, gCls, "janelaPronta", "()Z");
  mEscolher = (*env)->GetStaticMethodID(env, gCls, "escolher", "(II)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); return 0; }
  mHttpConfig = (*env)->GetStaticMethodID(env, gCls, "httpConfig", "(III)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mHttpConfig = NULL; }
  mBufferProfile = (*env)->GetStaticMethodID(env, gCls, "bufferProfile", "(IIIIIIII)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mBufferProfile = NULL; }
  mLimitedConnections = (*env)->GetStaticMethodID(env, gCls, "limitedConnections", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mLimitedConnections = NULL; }
  // Opcional: uma casca anterior ainda abre normalmente e recebe o seek
  // depois da duracao. A ausencia deste metodo nao derruba a ponte inteira.
  mAbrirPosicao = (*env)->GetStaticMethodID(env, gCls, "abrirPosicao", "(Ljava/lang/String;Ljava/lang/String;II)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mAbrirPosicao = NULL; }
  // P2-B optional: only bounded SAME-source native retries may ask to hold
  // a video frame. A user-selected new release never uses this method.
  mAbrirRecuperacao = (*env)->GetStaticMethodID(env, gCls, "abrirRecuperacao", "(Ljava/lang/String;Ljava/lang/String;II)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mAbrirRecuperacao = NULL; }
  mAbrirRetomada = (*env)->GetStaticMethodID(env, gCls, "abrirRetomada", "(Ljava/lang/String;Ljava/lang/String;III)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mAbrirRetomada = NULL; }
  mCache = (*env)->GetStaticMethodID(env, gCls, "cache", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mCache = NULL; }
  mTitulo = (*env)->GetStaticMethodID(env, gCls, "cacheTitle", "(Ljava/lang/String;)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mTitulo = NULL; }
  mIdentidade = (*env)->GetStaticMethodID(env, gCls, "cacheIdentity",
      "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IJ)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mIdentidade = NULL; }
  mDescartar = (*env)->GetStaticMethodID(env, gCls, "descartarGuardado", "()V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mDescartar = NULL; }
  mMorno = (*env)->GetStaticMethodID(env, gCls, "morno", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mMorno = NULL; }
  mQuadro = (*env)->GetStaticMethodID(env, gCls, "quadro", "(I)I");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mQuadro = NULL; }
  mBuffer = (*env)->GetStaticMethodID(env, gCls, "buffer", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mBuffer = NULL; }
  mKbps = (*env)->GetStaticMethodID(env, gCls, "kbps", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mKbps = NULL; }
  mGanho = (*env)->GetStaticMethodID(env, gCls, "ganho", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mGanho = NULL; }
  mAudioAjustes = (*env)->GetStaticMethodID(env, gCls, "audioAdjustments", "(IIII)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mAudioAjustes = NULL; }
  mVelocidade = (*env)->GetStaticMethodID(env, gCls, "velocidade", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mVelocidade = NULL; }
  mPreparar = (*env)->GetStaticMethodID(env, gCls, "preparar", "(Ljava/lang/String;Ljava/lang/String;IIIIILjava/lang/String;I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mPreparar = NULL; }
  mPrepararProximo = (*env)->GetStaticMethodID(env, gCls, "prepararProximo", "(Ljava/lang/String;Ljava/lang/String;IIIIILjava/lang/String;I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mPrepararProximo = NULL; }
  mPrepararEstado = (*env)->GetStaticMethodID(env, gCls, "prepararEstado", "(I)I");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mPrepararEstado = NULL; }
  mCancelarPreparo = (*env)->GetStaticMethodID(env, gCls, "cancelarPreparo", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mCancelarPreparo = NULL; }
  mAbrirPreparada = (*env)->GetStaticMethodID(env, gCls, "abrirPreparada", "(Ljava/lang/String;Ljava/lang/String;IIIILjava/lang/String;)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mAbrirPreparada = NULL; }
  mPararMantendoPreparo = (*env)->GetStaticMethodID(env, gCls, "pararMantendoPreparo", "(I)V");
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); mPararMantendoPreparo = NULL; }
  return mAbrir && mParar && mPausar && mBuscar && mVolume && mJanela && mEscolher;
}

// Chamado pelo Kotlin (NvPlayer.iniciar), no fio principal, que ve a classe do
// app. Guarda a classe e os jmethodID para os fios do SDL usarem depois.
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeIniciar(JNIEnv *env, jclass cls) {
  if (!gCls) gCls = (*env)->NewGlobalRef(env, cls);
  if (!resolverMetodos(env)) { gCls = NULL; printf("[video] android: metodos do NvPlayer nao achados\n"); }
  else printf("[video] android: ponte do NvPlayer pronta\n");
  fflush(stdout);
}

// --- F06: PCM do audio tocando (AudioSyncTap.kt) -----------------------------
// O tap so existe numa casca que o anuncia: o primeiro nativeAudioEstado
// registra o backend. Casca antiga = plataforma sem PCM (o ajuste diz isso).
static void ligarTap(int on);
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeAudioEstado(JNIEnv *env, jclass cls, jint fmt) {
  static int registrado;
  (void)env; (void)cls;
  if (!registrado) { registrado = 1; audsync_backend(ligarTap); }
  audsync_formato((int)fmt);
  // F07: the same sink-input format decides the boost. AudioSyncTap FMT_*
  // (0 none, 1 PCM, 2 bitstream) maps 1:1 to CB_GANHO_* (unknown, PCM,
  // passthrough): with bitstream the gain processor never sees samples.
  { static int ultimoGanho = -1;
    int g = fmt == 1 ? CB_GANHO_PCM : fmt == 2 ? CB_GANHO_PASSTHROUGH : CB_GANHO_DESCONHECIDO;
    cacheboost_ganho_relato(g);
    if (g != ultimoGanho && g != CB_GANHO_DESCONHECIDO) {
      printf("[video] android audio: %s\n", g == CB_GANHO_PASSTHROUGH ? "passthrough (sem reforco)" : "PCM (reforco disponivel)");
      fflush(stdout);
    }
    ultimoGanho = g; }
}
// Fio de reproducao do ExoPlayer. Copia para a pilha e entrega ao anel
// limitado do audsync.c: nenhuma alocacao, nunca espera o analisador.
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeAudioPcm(JNIEnv *env, jclass cls, jshortArray pcm, jint n, jlong ptsUs) {
  jshort b[2048];
  (void)cls;
  if (!pcm || n <= 0 || n > (jint)(sizeof b / sizeof *b)) return;
  (*env)->GetShortArrayRegion(env, pcm, 0, n, b);
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); return; }
  audsync_pcm((const int16_t *)b, (int)n, (int64_t)ptsUs);
}

// Plano B se o Kotlin nao chamou nativeIniciar antes do primeiro uso (lib
// carregada depois do iniciar): o ClassLoader da Activity enxerga a classe.
static int garantirPonte(JNIEnv *env) {
  jobject act;
  jclass cAct, cLoader, achada;
  jmethodID mGet, mLoad;
  jobject loader;
  jstring nome;
  if (gCls) return 1;
  act = (jobject)SDL_AndroidGetActivity();
  if (!act) return 0;
  cAct = (*env)->GetObjectClass(env, act);
  mGet = (*env)->GetMethodID(env, cAct, "getClassLoader", "()Ljava/lang/ClassLoader;");
  loader = mGet ? (*env)->CallObjectMethod(env, act, mGet) : NULL;
  cLoader = loader ? (*env)->GetObjectClass(env, loader) : NULL;
  mLoad = cLoader ? (*env)->GetMethodID(env, cLoader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;") : NULL;
  nome = (*env)->NewStringUTF(env, "space.nuvio.nativelegacy.NvPlayer");
  achada = (mLoad && nome) ? (jclass)(*env)->CallObjectMethod(env, loader, mLoad, nome) : NULL;
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); achada = NULL; }
  if (achada) { gCls = (*env)->NewGlobalRef(env, achada); if (!resolverMetodos(env)) gCls = NULL; }
  if (nome) (*env)->DeleteLocalRef(env, nome);
  if (achada) (*env)->DeleteLocalRef(env, achada);
  if (cLoader) (*env)->DeleteLocalRef(env, cLoader);
  if (loader) (*env)->DeleteLocalRef(env, loader);
  (*env)->DeleteLocalRef(env, cAct);
  (*env)->DeleteLocalRef(env, act);
  return gCls != NULL;
}

// UTF-8 -> jstring (UTF-16). O NewStringUTF do JNI e UTF-8 MODIFICADO e erra
// em tudo fora do BMP; cabecalhos e URLs raramente passam disso, mas o custo e
// pequeno.
static jstring paraJString(JNIEnv *env, const char *s) {
  jchar buf[NV_MEDIA_URL_MAX];
  int n = 0;
  const unsigned char *p = (const unsigned char *)(s ? s : "");
  while (*p && n < NV_MEDIA_URL_MAX - 2) {
    unsigned c = *p++, extra = 0;
    if (c >= 0xF0) { c &= 0x07; extra = 3; }
    else if (c >= 0xE0) { c &= 0x0F; extra = 2; }
    else if (c >= 0xC0) { c &= 0x1F; extra = 1; }
    else if (c >= 0x80) c = 0xFFFD;
    while (extra-- && (*p & 0xC0) == 0x80) c = (c << 6) | (*p++ & 0x3F);
    if (c >= 0x10000) { c -= 0x10000; buf[n++] = (jchar)(0xD800 + (c >> 10)); buf[n++] = (jchar)(0xDC00 + (c & 0x3FF)); }
    else buf[n++] = (jchar)c;
  }
  return (*env)->NewString(env, buf, n);
}

// jstring (UTF-16) -> UTF-8 de verdade em dst (GetStringUTFChars daria CESU-8
// para emoji, que o SDL_ttf nao le).
static void deJString(JNIEnv *env, jstring js, char *dst, size_t tam) {
  const jchar *u;
  jsize n, i;
  size_t o = 0;
  if (tam == 0) return;
  dst[0] = 0;
  if (!js) return;
  u = (*env)->GetStringChars(env, js, NULL);
  if (!u) return;
  n = (*env)->GetStringLength(env, js);
  for (i = 0; i < n && o + 5 < tam; i++) {
    unsigned c = u[i];
    if (c >= 0xD800 && c < 0xDC00 && i + 1 < n && u[i + 1] >= 0xDC00 && u[i + 1] < 0xE000) {
      c = 0x10000 + ((c - 0xD800) << 10) + (u[i + 1] - 0xDC00); i++;
    }
    if (c < 0x80) dst[o++] = (char)c;
    else if (c < 0x800) { dst[o++] = (char)(0xC0 | (c >> 6)); dst[o++] = (char)(0x80 | (c & 0x3F)); }
    else if (c < 0x10000) { dst[o++] = (char)(0xE0 | (c >> 12)); dst[o++] = (char)(0x80 | ((c >> 6) & 0x3F)); dst[o++] = (char)(0x80 | (c & 0x3F)); }
    else { dst[o++] = (char)(0xF0 | (c >> 18)); dst[o++] = (char)(0x80 | ((c >> 12) & 0x3F));
           dst[o++] = (char)(0x80 | ((c >> 6) & 0x3F)); dst[o++] = (char)(0x80 | (c & 0x3F)); }
  }
  dst[o] = 0;
  (*env)->ReleaseStringChars(env, js, u);
}

// Env do fio que chama (o SDL ja anexa o fio do app) ou NULL sem ponte.
static JNIEnv *ambiente(void) {
  JNIEnv *env = (JNIEnv *)SDL_AndroidGetJNIEnv();
  if (!env || !garantirPonte(env)) return NULL;
  return env;
}
static void fimChamada(JNIEnv *env) {
  if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); printf("[video] android: excecao no NvPlayer\n"); fflush(stdout); }
}
static void kSemArg(jmethodID m) {
  JNIEnv *env = ambiente();
  if (!env) return;
  (*env)->CallStaticVoidMethod(env, gCls, m);
  fimChamada(env);
}
static void kInt(jmethodID m, int v) {
  JNIEnv *env = ambiente();
  if (!env) return;
  (*env)->CallStaticVoidMethod(env, gCls, m, (jint)v);
  fimChamada(env);
}

static void enviarPerfil(JNIEnv *env, int averageBps) {
  if (mHttpConfig) {
    int http[3];
    ajustes_http_config(http);
    (*env)->CallStaticVoidMethod(env, gCls, mHttpConfig, (jint)http[0], (jint)http[1], (jint)http[2]);
    fimChamada(env);
  }
  if (!mBufferProfile) return;
  int values[8];
  ajustes_buffer_profile(averageBps, values);
  (*env)->CallStaticVoidMethod(env, gCls, mBufferProfile,
      (jint)values[0], (jint)values[1], (jint)values[2], (jint)values[3],
      (jint)values[4], (jint)values[5], (jint)values[6], (jint)values[7]);
  fimChamada(env);
}

static int precarregarVideo(const char *url, const char *cab, double segundos,
                            double pct, int buffer, int cacheMb, unsigned token,
                            const char *idiomaAudio, int kbps, int proximo) {
  JNIEnv *env;
  jmethodID metodo;
  jstring u, c, a;
  int inicioMs, fracao, ok;
  if (!url || !*url || !token || token > INT_MAX || xtream_url_limitada(url)) return 0;
  env = ambiente();
  metodo = proximo ? mPrepararProximo : mPreparar;
  if (!env || !metodo || !mPrepararEstado || !mCancelarPreparo || !mAbrirPreparada) return 0;
  inicioMs = isfinite(segundos) && segundos > 0.0 && segundos <= INT_MAX / 1000.0
               ? (int)(segundos * 1000.0) : 0;
  fracao = !inicioMs && isfinite(pct) && pct > 0.0 && pct < 100.0
               ? (int)(pct * 100.0 + 0.5) : 0;
  u = paraJString(env, nv_media_url(url));
  c = u && !(*env)->ExceptionCheck(env) ? paraJString(env, cab ? cab : "") : NULL;
  a = c && !(*env)->ExceptionCheck(env) ? paraJString(env, idiomaAudio ? idiomaAudio : "") : NULL;
  if (!u || !c || !a) {
    if (u) (*env)->DeleteLocalRef(env, u);
    if (c) (*env)->DeleteLocalRef(env, c);
    if (a) (*env)->DeleteLocalRef(env, a);
    fimChamada(env); return 0;
  }
  enviarPerfil(env, player_buffer_average(url, cab ? cab : "", 1));
  (*env)->CallStaticVoidMethod(env, gCls, metodo, u, c, (jint)inicioMs,
      (jint)fracao, (jint)token, (jint)buffer, (jint)cacheMb, a, (jint)kbps);
  ok = !(*env)->ExceptionCheck(env);
  (*env)->DeleteLocalRef(env, u); (*env)->DeleteLocalRef(env, c);
  (*env)->DeleteLocalRef(env, a);
  fimChamada(env);
  return ok;
}
int video_precarregar(const char *url, const char *cab, double segundos,
                     double pct, int buffer, int cacheMb, unsigned token, const char *idiomaAudio, int kbps) {
  return precarregarVideo(url, cab, segundos, pct, buffer, cacheMb, token, idiomaAudio, kbps, 0);
}
int video_precarregar_proximo(const char *url, const char *cab, double segundos,
                             double pct, int buffer, int cacheMb, unsigned token, const char *idiomaAudio, int kbps) {
  return precarregarVideo(url, cab, segundos, pct, buffer, cacheMb, token, idiomaAudio, kbps, 1);
}
int video_precarregar_estado(unsigned token) {
  JNIEnv *env = ambiente();
  int estado;
  if (!env || !mPrepararEstado || !token || token > INT_MAX) return 0;
  estado = (*env)->CallStaticIntMethod(env, gCls, mPrepararEstado, (jint)token);
  if ((*env)->ExceptionCheck(env)) { fimChamada(env); return 0; }
  return estado >= -1 && estado <= 2 ? estado : 0;
}
void video_precarregar_cancelar(unsigned token) {
  JNIEnv *env;
  if (!token || preparoAdotar == token) preparoAdotar = 0;
  env = ambiente();
  if (env && mCancelarPreparo)
    (*env)->CallStaticVoidMethod(env, gCls, mCancelarPreparo, (jint)token);
  if (env) fimChamada(env);
}
void video_precarregar_adotar(unsigned token) { preparoAdotar = token <= INT_MAX ? token : 0; }

// escolher(3, on): liga/desliga o tap no Kotlin (so posta ao fio principal).
static void ligarTap(int on) {
  JNIEnv *env = ambiente();
  if (!env) return;
  (*env)->CallStaticVoidMethod(env, gCls, mEscolher, (jint)3, (jint)(on ? 1 : 0));
  fimChamada(env);
}

// --- estado ------------------------------------------------------------------
#define MAX_FAIXAS NV_FAIXA_MAX
static VideoFaixa faixaAudio[MAX_FAIXAS], faixaLeg[MAX_FAIXAS];
static volatile int nAudio, nLeg, audioAtual, legAtual = -1;
static SDL_mutex *travaLeg;
static char legTexto[1024];
static Uint32 legAte;

static char urlAtual[NV_MEDIA_URL_MAX];
static char cabecalhos[2048];
// ULTIMA JANELA ENTREGUE ao Kotlin (dedupe de reenvio; ver janelaSeMudou, la
// embaixo, onde a regra vive junto das funcoes que enviam).
static int envX, envY, envW, envH, envEnc, temEnv;
// READY, first frame in this playback session, and first frame on the CURRENT
// SurfaceView are distinct. Recreating the surface must not reset Media3's
// session history, but must gate the first cinematic visual handoff.
static volatile int superficieEstavel, quadroSuperficie;
static volatile int ativo, prontoLoad, primeiroQuadro, falhou, terminou, tocando, largura, altura;
static volatile int cargaFase, somenteAudio;
static volatile int conflito, semDecoderAudio;
// Native track identities to carry over one explicit same-episode source switch.
// Ordinal indices are not transferable between unrelated releases.
static VideoFaixa p1Audio, p1Leg;
static int p1AudioValido, p1LegValida, p1FaixasPendentes;
// PAUSA CONFIRMADA (player_suspender): o pedido daqui e o evento 3 do Kotlin
// DEPOIS dele. O evento chega do fio principal ~3 ms depois (medido na TCL,
// 02/10); um evento 2 (tocando) no meio desfaz a confirmacao.
static volatile int pausaPedida, pausaVista;
static volatile int durMs, bufferando, posMs, posNativoMs;
// VELOCIDADE (#202, video.h): o ExoPlayer novo de cada abertura nasce em 1x
// (velEnviada = 100 no abrirSessao) e o video_bombear reaplica o pedido assim
// que ele esta pronto. setPlaybackSpeed aceita qualquer valor positivo; com o
// audio em PASSTHROUGH (bitstream ao receptor) eu acho que o sink nao consegue
// mudar o tempo e o Media3 segue em 1x, sem erro — nao provado em TV.
static volatile int velPedida = 100, velEnviada = 100, velRecusada;
static volatile Uint32 bufferDesde, tocandoDesde;
static volatile const char *hdrAtual = "none";
static volatile int dvAtual, atmosAtual;
static unsigned sessao;
// Signed position and buffer edge are sampled together in the same live window.
// Protected by travaRetomada and scoped to the current open generation.
static jlong livePosMs, liveBufferFimMs, vodBufferFimMs;
static int seekRevision;
static int liveProgressoValido, liveBufferValido;
static int sampleBytes = -1, samplePeakBytes = -1;
static unsigned liveSegmentoMs; // protected by travaRetomada; reset on each open
static Uint32 liveRecuperandoAte; // protected by travaRetomada, with the open generation
// F07: the player arms the next video_tocar with its cache limit
// (cacheboost_backend_cache); trailers never arm, so they stay uncached and
// at 100%. `cacheSessao` survives the reconnection reopens of that video.
static int bufferPreset, bufferEnviado;
// Playback StreamFit (redefit.h): declared kbps of this open (0 = trailer or
// unknown), sent sticky to Kotlin for the buffer size; last seek/track switch
// so their BUFFERING is not counted as a rebuffer. State under travaStats.
static int redeKbps, kbpsEnviado = -1;
static volatile Uint32 acaoMs;
static int cacheArmado = -1, cacheSessao, cacheEnviado, playerSessao;
// Warm resume (#492): id of the title the player armed for the next open, the
// one of this open (empty for trailers/channels) and the one Kotlin has.
static char tituloArmado[96], tituloSessao[96], tituloEnviado[96];
static int ganhoEnviado = CB_VOL_NORMAL;
static int retomadaInicialEstado = -1;
static pthread_mutex_t travaRetomada = PTHREAD_MUTEX_INITIALIZER;
// Serialize playback event publication against opening/closing generation reset.
// Lock order: travaEvento -> travaRetomada (and, transiently, travaStats).
static pthread_mutex_t travaEvento = PTHREAD_MUTEX_INITIALIZER;

// RECONEXAO (video_reconexao.h): igual ao .tpk. O evento 5 so ANOTA; a
// decisao e o recarregar sao do video_bombear.
static NvReconexao recon;
static BufAvanco reconBufferAvanco; // actual buffer progress vetoes false stalls
static int reconProxima, reconPermitida, reconIniciou;
static volatile int reconErroPend, reconErroCod, reconErroVideo;
static volatile int terminalVideoP5B;
static int reconAudio = -1, reconLeg = -1;
static volatile int reconFaixasPend;
static int reconBuscarMs = -1;
static int reconPausado;
static int recarregarPausa = -1, recarregarPosMs;
static char erroTxt[64];

// Media3 I/O failures, including HTTP rejection and premature EOF. Recovery
// is bounded; the app can renew the addon URL after reopening fails.
static int erroDeRede(int cod) { return (cod >= 2000 && cod <= 2002) ||
                                      (cod >= 2004 && cod <= 2006) || cod == 2008; }

void video_escolher_audio(int i);

// Source-quality ranking uses genuine Android Format metadata where present.
// Format.codec alone does not determine fidelity, and output compatibility
// was already independently established by the current-route probe.
static long notaFonteAudio(const VideoFaixa *f) {
  long score = (long)audioesc_qualidade(f->codec, f->canais, f->rotulo) * 100000L;
  score += (long)(f->amostraHz > 0 && f->amostraHz <= 384000 ? f->amostraHz : 0) * 2L;
  score += (long)(f->bits > 0 && f->bits <= 64 ? f->bits : 0) * 3000L;
  score += (long)(f->bitrate > 0 ? f->bitrate / 100 : 0);
  return score;
}

// A ORDEM DO AUDIO (audioescolha.h), como no video_tpk.c; sem regra que
// decida, fica a do arquivo.
static void escolherAudioPreferido(void) {
  AudioCand c[MAX_FAIXAS];
  int i, o, k;
  if (nAudio < 2) return;
  for (i = 0; i < nAudio; i++) {
    c[i].idioma = faixaAudio[i].idioma; c[i].codec = faixaAudio[i].codec; c[i].rotulo = faixaAudio[i].rotulo;
    c[i].canais = faixaAudio[i].canais;
    // Decoder support alone cannot prove that PCM or encoded audio reaches
    // the current HDMI/speaker route. Kotlin publishes independent findings.
    c[i].dts = audioesc_dts_truehd(c[i].codec, c[i].rotulo);
    c[i].pode = faixaAudio[i].decodifica && faixaAudio[i].saidaFiel;
    c[i].numero = faixaAudio[i].numero;
  }
  k = audioesc_escolher(c, nAudio, &o);
  if (audioesc_tem_lembrada() && o != AUDIOESC_LEMBRADA) {
    // A remembered manual language must not be silently replaced by the
    // original or a dub just because this release lacks that manual track.
    semDecoderAudio = 1;
    return;
  }
  if (k >= 0 && o == AUDIOESC_ORIGINAL) {
    long melhor = notaFonteAudio(&faixaAudio[k]);
    for (i = 0; i < nAudio; i++) {
      if (c[i].pode && c[i].idioma && c[i].idioma[0] &&
          ling_casa(c[i].idioma, c[k].idioma)) {
        long nota = notaFonteAudio(&faixaAudio[i]);
        if (nota > melhor) { k = i; melhor = nota; }
      }
    }
  }
  if (k < 0 || k == audioAtual) return;
  printf("[video] audio pela ordem (%s): %s (faixa %d de %d)\n", audioesc_origem_nome(o), faixaAudio[k].rotulo, k + 1, nAudio);
  video_escolher_audio(k);
}

// --- natives (Kotlin -> C) ----------------------------------------------------
// Fio principal do Kotlin, depois que o player conhece as faixas: uma chamada
// por faixa (tipo 0 = audio, 1 = legenda) e no fim nativeFaixasFim. O contador
// so sobe no fim, com a lista inteira escrita.
static VideoFaixa novasA[MAX_FAIXAS], novasL[MAX_FAIXAS];
static int nNovasA, nNovasL;

// `flags` (#287): bit 0 = SELECTION_FLAG_FORCED, bit 1 = ROLE_FLAG_DESCRIBES_
// MUSIC_AND_SOUND (SDH); `nome` = Format.label (o Name da faixa do MKV).
// `mime`/`canais` (#293): codec e canais da faixa de audio.
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeFaixa(JNIEnv *env, jclass cls, jint tipo, jint idx, jstring lingua,
                                                                       jint flags, jstring nome, jstring mime, jint canais) {
  VideoFaixa *f;
  char l[16], nm[48], mm[24], ai[40];
  (void)cls;
  deJString(env, lingua, l, sizeof l);
  deJString(env, nome, nm, sizeof nm);
  if (tipo == 0) { if (nNovasA >= MAX_FAIXAS) return; f = &novasA[nNovasA++]; }
  else           { if (nNovasL >= MAX_FAIXAS) return; f = &novasL[nNovasL++]; }
  memset(f, 0, sizeof *f);
  f->numero = idx;
  f->ordinalMkv = tipo ? idx : -1;
  if (strcmp(l, "und") && strcmp(l, "unknown") && strcmp(l, "zxx")) snprintf(f->idioma, sizeof f->idioma, "%s", l);
  if (f->idioma[0]) snprintf(f->rotulo, sizeof f->rotulo, "%s", i18n(ling_nome(f->idioma)));
  else snprintf(f->rotulo, sizeof f->rotulo, "%s %d", i18n(tipo ? "Legenda" : "Áudio"),
                tipo ? nNovasL : nNovasA);
  if (tipo) {
    const char *r;
    // O nome de exibicao nao passa pelo buffer curto usado pela classificacao.
    // deJString preserva UTF-8 completo, inclusive emoji, ate o limite do campo.
    deJString(env, nome, f->nome, sizeof f->nome);
    f->tipoLeg = ling_tipo_legenda(nm, flags & 1, (flags & 2) != 0);
    f->letreiro = f->tipoLeg == LING_LEG_FORCADA || f->tipoLeg == LING_LEG_LETREIROS;
    r = ling_tipo_legenda_rotulo(f->tipoLeg);
    if (r) {
      size_t k = strlen(f->rotulo);
      snprintf(f->rotulo + k, sizeof f->rotulo - k, "  \xc2\xb7  %s", i18n(r));
    }
  } else {
    // #293: codec (mime do Format) e canais; a linha de baixo da folha de Audio
    // vira "E-AC-3 5.1 Atmos". Mime desconhecido: rotulo como estava.
    deJString(env, mime, mm, sizeof mm);
    snprintf(f->codec, sizeof f->codec, "%s", mm);
    deJString(env, nome, f->nome, sizeof f->nome);
    f->canais = canais;
    if (audioinfo_texto(mm, canais, 0, ai, sizeof ai))
    { char base[sizeof f->rotulo];
      snprintf(base, sizeof base, "%s", f->rotulo);
      snprintf(f->rotulo, sizeof f->rotulo, "%s  \xc2\xb7  %s", base, ai); }
  }
}

// Q36 current-route capability is separate from renderer support. Arrives
// immediately after nativeFaixa in the SAME Kotlin track enumeration callback.
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeAudioCapacidade(JNIEnv *env, jclass cls,
    jint idx, jint hz, jint bits, jint bitrate, jint dec, jint pcm, jint pas, jint fiel, jint incerto) {
  VideoFaixa *f;
  (void)env; (void)cls;
  if (idx < 0 || idx >= nNovasA) return;
  f = &novasA[idx];
  f->amostraHz = hz > 0 ? hz : 0;
  f->bits = bits > 0 ? bits : 0;
  f->bitrate = bitrate > 0 ? bitrate : 0;
  f->decodifica = dec == 1;
  f->pcmSaida = pcm == 1;
  f->passthroughSaida = pas == 1;
  f->saidaFiel = fiel == 1;
  f->saidaIncerta = incerto == 1;
}

static void validarAudioSelecionadoP5A(void) {
  if (nAudio <= 0) return; // Kotlin reports no compatible decoder itself.
  const VideoFaixa *a = video_audio(audioAtual);
  const AudioLembrada *m = audioesc_lembrada();
  const char *original = ling_original();
  // An explicit manual track can intentionally differ from the original.
  // It MUST NOT silently fall back to a different language if unavailable.
  int manualErrado = m && m->idioma[0] &&
      (!a || !a->idioma[0] || !ling_casa(a->idioma, m->idioma));
  int idiomaErrado = !m && original && original[0] &&
      strcmp(original, "none") && a &&
      (!a->idioma[0] || !ling_casa(a->idioma, original));
  if (!a || !a->decodifica || !a->saidaFiel || manualErrado || idiomaErrado) {
    semDecoderAudio = 1;
    printf("[audio] Q36 selected output incompatible: track=%d decoder=%d pcm=%d pass=%d faithful=%d unknown=%d manualMismatch=%d originalMismatch=%d\n",
           audioAtual, a ? a->decodifica : 0, a ? a->pcmSaida : 0,
           a ? a->passthroughSaida : 0, a ? a->saidaFiel : 0,
           a ? a->saidaIncerta : 1, manualErrado, idiomaErrado);
    fflush(stdout);
  }
}

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeFaixasFim(JNIEnv *env, jclass cls, jint selAudio, jint selLeg) {
  // Lista de legendas VAZIA mandada com a de audio ja existente = releitura so
  // do audio (HLS que publica as faixas depois de tocar): a legenda que o app
  // ja escolheu fica.
  int releitura = (nAudio || nLeg) && !nNovasL && nLeg;
  (void)env; (void)cls; (void)selLeg;
  memcpy(faixaAudio, novasA, sizeof novasA);
  audioAtual = selAudio >= 0 ? selAudio : 0;
  if (!releitura) {
    memcpy(faixaLeg, novasL, sizeof novasL);
    legAtual = -1;   // o Kotlin sobe com legenda desligada; quem liga e o app (faixas.c)
    nLeg = nNovasL;
  }
  nAudio = nNovasA;
  nNovasA = nNovasL = 0;
  printf("[video] faixas: %d audio, %d legenda\n", nAudio, nLeg);
  fflush(stdout);
  // Recarregar de reconexao: devolve as faixas que a pessoa tinha.
  if (reconFaixasPend) {
    if (!releitura && reconLeg >= -1 && reconLeg < nLeg) video_escolher_legenda(reconLeg);
    if (nAudio > 0) {
      reconFaixasPend = 0;
      if (reconAudio >= 0 && reconAudio < nAudio) video_escolher_audio(reconAudio);
    }
    printf("[video] reconexao: faixas devolvidas (audio %d, legenda %d)\n", reconAudio, reconLeg);
    fflush(stdout);
    validarAudioSelecionadoP5A();
    return;
  }
  // Explicit same-episode source replacement: restore only tracks with a
  // matching language + codec/channel (audio) or language + subtitle kind.
  // Never carry ordinal n from file A to arbitrary track n in file B.
  int restaurouAudio = 0;
  if (p1FaixasPendentes) {
    p1FaixasPendentes = 0;
    if (p1AudioValido) {
      int melhor = -1, pontuacao = -1, empatou = 0;
      for (int k = 0; k < nAudio; k++) {
        const VideoFaixa *f = &faixaAudio[k];
        if (!f->idioma[0] || strcmp(f->idioma, p1Audio.idioma)) continue;
        int pontos = 1;
        if (p1Audio.codec[0] && !strcmp(f->codec, p1Audio.codec)) pontos += 2;
        if (p1Audio.canais > 0 && f->canais == p1Audio.canais) pontos++;
        if (pontos > pontuacao) { melhor = k; pontuacao = pontos; empatou = 0; }
        else if (pontos == pontuacao) empatou = 1;
      }
      if (melhor >= 0 && !empatou) { video_escolher_audio(melhor); restaurouAudio = 1; }
    }
    if (p1LegValida && !releitura) {
      int unica = -1;
      for (int k = 0; k < nLeg; k++) {
        const VideoFaixa *f = &faixaLeg[k];
        if (!f->idioma[0] || strcmp(f->idioma, p1Leg.idioma) ||
            f->tipoLeg != p1Leg.tipoLeg) continue;
        if (unica >= 0) { unica = -1; break; }
        unica = k;
      }
      if (unica >= 0) video_escolher_legenda(unica);
    }
    p1AudioValido = p1LegValida = 0;
  }
  if (!restaurouAudio) escolherAudioPreferido();
  validarAudioSelecionadoP5A();
}

// Texto da legenda embutida em vigor, valido por `durMs` (estimada no Kotlin:
// o Media3 nao diz quando o cue acaba, so quando o proximo grupo chega).
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeLegenda(JNIEnv *env, jclass cls, jstring texto, jint dur) {
  char buf[sizeof legTexto];
  (void)cls;
  if (!travaLeg) return;
  deJString(env, texto, buf, sizeof buf);
  SDL_LockMutex(travaLeg);
  memcpy(legTexto, buf, sizeof legTexto);
  legAte = SDL_GetTicks() + (Uint32)(dur > 0 ? dur : 3000);
  SDL_UnlockMutex(travaLeg);
}

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativePos(JNIEnv *env, jclass cls,
    jlong ms, jlong fimMs, jboolean live, jint memoria, jint geracao, jint revision) {
  (void)env; (void)cls;
  pthread_mutex_lock(&travaRetomada);
  if (ativo && geracao > 0 && (unsigned)geracao == sessao && revision == seekRevision) {
    /* Media3 TIME_UNSET is not a playback position. */
    int conhecida = ms > (jlong)(INT64_MIN + 1);
    posMs = posNativoMs = !conhecida || ms < 0 ? 0 : ms > INT_MAX ? INT_MAX : (int)ms;
    liveProgressoValido = live && conhecida;
    livePosMs = conhecida ? ms : 0;
    liveBufferValido = liveProgressoValido && fimMs > (jlong)(INT64_MIN + 1) && fimMs >= ms;
    liveBufferFimMs = liveBufferValido ? fimMs : 0;
    vodBufferFimMs = live ? 0 : nv_vod_buffer_end(ms, fimMs, durMs);
    /* Sample allocator only: excludes decoder, textures and retained free blocks. */
    sampleBytes = memoria >= 0 ? memoria : -1;
    if (sampleBytes > samplePeakBytes) samplePeakBytes = sampleBytes;
  }
  pthread_mutex_unlock(&travaRetomada);
}

// ESTATISTICAS AO VIVO (video.h, VideoStats): o Kotlin manda 1x/s, do fio
// principal, o que ele CONTA — bytes do socket (ParaleloDataSource), buffer
// (bufferedPosition), conexoes abertas, 1o byte, host final e quadros
// (AnalyticsListener). A trava e curta: o app copia no quadro dele.
static pthread_mutex_t travaStats = PTHREAD_MUTEX_INITIALIZER;
static VideoStats statsAndroid;
static RedeFit redeFit;
static int statsValido, redeMinMs = -1, redeMaxMs = -1, redeMaxMb = -1;
static double redeMbps = -1;
static NvMedidorDownload medidorDownload;
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeStats(JNIEnv *env, jclass cls,
    jlong bytes, jint bufferMs, jint alvoMs, jint conexoes, jint primeiroByteMs,
    jlong perdidos, jlong quadros, jstring host, jint minMs, jint alvoMb, jint carregando, jint geracao) {
  char h[96];
  double mbps = -1;
  int sinal;
  int memoria, pico;
  (void)cls;
  pthread_mutex_lock(&travaRetomada);
  /* P1 identity: an old player's telemetry cannot describe a new stream. */
  if (!ativo || geracao <= 0 || (unsigned)geracao != sessao) {
    pthread_mutex_unlock(&travaRetomada);
    return;
  }
  memoria = sampleBytes; pico = samplePeakBytes;
  pthread_mutex_unlock(&travaRetomada);
  deJString(env, host, h, sizeof h);
  pthread_mutex_lock(&travaStats);
  video_stats_vazio(&statsAndroid);
  nv_download_amostrar(&medidorDownload, bytes >= 0 ? (int64_t)bytes : -1, SDL_GetTicks());
  statsAndroid.bytes = bytes >= 0 ? (long long)bytes : -1;
  statsAndroid.bufferSeg = bufferMs >= 0 ? bufferMs / 1000.0 : -1.0;
  statsAndroid.bufferAlvoSeg = alvoMs > 0 ? alvoMs / 1000.0 : 0.0;
  statsAndroid.conexoes = conexoes;
  statsAndroid.primeiroByteMs = primeiroByteMs;
  statsAndroid.quadrosPerdidos = perdidos;
  statsAndroid.quadros = quadros;
  snprintf(statsAndroid.host, sizeof statsAndroid.host, "%s", h);
  statsValido = 1;
  sinal = redeFit.sinal;
  redeMinMs = minMs; redeMaxMs = alvoMs; redeMaxMb = alvoMb;
  if (redefit_segundo(&redeFit, bytes, carregando, redeKbps, &mbps)) {
    redeMbps = mbps;
    /* English, numbers only: never URL, host or token. */
    printf("[rede] mbps=%.1f loading_s=%d/%d buffer_ms=%d declared_kbps=%d min_ms=%d max_ms=%d max_mb=%d sample_allocated_bytes=%d sample_peak_bytes=%d rebuffers=%d short=%d\n",
           mbps, redeFit.cargaUlt, REDEFIT_JANELA_S, (int)bufferMs, redeKbps, (int)minMs, (int)alvoMs,
           (int)alvoMb, memoria, pico, redeFit.rebufs, redeFit.baixas);
  }
  if (redeFit.sinal && !sinal)
    printf("[stream_fit] playback flagged reason=throughput mbps=%.1f declared_kbps=%d\n", mbps, redeKbps);
  pthread_mutex_unlock(&travaStats);
  fflush(stdout);
}
// 1 = this open's source cannot keep up (app.c tentarProximaFonteVOD).
int video_rede_lenta(void) {
  int s;
  pthread_mutex_lock(&travaStats); s = redeFit.sinal; pthread_mutex_unlock(&travaStats);
  return s;
}
void video_estatisticas(VideoStats *s) {
  pthread_mutex_lock(&travaStats);
  if (statsValido && ativo) *s = statsAndroid;
  else video_stats_vazio(s);
  pthread_mutex_unlock(&travaStats);
}
/* Real HTTP socket bytes only; no HLS placeholder, media Mbps or synthetic %. */
double video_download_bps(void) {
  double speed = -1.0;
  pthread_mutex_lock(&travaStats);
  if (ativo && medidorDownload.valido &&
      SDL_GetTicks() - medidorDownload.emAnterior <= 3000u)
    speed = medidorDownload.bps;
  pthread_mutex_unlock(&travaStats);
  return speed;
}

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeRetomada(JNIEnv *env, jclass cls, jint geracao, jint aceita) {
  (void)env; (void)cls;
  pthread_mutex_lock(&travaRetomada);
  if ((unsigned)geracao == sessao) retomadaInicialEstado = aceita ? 1 : -1;
  pthread_mutex_unlock(&travaRetomada);
}
int video_retomada_inicial_estado(void) {
  int estado;
  pthread_mutex_lock(&travaRetomada);
  estado = retomadaInicialEstado;
  pthread_mutex_unlock(&travaRetomada);
  return estado;
}
static unsigned novaRetomada(int estado) {
  unsigned geracao;
  pthread_mutex_lock(&travaRetomada);
  if (++sessao == 0) sessao++;
  geracao = sessao; retomadaInicialEstado = estado; liveRecuperandoAte = 0; liveSegmentoMs = 0;
  posMs = posNativoMs = seekRevision = 0;
  vodBufferFimMs = 0;
  livePosMs = liveBufferFimMs = 0; liveProgressoValido = liveBufferValido = 0;
  sampleBytes = samplePeakBytes = -1;
  pthread_mutex_unlock(&travaRetomada);
  return geracao;
}
static void enviarBusca(int ms) {
  JNIEnv *env = ambiente();
  if (!env || !mBuscar) return;
  pthread_mutex_lock(&travaRetomada);
  int revision = seekRevision = seekRevision == INT_MAX ? 1 : seekRevision + 1;
  vodBufferFimMs = 0;
  pthread_mutex_unlock(&travaRetomada);
  (*env)->CallStaticVoidMethod(env, gCls, mBuscar, (jint)ms, (jint)revision);
  fimChamada(env);
}
// StreamFit (F03): the session the Kotlin player received with its open; the
// passive telemetry gate (streamfitpassiva.c) only accepts this generation.
unsigned video_android_sessao(void) {
  unsigned g;
  pthread_mutex_lock(&travaRetomada);
  g = sessao;
  pthread_mutex_unlock(&travaRetomada);
  return g;
}
static void estadoRetomada(int estado) {
  pthread_mutex_lock(&travaRetomada);
  retomadaInicialEstado = estado;
  pthread_mutex_unlock(&travaRetomada);
}

// Capacidade da TELA (Display.getHdrCapabilities), lida no onCreate: so o
// formato preferido entre resolucoes iguais depende dela (streams.c).
// -1 = o aparelho nao informou (lista vazia): desconhecido nao penaliza.
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeTela(JNIEnv *env, jclass cls, jint hdr, jint dv) {
  (void)env; (void)cls;
  stream_definir_tela(hdr, dv);
  printf("[video] android tela hdr=%d dv=%d\n", (int)hdr, (int)dv);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeDecoder4k(JNIEnv *env, jclass cls,
                                                                                  jint hevc, jint avc, jint vp9, jint av1) {
  (void)env; (void)cls;
  stream_definir_decoder4k(hevc, avc, vp9, av1);
  printf("[tv] decoder 4K: hevc=%d avc=%d vp9=%d av1=%d\n", (int)hevc, (int)avc, (int)vp9, (int)av1);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeErroTexto(JNIEnv *env, jclass cls, jstring texto) {
  char t[512];
  (void)cls;
  deJString(env, texto, t, sizeof t);
  printf("[video] android erro: %s\n", t);
  fflush(stdout);
}

// #345: keep audio diagnostics in the uploaded app log, not only logcat.
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeAudioLog(JNIEnv *env, jclass cls, jstring texto) {
  char t[512];
  (void)cls;
  deJString(env, texto, t, sizeof t);
  printf("[video] android audio: %s\n", t);
  fflush(stdout);
}

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeHdr(JNIEnv *env, jclass cls, jstring hdr, jint dv, jint atmos) {
  char h[24];
  (void)cls;
  deJString(env, hdr, h, sizeof h);
  // So literais: video_hdr() devolve o ponteiro cru ao fio do app.
  hdrAtual = !strcmp(h, "HDR10") ? "HDR10" : !strcmp(h, "HLG") ? "HLG" :
             !strcmp(h, "DolbyVision") ? "DolbyVision" : "none";
  dvAtual = dv != 0;
  atmosAtual = atmos != 0;
  printf("[video] android hdr=%s dv=%d atmos=%d\n", (const char *)hdrAtual, dvAtual, atmosAtual);
  fflush(stdout);
}

// F07: what the Kotlin player did with the seek cache in this open. Kotlin
// main thread; cacheboost locks. (The audio output state for the boost comes
// from F06's nativeAudioEstado, below: one detection for both features.)
JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeCache(JNIEnv *env, jclass cls, jint estado, jint pedidoMb, jint limiteMb, jint usadoMb) {
  static int ultimo = -1;
  (void)env; (void)cls;
  cacheboost_cache_relato(estado, pedidoMb, limiteMb, usadoMb);
  if (estado != ultimo) {
    ultimo = estado;
    printf("[video] android cache: estado %d, pedido %d MB, limite %d MB\n", (int)estado, (int)pedidoMb, (int)limiteMb);
    fflush(stdout);
  }
}

JNIEXPORT jboolean JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeAfr(JNIEnv *env, jclass cls) {
  (void)env; (void)cls;
  return ajustes_afr();
}

JNIEXPORT jboolean JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeTunneling(JNIEnv *env, jclass cls) {
  (void)env; (void)cls;
  return ajustes_tunneling();
}

JNIEXPORT jint JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeLimitedStatus(JNIEnv *env, jclass cls) {
  XtreamConta c;
  (void)env; (void)cls;
  if (!xtream_conexoes_limitadas()) return 0;
  xtream_conta_ler(&c);
  return c.http == 403 || c.http == 458 || c.http == 509 ? c.http :
      c.valido && c.maxConexoes > 0 && c.conexoes >= c.maxConexoes ? 458 : 0;
}

enum { EV_PRONTO = 1, EV_TOCANDO = 2, EV_PAUSADO = 3, EV_FIM = 4, EV_ERRO = 5,
       EV_TAMANHO = 6, EV_BUFFER = 7, EV_PRIMEIRO_QUADRO = 8, EV_AUDIO_SEM_DECODER = 9,
       EV_SUPERFICIE_ESTAVEL = 10, EV_LIVE_RECOVERY = 11, EV_LIVE_SEGMENT = 12,
       EV_CARGA_FASE = 13, EV_SO_AUDIO = 14, EV_SUPERFICIE_INVALIDADA = 15 };

JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativeEvento(JNIEnv *env, jclass cls,
    jint tipo, jint a, jint b, jint geracao) {
  (void)env; (void)cls;
  // Every Media3 playback event belongs to one opening, not just position
  // samples. A delayed READY/ERROR/PAUSE from the old player must never alter
  // the state or rescue/error the newly selected source.
  pthread_mutex_lock(&travaEvento);
  pthread_mutex_lock(&travaRetomada);
  int atual = geracao > 0 && videoerro_p5b_evento_aceito(ativo, (unsigned)geracao,
                                                       sessao, terminalVideoP5B);
  pthread_mutex_unlock(&travaRetomada);
  if (!atual) { pthread_mutex_unlock(&travaEvento); return; }
  switch (tipo) {
    case EV_PRONTO:  if (!prontoLoad) marco("video: pronto (android)"); durMs = a; prontoLoad = 1; break;
    case EV_TOCANDO: tocando = 1; bufferando = 0; pausaVista = 0; if (!tocandoDesde) tocandoDesde = SDL_GetTicks() | 1; break;
    case EV_PAUSADO: tocando = 0; if (pausaPedida) pausaVista = 1; break;
    case EV_FIM:     terminou = 1; tocando = 0; break;
    // Sem `falhou` aqui: o video_bombear decide entre reconectar e desistir.
    case EV_ERRO:    if (b == 1) { /* Media3 audio renderer: do not retry as a network stall. */
                       semDecoderAudio = 1; reconErroPend = 0; tocando = 0;
                       printf("[video] android: erro terminal de audio %d\n", a);
                       break;
                     }
                     reconErroCod = a; reconErroVideo = b == 2; /* Media3 C.TRACK_TYPE_VIDEO */
                     reconErroPend = 1; tocando = 0;
                     snprintf(erroTxt, sizeof erroTxt, "%d player error:%d", a, b);
                     printf("[video] android: erro do player %d (%d)\n", a, b); break;
    case EV_TAMANHO: largura = a; altura = b; break;
    case EV_BUFFER:
      if (a < 100) {
        pthread_mutex_lock(&travaStats);
        int antes = redeFit.sinal;
        if (!bufferando && prontoLoad && tocandoDesde && SDL_GetTicks() - acaoMs > 3000u)
          redefit_rebuffer(&redeFit, SDL_GetTicks());
        printf("[rede] buffering mbps=%.1f loading_s=%d/%d buffer_ms=%d declared_kbps=%d min_ms=%d max_ms=%d max_mb=%d rebuffers=%d\n",
               redeMbps, redeFit.cargaUlt, REDEFIT_JANELA_S,
               statsValido ? (int)(statsAndroid.bufferSeg * 1000.0) : -1,
               redeKbps, redeMinMs, redeMaxMs, redeMaxMb, redeFit.rebufs);
        if (redeFit.sinal && !antes)
          printf("[stream_fit] playback flagged reason=rebuffer rebuffers=%d declared_kbps=%d\n",
                 redeFit.rebufs, redeKbps);
        pthread_mutex_unlock(&travaStats);
        fflush(stdout);
      }
      if (a < 100 && !bufferando) {
        bufferando = 1; bufferDesde = SDL_GetTicks();
      }
      else if (a >= 100) bufferando = 0;
      break;
    case EV_PRIMEIRO_QUADRO: marco("video: primeiro quadro (android)"); primeiroQuadro = quadroSuperficie = 1; break;
    case EV_SUPERFICIE_ESTAVEL: superficieEstavel = 1; break;
    case EV_SUPERFICIE_INVALIDADA: superficieEstavel = 0; quadroSuperficie = 0; break; // old surface frame cannot authorize new handoff
    case EV_AUDIO_SEM_DECODER: semDecoderAudio = 1; break;
    case 16: /* EV_AUDIO_SAIDA_INCOMPATIVEL: current route or actual sink rejected. */
      semDecoderAudio = 1; tocando = 0; break;
    case 17: /* EV_VIDEO_FALHA: terminal video capability/renderer error. */
      reconErroCod = b; reconErroVideo = 1; reconErroPend = 0;
      // A terminal video error cannot expose a previously rendered frame
      // as proof of current playback readiness.
      terminalVideoP5B = a > 0 ? a : 1;
      tocando = bufferando = primeiroQuadro = quadroSuperficie = superficieEstavel = 0;
      prontoLoad = somenteAudio = 0; falhou = 1;
      snprintf(erroTxt, sizeof erroTxt, "%d video playback error:%d", b, a);
      break;
    case EV_CARGA_FASE: if (a >= 0 && a <= 3) cargaFase = a; break;
    case EV_SO_AUDIO: if (a == 1) somenteAudio = 1; break;
    case EV_LIVE_SEGMENT:
      pthread_mutex_lock(&travaRetomada);
      if (ativo && (unsigned)b == sessao) liveSegmentoMs = a > 60000 ? 60000u : a > 0 ? (unsigned)a : 0u;
      pthread_mutex_unlock(&travaRetomada);
      break;
    case EV_LIVE_RECOVERY:
      pthread_mutex_lock(&travaRetomada);
      if (ativo && (unsigned)b == sessao) {
        unsigned prazo = a > 60000 ? 60000 : a > 0 ? (unsigned)a : 0;
        liveRecuperandoAte = prazo ? SDL_GetTicks() + prazo : 0;
        if (prazo && !liveRecuperandoAte) liveRecuperandoAte = 1;
      }
      pthread_mutex_unlock(&travaRetomada);
      break;
    default: break;
  }
  if (tipo != EV_BUFFER) { printf("[video] android evento %d (%d, %d)\n", tipo, a, b); fflush(stdout); }
  pthread_mutex_unlock(&travaEvento);
}

// --- video.h (C -> Kotlin) -----------------------------------------------------
int  video_iniciar(void) {
  JNIEnv *env;
  if (!travaLeg) travaLeg = SDL_CreateMutex();
  env = (JNIEnv *)SDL_AndroidGetJNIEnv();
  return env && garantirPonte(env);
}
int  video_iniciar_auto(void) { return video_iniciar(); }
int  video_registro_negado(void) { return 0; }

// Abre urlAtual no Kotlin. Serve a fonte nova e ao recarregar da reconexao.
static int abrirSessao(int inicioMs, int fracao, int mesmaFonte) {
  unsigned preparado = preparoAdotar;
  preparoAdotar = 0;
  JNIEnv *env;
  pthread_mutex_lock(&travaEvento);
  ativo = 1; superficieEstavel = quadroSuperficie = 0; prontoLoad = primeiroQuadro = falhou = terminou = tocando = 0;
  cargaFase = somenteAudio = 0;
  velEnviada = 100;
  largura = altura = durMs = posMs = posNativoMs = 0; bufferando = 1; bufferDesde = SDL_GetTicks();
  tocandoDesde = 0; semDecoderAudio = 0; erroTxt[0] = 0; reconErroVideo = 0; terminalVideoP5B = 0;
  pausaPedida = pausaVista = 0;
  hdrAtual = "none"; dvAtual = atmosAtual = 0;
  pthread_mutex_lock(&travaStats); statsValido = 0;
  nv_download_zerar(&medidorDownload);
  redeMbps = -1; redeMinMs = redeMaxMs = redeMaxMb = -1;
  memset(&redeFit, 0, sizeof redeFit); redeKbps = playerSessao ? stream_kbps_atual() : 0;
  pthread_mutex_unlock(&travaStats);
  nAudio = nLeg = 0; audioAtual = 0; legAtual = -1; legAte = 0;
  nNovasA = nNovasL = 0;
  temEnv = 0;   // a SurfaceView nova ainda nao recebeu janela nenhuma
  if (!travaLeg) travaLeg = SDL_CreateMutex();
  unsigned geracao = novaRetomada(-1);
  pthread_mutex_unlock(&travaEvento);
  memset(&reconBufferAvanco, 0, sizeof reconBufferAvanco);
  env = ambiente();
  if (!env) { falhou = 1; printf("[video] android: NvPlayer indisponivel\n"); fflush(stdout); return 0; }
  enviarPerfil(env, playerSessao ? player_buffer_average(urlAtual, cabecalhos, 0) : 0);
  // F07: the cache limit is sticky in Kotlin (read at each open), so it only
  // crosses JNI when it changes. Posted before the open on the same main
  // thread queue, it is in place when the open runs.
  // #338: buffer MiB (0 = Auto), sticky in Kotlin like the cache limit.
  if (mBuffer && bufferPreset != bufferEnviado) {
    (*env)->CallStaticVoidMethod(env, gCls, mBuffer, (jint)bufferPreset);
    fimChamada(env);
    bufferEnviado = bufferPreset;
  }
  if (mKbps && redeKbps != kbpsEnviado) {
    (*env)->CallStaticVoidMethod(env, gCls, mKbps, (jint)redeKbps);
    fimChamada(env);
    kbpsEnviado = redeKbps;
  }
  if (mCache && cacheSessao != cacheEnviado) {
    (*env)->CallStaticVoidMethod(env, gCls, mCache, (jint)cacheSessao);
    fimChamada(env);
    cacheEnviado = cacheSessao;
  }
  if (mTitulo && strcmp(tituloSessao, tituloEnviado)) {
    jstring t = paraJString(env, tituloSessao);
    if (t) {
      (*env)->CallStaticVoidMethod(env, gCls, mTitulo, t);
      (*env)->DeleteLocalRef(env, t);
      fimChamada(env);
      snprintf(tituloEnviado, sizeof tituloEnviado, "%s", tituloSessao);
    }
  }
  {
    jstring u = paraJString(env, nv_media_url(urlAtual));
    jstring c = u && !(*env)->ExceptionCheck(env) ? paraJString(env, cabecalhos) : NULL;
    if (!u || !c) {
      if (u) (*env)->DeleteLocalRef(env, u);
      if (c) (*env)->DeleteLocalRef(env, c);
      fimChamada(env);
      if (preparado) video_precarregar_cancelar(preparado);
      falhou = 1; return 0;
    }
    // abrirPosicao also when starting at 0: it is how the Kotlin side learns
    // this session's generation (StreamFit passive telemetry). At 0 the
    // resume state stays -1, exactly as the plain abrir path left it.
    if (preparado && mAbrirPreparada) {
      jstring a = paraJString(env, ling_audio());
      if (!a) {
        (*env)->DeleteLocalRef(env, u); (*env)->DeleteLocalRef(env, c);
        fimChamada(env);
        video_precarregar_cancelar(preparado);
        falhou = 1; return 0;
      }
      if (inicioMs > 0 || fracao > 0) estadoRetomada(0);
      (*env)->CallStaticVoidMethod(env, gCls, mAbrirPreparada, u, c,
          (jint)inicioMs, (jint)fracao, (jint)geracao, (jint)preparado, a);
      (*env)->DeleteLocalRef(env, a);
      if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        estadoRetomada(-1);
        (*env)->CallStaticVoidMethod(env, gCls, mAbrir, u, c);
      }
    } else if (inicioMs <= 0 && fracao > 0 && mAbrirRetomada) {
      estadoRetomada(0);
      (*env)->CallStaticVoidMethod(env, gCls, mAbrirRetomada, u, c, (jint)0, (jint)fracao, (jint)geracao);
      if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        estadoRetomada(-1);
        printf("[video] retomada: abrirRetomada recusado, abre do inicio e o player busca depois\n");
        fflush(stdout);
        (*env)->CallStaticVoidMethod(env, gCls, mAbrir, u, c);
      }
    } else if (mAbrirPosicao || (mesmaFonte && mAbrirRecuperacao)) {
      if (fracao > 0 && inicioMs <= 0) {
        printf("[video] retomada: casca sem abrirRetomada, o player busca depois do pronto\n");
        fflush(stdout);
      }
      if (inicioMs > 0) estadoRetomada(0);
      // Recovery-only optional ABI; manual release changes use normal open.
      (*env)->CallStaticVoidMethod(env, gCls,
          mesmaFonte && mAbrirRecuperacao ? mAbrirRecuperacao : mAbrirPosicao,
          u, c, (jint)inicioMs, (jint)geracao);
      if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        estadoRetomada(-1);
        (*env)->CallStaticVoidMethod(env, gCls, mAbrir, u, c);
      }
    } else (*env)->CallStaticVoidMethod(env, gCls, mAbrir, u, c);
    (*env)->DeleteLocalRef(env, u);
    (*env)->DeleteLocalRef(env, c);
  }
  int chamadaFalhou = (*env)->ExceptionCheck(env);
  fimChamada(env);
  if (chamadaFalhou) { falhou = 1; return 0; }
  // Every Kotlin open starts at 100%: a player session re-applies its volume
  // (source change, reconnection) after the open in the same queue.
  ganhoEnviado = CB_VOL_NORMAL;
  // Numero medido na TV TCL de 2,4 GB: 217 MB de textura + 96 MB de buffer +
  // paralelo/preparo levaram o app a ~575 MB de PSS e o lmkd matou. So o
  // player em tela cheia esconde a arte da home; trailers (home/detalhe) nao.
  // Ligado so depois de abrir: falha no meio nao deixa o teto preso.
  tex_pressao_reproducao(playerSessao);
  if (playerSessao && cacheboost_volume() != CB_VOL_NORMAL) cacheboost_backend_ganho(cacheboost_volume());
  return 1;
}

int video_tocar_retomada(const char *u, double segundos, double pct) {
  static int limitedEnviado;
  int limited = xtream_url_limitada(u);
  if (mLimitedConnections && limited != limitedEnviado) {
    kInt(mLimitedConnections, limited); limitedEnviado = limited;
  }
  if (limited && !mLimitedConnections) return 0; // Older shells cannot enforce this policy.
  int inicioMs = isfinite(segundos) && segundos > 0.0 && segundos <= INT_MAX / 1000.0
    ? (int)(segundos * 1000.0) : 0;
  // Centesimos de ponto percentual; segundos exatos vencem (abrirPosicao).
  int fracao = !inicioMs && isfinite(pct) && pct > 0.0 && pct < 100.0
    ? (int)(pct * 100.0 + 0.5) : 0;
  snprintf(urlAtual, sizeof urlAtual, "%s", u ? u : "");
  if (limited) capmkv_zerar(); else capmkv_iniciar(urlAtual);
  nv_recon_zerar(&recon);
  reconPermitida = reconProxima; reconProxima = 0;
  reconIniciou = 0; reconErroPend = 0;
  reconAudio = reconLeg = -1; reconFaixasPend = 0; reconBuscarMs = -1; reconPausado = 0;
  recarregarPausa = -1;
  playerSessao = cacheArmado >= 0;
  cacheSessao = cacheArmado > 0 ? cacheArmado : 0;
  cacheArmado = -1;
  snprintf(tituloSessao, sizeof tituloSessao, "%s", playerSessao ? tituloArmado : "");
  tituloArmado[0] = 0;
  return abrirSessao(inicioMs, fracao, 0);
}
int video_tocar_posicao(const char *u, double segundos) { return video_tocar_retomada(u, segundos, 0.0); }

void cacheboost_backend_buffer(int mb) { bufferPreset = mb >= 0 && mb <= 256 ? mb : 0; }
void cacheboost_backend_cache(int mb) { cacheArmado = mb > 0 ? mb : 0; }
void cacheboost_backend_titulo(const char *id) { snprintf(tituloArmado, sizeof tituloArmado, "%s", id ? id : ""); }
void cacheboost_backend_identidade(const char *conta, int perfil, const char *catalogo,
                                   const char *addon, const char *torrent,
                                   int arquivo, unsigned long long bytes) {
  JNIEnv *env = ambiente();
  if (!env || !mIdentidade) return; // Older Kotlin shells: no durable reuse.
  jstring c = paraJString(env, conta ? conta : "");
  jstring t = paraJString(env, catalogo ? catalogo : "");
  jstring a = paraJString(env, addon ? addon : "");
  jstring h = paraJString(env, torrent ? torrent : "");
  if (c && t && a && h) {
    (*env)->CallStaticVoidMethod(env, gCls, mIdentidade, c, (jint)perfil,
        t, a, h, (jint)arquivo, (jlong)bytes);
  }
  if (c) (*env)->DeleteLocalRef(env, c);
  if (t) (*env)->DeleteLocalRef(env, t);
  if (a) (*env)->DeleteLocalRef(env, a);
  if (h) (*env)->DeleteLocalRef(env, h);
  fimChamada(env);
}
void cacheboost_backend_descartar(void) { if (mDescartar) kSemArg(mDescartar); }
void cacheboost_backend_morno(int ligado) { if (mMorno) kInt(mMorno, ligado); }
int cacheboost_backend_quadro(int acao) {
  JNIEnv *env = ambiente(); int r = 0;
  if (env && mQuadro) { r = (*env)->CallStaticIntMethod(env, gCls, mQuadro, acao); fimChamada(env); }
  return r;
}
void cacheboost_backend_ganho(int pct) {
  if (pct < CB_VOL_MIN) pct = CB_VOL_MIN;
  if (pct > CB_VOL_MAX) pct = CB_VOL_MAX;
  if (!mGanho || !ativo || pct == ganhoEnviado) return;
  ganhoEnviado = pct;
  kInt(mGanho, pct);
}
void cacheboost_backend_audioajuste(int amplificacaoDb, int baixoDb, int centroDb, int noite) {
  JNIEnv *env = ambiente();
  if (!env || !mAudioAjustes) return;
  (*env)->CallStaticVoidMethod(env, gCls, mAudioAjustes,
    (jint)amplificacaoDb, (jint)baixoDb, (jint)centroDb, (jint)noite);
  fimChamada(env);
}
int video_tocar(const char *u) { return video_tocar_posicao(u, 0.0); }

void video_definir_reconexao(int sim) { reconProxima = sim ? 1 : 0; }
int video_recarregar(void) {
  int alvo = posNativoMs, pausado = pausaPedida;
  if (!ativo || !prontoLoad || !urlAtual[0] || falhou || terminou ||
      reconErroPend || video_reconectando() || recarregarPausa >= 0 ||
      alvo < 0 || !ambiente()) return 0;
  reconAudio = audioAtual; reconLeg = legAtual; reconFaixasPend = 1;
  reconBuscarMs = -1;
  recarregarPosMs = alvo;
  if (!abrirSessao(alvo, 0, 1)) { recarregarPausa = -1; return 0; }
  recarregarPausa = pausado;
  // As duas chamadas entram na mesma fila do Kotlin: a pausa pertence ao
  // player novo, antes do prepare completar, sem alterar o audio preferido.
  if (pausado) video_pausar(1);
  return 1;
}
static int retryMesmoSourceP5(void) {
  /* Explicit Retry on Q36's audio card; never choose a different URL/release.
   * This calls abrirSessao, which advances sessao before any old callbacks
   * can be applied and retains the Kotlin source identity. The UI allows at
   * most one retry per selected stream. */
  int posicao = posNativoMs > 0 ? posNativoMs : 0;
  if (!ativo || !urlAtual[0] || !ambiente()) return 0;
  reconAudio = audioAtual; reconLeg = legAtual; reconFaixasPend = 1;
  reconErroPend = 0;
  nv_recon_zerar(&recon);
  reconBuscarMs = -1; recarregarPausa = -1; reconPausado = 0;
  return abrirSessao(posicao, 0, 1);
}
int video_audio_retry(void) { return retryMesmoSourceP5(); }
int video_video_retry(void) {
  int motivo = terminalVideoP5B;
  int posicao = posNativoMs, posVisivel = posMs, duracao = durMs;
  int codigo = reconErroCod;
  char motivoTexto[sizeof erroTxt];
  if (!motivo) return 0;
  snprintf(motivoTexto, sizeof motivoTexto, "%s", erroTxt);
  if (retryMesmoSourceP5()) return 1;
  // abrirSessao resets the terminal reason before making the JNI call.
  // If that call fails synchronously, retain ownership of the explicit
  // video-error card rather than exposing a generic source watchdog.
  pthread_mutex_lock(&travaEvento);
  terminalVideoP5B = motivo;
  reconErroCod = codigo; reconErroVideo = 1; reconErroPend = 0;
  posNativoMs = posicao; posMs = posVisivel; durMs = duracao;
  tocando = bufferando = 0; falhou = 1;
  snprintf(erroTxt, sizeof erroTxt, "%s", motivoTexto);
  pthread_mutex_unlock(&travaEvento);
  return 0;
}
int video_erro_video_p5b(void) { return ativo ? terminalVideoP5B : 0; }
int  video_reconectando(void) {
  return nv_recon_ativa(&recon) && (recon.pendente || !prontoLoad) ? recon.tentativa : 0;
}

void video_bombear(void) {
  // Once Q36's terminal audio error takes over, no watchdog may silently
  // launch another player generation. Only the user-owned Retry may reopen it.
  if (semDecoderAudio || terminalVideoP5B) {
    reconErroPend = 0; recarregarPausa = -1;
    nv_recon_zerar(&recon);
    return;
  }
  double pos = video_pos();
  int eraRecarga = recarregarPausa >= 0;
  if (recarregarPausa >= 0) {
    if (falhou || terminou || !ativo) recarregarPausa = -1;
    else if (prontoLoad && !reconErroPend && !recon.pendente) {
      // Cascas antigas podem recusar a posicao de abertura. Nesse caso a
      // mesma sessao recebe o seek, inclusive se a pessoa estava pausada.
      if (recarregarPosMs > 0 && video_retomada_inicial_estado() < 0)
        enviarBusca(recarregarPosMs);
      if (recarregarPausa) video_pausar(1);
      recarregarPausa = -1;
    }
  }
  if (mVelocidade && ativo && prontoLoad && velEnviada != velPedida) {
    velEnviada = velPedida;
    kInt(mVelocidade, velEnviada);
    // So "enviado": o setPlaybackSpeed nao recusa nada, e quem prova que andou
    // e o medidor do player (VelMedidor), pela posicao contra o relogio.
    vel_log(velEnviada, "enviado ao ExoPlayer");
  }
  if (prontoLoad && pos > 0.5) reconIniciou = 1;
  if (prontoLoad && reconBuscarMs < 0) nv_recon_progresso(&recon, pos);
  // O seek do recarregar sai com o player ja tocando.
  if (reconBuscarMs >= 0 && prontoLoad &&
      (video_retomada_inicial_estado() != 0 || tocando || reconPausado)) {
    if (video_retomada_inicial_estado() != 1) enviarBusca(reconBuscarMs);
    printf("[video] reconexao: retomado em %ds\n", reconBuscarMs / 1000);
    fflush(stdout);
    reconBuscarMs = -1;
  }
  if (!falhou && !reconPausado &&
      (!prontoLoad || reconBuscarMs >= 0 || (!pausaPedida && bufferando)) &&
      nv_recon_expirou(&recon, SDL_GetTicks())) {
    VideoStats v;
    video_estatisticas(&v);
    // An in-progress, slowly filling REMUX is not a hung recovery.
    if (!bufavanco_segura(&reconBufferAvanco, sessao,
                          SDL_GetTicks() - bufferDesde, SDL_GetTicks(),
                          bufavanco_borda(pos, v.bufferSeg))) {
      reconErroCod = 2002; reconErroPend = 1;
      snprintf(erroTxt, sizeof erroTxt, "2002 player error: recovery timeout");
    }
  }
  // A silent VOD stall also needs recovery when Media3 never emits an error.
  if (!falhou && reconPermitida && reconIniciou && !pausaPedida && !nv_recon_ativa(&recon) &&
      bufferando && SDL_GetTicks() - bufferDesde >= NV_RECON_PRAZO_MS) {
    VideoStats v;
    video_estatisticas(&v);
    // Large REMUX streams may take more than 30s to fill a safe buffer. Only
    // treat it as a stall if its buffered edge stopped advancing over 10s.
    if (!bufavanco_segura(&reconBufferAvanco, sessao,
                          SDL_GetTicks() - bufferDesde, SDL_GetTicks(),
                          bufavanco_borda(pos, v.bufferSeg))) {
      reconErroCod = 2002; reconErroPend = 1;
      snprintf(erroTxt, sizeof erroTxt, "2002 player error: stalled download");
    }
  }
  if (reconErroPend) {
    int antes = recon.tentativa;
    int rede = erroDeRede(reconErroCod);
    reconErroPend = 0;
    if (!(xtream_url_limitada(urlAtual) && (reconErroCod == 403 || reconErroCod == 458 || reconErroCod == 509)) &&
        reconPermitida && urlAtual[0] && (reconIniciou || recon.tentativa) &&
        nv_recon_erro(&recon, rede, SDL_GetTicks(),
                      eraRecarga ? recarregarPosMs / 1000.0 : pos)) {
      if (!antes && !eraRecarga) {
        reconAudio = audioAtual; reconLeg = legAtual;
        // P1 Q22: automatic same-source recovery starts playing even if the
        // old session was paused. A NEW pause/Back/background during recovery
        // overrides this through video_pausar() or video_parar().
        reconPausado = 0;
      }
      if (recon.tentativa != antes) {
        printf("[video] conexao caiu (%d): tentativa %d/%d, espera %us\n",
               reconErroCod, recon.tentativa, NV_RECON_MAX,
               nv_recon_espera_ms(recon.tentativa) / 1000u);
        fflush(stdout);
      }
      bufferando = 0;
    } else {
      if (recon.esgotou) { printf("[video] reconexao: desistiu depois de %d tentativas\n", NV_RECON_MAX); fflush(stdout); }
      falhou = 1;
    }
  }
  if (falhou) recarregarPausa = -1;
  // A new user pause (including Android background) parks the pending
  // restart. It can resume only after an explicit Play; Back fully cancels
  // the generation in video_parar().
  if (!reconPausado && nv_recon_vencida(&recon, SDL_GetTicks())) {
    printf("[video] reconectando: alvo %.0fs, tentativa %d\n", recon.alvo, recon.tentativa);
    fflush(stdout);
    reconFaixasPend = 1;
    reconBuscarMs = recarregarPausa < 0 && recon.alvo > 1.0 ? (int)(recon.alvo * 1000.0) : -1;
    if (!abrirSessao(recarregarPausa >= 0 ? recarregarPosMs :
                     (int)(recon.alvo * 1000.0), 0, 1))
      { reconErroCod = -1; reconErroPend = 1; }
    else if (recarregarPausa > 0) video_pausar(1);
    else if (recarregarPausa < 0 && reconPausado) {
      // abrirSessao nasce tocando. Enfileirar pause logo depois da abertura
      // conserva o estado de um seek feito pausado; o seek de retomada acima
      // aceita prontoLoad mesmo sem EV_TOCANDO.
      pausaPedida = 1; pausaVista = 0;
      kInt(mPausar, 1);
    }
  }
}
void video_parar(void) {
  p1FaixasPendentes = p1AudioValido = p1LegValida = 0;
  pthread_mutex_lock(&travaEvento);
  novaRetomada(-1);
  terminalVideoP5B = 0;
  pthread_mutex_unlock(&travaEvento);
  capmkv_zerar();   // fio de capitulos em voo nao alimenta o proximo titulo
  nv_recon_zerar(&recon);
  reconErroPend = 0; reconErroVideo = 0; terminalVideoP5B = 0; reconFaixasPend = 0; reconBuscarMs = -1; reconPausado = 0;
  recarregarPausa = -1;
  if (ativo) {
    // Troca de episodio: o consumidor ja destacou o proximo buffer. Soltar
    // o decoder atual nao pode cancelar esse preparo nem o token da abertura.
    JNIEnv *env = ambiente();
    if (preparoAdotar && env && mPararMantendoPreparo)
      kInt(mPararMantendoPreparo, (int)preparoAdotar);
    else { preparoAdotar = 0; kSemArg(mParar); }
  }
  ativo = prontoLoad = primeiroQuadro = tocando = 0; superficieEstavel = quadroSuperficie = 0;
  cargaFase = somenteAudio = 0;
  pausaPedida = pausaVista = 0;
  tex_pressao_reproducao(0);
}
void video_pausar(int p) {
  pausaVista = 0; pausaPedida = p ? 1 : 0;
  if (nv_recon_ativa(&recon)) reconPausado = pausaPedida;
  if (recarregarPausa >= 0) recarregarPausa = pausaPedida;
  kInt(mPausar, pausaPedida);
}
int video_pausa_confirmada(void) {
  return pausaPedida && pausaVista && !tocando && ativo && prontoLoad && primeiroQuadro &&
         !falhou && !terminou && !video_reconectando();
}

void video_volume(int pct) { kInt(mVolume, pct); }
void video_buscar(double s) {
  posMs = (int)(s * 1000.0);   // a barra nao pode voltar enquanto o seek corre
  acaoMs = SDL_GetTicks();
  enviarBusca(posMs);
  terminou = 0;
}
// `encaixa` = 1: o quadro ENCAIXA no retangulo mantendo a proporcao (tarja),
// que e o que o plano de video da LG e o LetterBox da Samsung fazem com uma
// janela lisa — o nucleo conta com isso (trailer "Original", janela antes do
// videoInfo). 0: o retangulo e exato, ja calculado com a proporcao (recorte).
// ULTIMA JANELA ENTREGUE (dedupe). O nucleo manda o retangulo de novo quando
// dois modos de aspecto coincidem no mesmo filme — Ajustar largura e Original
// num 2.39:1, por exemplo — e a cada releitura do videoInfo; o log da TCL
// (11/10) mostrou "1920x1037" pedido duas vezes seguidas. O Kotlin nao faria
// nada com o repetido (layout identico) e o dvtrace enchia de "changed" sem
// mudanca. O reaplicar do trailer chama janelaKt DIRETO, fora do dedupe: ele
// existe justamente para reenviar. abrirSessao zera — a SurfaceView de cada
// abertura comeca sem janela nenhuma e precisa receber ate o retangulo igual.
static void janelaKt(int x, int y, int w, int h, int encaixa) {
  JNIEnv *env = ambiente();
  if (!env) return;   // sem JNI nao ha entrega: o dedupe so lembra do que chegou
  envX = x; envY = y; envW = w; envH = h; envEnc = encaixa; temEnv = 1;
  (*env)->CallStaticVoidMethod(env, gCls, mJanela, (jint)x, (jint)y, (jint)w, (jint)h, (jint)encaixa);
  fimChamada(env);
}
static void janelaSeMudou(int x, int y, int w, int h, int encaixa) {
  if (temEnv && x == envX && y == envY && w == envW && h == envH && encaixa == envEnc) return;
  janelaKt(x, y, w, h, encaixa);
}
int video_janela_pronta(void) {
  JNIEnv *env = ambiente();
  if (!env || !temEnv || !mJanelaPronta) return 0;
  int pronta = (*env)->CallStaticBooleanMethod(env, gCls, mJanelaPronta);
  fimChamada(env);
  return pronta;
}
void video_janela(int x, int y, int w, int h) { janelaSeMudou(x, y, w, h, 1); }

// RECORTE DE FONTE PELO RETANGULO DE DESTINO AMPLIADO. Mesma conta do ROI do
// .tpk (video_tpk.c, #178): desenhar o recorte (sx,sy,sw,sh) dentro de
// (dx,dy,dw,dh) e o MESMO que desenhar o quadro INTEIRO num retangulo maior,
// deslocado para o pedaco desejado cair sobre o destino:
//   escala = dw/sw, dh/sh;  W,H = quadro * escala;  X,Y = d - s * escala.
// Aqui o retangulo pode sair da tela e ter origem negativa: o Kotlin aplica por
// layout na SurfaceView e o pai recorta o excedente, sem a restricao do Tizen.
static int ultX, ultY, ultW, ultH, temUlt;
void video_janela_fonte(int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
  double qw = largura, qh = altura, ex, ey;
  int X, Y, W, H;

  // Sem as dimensoes do quadro nao ha recorte a calcular: janela lisa (encaixa).
  if (qw < 2.0 || qh < 2.0 || sw <= 0 || sh <= 0) { temUlt = 0; video_janela(dx, dy, dw, dh); return; }
  // Quadro INTEIRO num destino que o chamador ja calculou (Original encaixado,
  // Esticar, Encaixar altura): o destino e EXATO, nao se encaixa de novo —
  // senao Esticar virava Original.
  if (sx <= 0 && sy <= 0 && sw >= (int)qw && sh >= (int)qh) { temUlt = 0; janelaSeMudou(dx, dy, dw, dh, 0); return; }

  ex = (double)dw / (double)sw;
  ey = (double)dh / (double)sh;
  W  = (int)(qw * ex + 0.5);
  H  = (int)(qh * ey + 0.5);
  X  = (int)(dx - sx * ex + (dx - sx * ex < 0 ? -0.5 : 0.5));
  Y  = (int)(dy - sy * ey + (dy - sy * ey < 0 ? -0.5 : 0.5));

  printf("[video] android recorte %d,%d %dx%d de %.0fx%.0f -> janela %d,%d %dx%d\n",
         sx, sy, sw, sh, qw, qh, X, Y, W, H);
  fflush(stdout);
  ultX = X; ultY = Y; ultW = W; ultH = H; temUlt = 1;
  janelaSeMudou(X, Y, W, H, 0);
}
int  video_recorte_fonte(void) { return 1; }
void video_recorte_reaplicar(void) { if (temUlt) janelaKt(ultX, ultY, ultW, ultH, 0); }
// O Kotlin escala 1920x1080 de layout para a camada real; nada a fazer aqui.
void video_escala_definir(int sw, int sh) { (void)sw; (void)sh; }

const char *video_url_atual(void) { return urlAtual; }
double video_pos(void) {
  // Reopening clears native position until READY; never publish zero as the
  // resume point to the source-renewal worker during that interval.
  return recon.tentativa && (!prontoLoad || reconBuscarMs >= 0 || recon.esgotou)
         ? recon.alvo : prontoLoad ? posMs / 1000.0 : 0;
}
double video_duracao(void) { return durMs / 1000.0; }
// Capitulos do MKV lidos por um fio lateral (capmkv.c, 203-capitulos).
double video_creditos(void) { return capmkv_creditos(video_duracao()); }
int video_live_progresso(double *pos, double *fim) {
  int valido;
  pthread_mutex_lock(&travaRetomada);
  valido = ativo && liveProgressoValido;
  if (valido) {
    *pos = livePosMs / 1000.0;
    *fim = liveBufferValido ? liveBufferFimMs / 1000.0 : 0.0;
  }
  pthread_mutex_unlock(&travaRetomada);
  return valido;
}
double video_buffer_fim(void) {
  pthread_mutex_lock(&travaRetomada);
  jlong end = liveProgressoValido ? liveBufferFimMs : vodBufferFimMs;
  double seconds = ativo && !falhou && end > 0 ? end / 1000.0 : 0.0;
  pthread_mutex_unlock(&travaRetomada);
  return seconds;
}
unsigned video_live_segmento_ms(void) {
  unsigned ms;
  pthread_mutex_lock(&travaRetomada);
  ms = ativo ? liveSegmentoMs : 0;
  pthread_mutex_unlock(&travaRetomada);
  return ms;
}
int video_recuperando_live(void) {
  int recuperando;
  pthread_mutex_lock(&travaRetomada);
  recuperando = ativo && liveRecuperandoAte && (Sint32)(liveRecuperandoAte - SDL_GetTicks()) > 0;
  pthread_mutex_unlock(&travaRetomada);
  return recuperando;
}
unsigned video_bufferando_ms(void) {
  // Esperando para reconectar: o watchdog (app.c) nao troca de fonte.
  if (nv_recon_ativa(&recon)) return 0;
  return bufferando ? SDL_GetTicks() - bufferDesde : 0;
}
void video_definir_dv(int dv) { (void)dv; }
void video_definir_cabecalhos(const char *c) { snprintf(cabecalhos, sizeof cabecalhos, "%s", c ? c : ""); }
const char *video_cabecalhos(void) { return cabecalhos; }
void video_definir_mp4(int m) { (void)m; }
int  video_tocando(void) { return tocando; }
// Media3 READY/playback clock alone never proves that a frame exists.
// Audio-only is allowed solely after the selected track-type confirmation.
int  video_pronto(void) {
  return ativo && !terminalVideoP5B && nv_p2d_media_ready(prontoLoad, primeiroQuadro, somenteAudio);
}
int  video_ativo(void) { return ativo; }
int  video_superficie_estavel(void) { return superficieEstavel; }
int  video_quadro_visto(void) { return ativo && quadroSuperficie; }
int  video_somente_audio(void) { return ativo && prontoLoad && somenteAudio; }
int  video_carga_fase(void) { return ativo ? cargaFase : 0; }
int  video_falhou(void) { return falhou; }
int  video_audio_nao_suportado(void) { return semDecoderAudio; }
int  video_seek_desistiu(void) { return 0; }
int  video_terminou(void) { return terminou; }
// Foco de audio perdido = pausa (NvPlayer); nao ha "outro app com o video".
int  video_conflito_recurso(void) { return conflito; }
const char *video_erro_texto(void) { return falhou ? erroTxt : ""; }
int video_erro_decoder_codigo(void) { return falhou && reconErroVideo ? reconErroCod : 0; }
// O Media3 nao da o sinal de decoder anunciado: valem os neutros do .tpk.
int  video_decoder_anunciou(void) { return 1; }

int  video_n_audio(void) { return nAudio; }
int  video_n_legenda(void) { return nLeg; }
const VideoFaixa *video_audio(int i) { return (i >= 0 && i < nAudio) ? &faixaAudio[i] : 0; }
const VideoFaixa *video_legenda(int i) { return (i >= 0 && i < nLeg) ? &faixaLeg[i] : 0; }
int  video_legenda_ordinal_mkv(int i) { return (i >= 0 && i < nLeg) ? faixaLeg[i].ordinalMkv : -1; }
// "Nao ha sonda": sem libass (NV_ASS_LIBASS) a sonda de MKV nao serve a nada.
// TODO quando o libass entrar: sonda real, como video.c.
int  video_mkv_sondado(void) { return 2; }
void video_sondar_mkv_agora(void) {}
int  video_audio_atual(void) { return audioAtual; }
int  video_legenda_atual(void) { return legAtual; }
void video_p1_guardar_faixas_manual(void) {
  const VideoFaixa *a = video_audio(audioAtual);
  const VideoFaixa *l = video_legenda(legAtual);
  p1AudioValido = a && a->idioma[0];
  p1LegValida = l && l->idioma[0];
  if (p1AudioValido) p1Audio = *a;
  if (p1LegValida) p1Leg = *l;
  p1FaixasPendentes = p1AudioValido || p1LegValida;
}

void video_escolher_audio(int i) {
  if (i < 0 || i >= nAudio) return;
  if (!faixaAudio[i].decodifica || !faixaAudio[i].saidaFiel) {
    semDecoderAudio = 1; // Manual choice is not permission for silent output.
    return;
  }
  audioAtual = i;
  acaoMs = SDL_GetTicks();   // the switch rebuffers: not a network stall
  {
    JNIEnv *env = ambiente();
    if (!env) return;
    (*env)->CallStaticVoidMethod(env, gCls, mEscolher, (jint)0, (jint)faixaAudio[i].numero);
    fimChamada(env);
  }
}
void video_escolher_legenda(int i) {
  if (i >= nLeg) return;
  legAtual = i;
  if (travaLeg) { SDL_LockMutex(travaLeg); legTexto[0] = 0; legAte = 0; SDL_UnlockMutex(travaLeg); }
  {
    JNIEnv *env = ambiente();
    if (!env) return;
    // -1 desliga a legenda no Kotlin.
    (*env)->CallStaticVoidMethod(env, gCls, mEscolher, (jint)1, (jint)(i >= 0 ? faixaLeg[i].numero : -1));
    fimChamada(env);
  }
}
int  video_legenda_nativa(char *d, int t) {
  if (!d || t < 2) return 0;
  d[0] = 0;
  if (!ativo || legAtual < 0 || !travaLeg) return 0;
  SDL_LockMutex(travaLeg);
  if (legTexto[0] && (Sint32)(legAte - SDL_GetTicks()) > 0) snprintf(d, (size_t)t, "%s", legTexto);
  SDL_UnlockMutex(travaLeg);
  return d[0] != 0;
}
void video_legenda_externa(const char *u) { (void)u; }
// O atraso do estilo vale para a embutida: o Kotlin guarda e adia os cues
// (so atraso positivo; adiantar um cue que ainda nao chegou nao da).
void video_legenda_estilo(const VideoLegendaEstilo *e) {
  static int ultAtraso = 0x7fffffff;
  if (!e || e->atrasoMs == ultAtraso) return;
  ultAtraso = e->atrasoMs;
  {
    JNIEnv *env = ambiente();
    if (!env) return;
    (*env)->CallStaticVoidMethod(env, gCls, mEscolher, (jint)2, (jint)e->atrasoMs);
    fimChamada(env);
  }
}
int  video_tem_atmos(void) { return atmosAtual; }
int  video_tem_dolby_vision(void) { return dvAtual; }
const char *video_hdr(void) { return (const char *)hdrAtual; }
int  video_largura(void) { return largura; }
int  video_altura(void) { return altura; }
int  video_pode_forcar_sdr(void) { return 0; }
int  video_velocidade_suportada(void) { return mVelocidade != NULL && !velRecusada; }
void video_velocidade(int c) { if (!velRecusada) velPedida = (c <= 0 || c > 400) ? 100 : c; }
int  video_velocidade_atual(void) { return velRecusada ? 100 : velPedida; }
void video_velocidade_recusada(void) { velRecusada = 1; velPedida = 100; }   // o bombear manda o 100
// The Android bridge decodes to PCM at non-normal speeds, restoring encoded
// output eligibility at 1x. VelMedidor still verifies the actual media clock.
int  video_velocidade_bloqueada(void) { return 0; }
void video_forcar_sdr(void) {}
void video_encerrar(void) { video_parar(); }
#endif
