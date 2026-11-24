// Exercise the production JNI entry points. Replace the source while JNI is
// copying the old callback's string, after the Kotlin listener checked owner.
#define NV_ANDROID 1
#include <SDL2/SDL.h>
void *SDL_AndroidGetJNIEnv(void);
void *SDL_AndroidGetActivity(void);
#include "../src/video_android.c"
#include <assert.h>
#include <stdint.h>

static int replace_on_copy;
static const jchar value[] = {'o','l','d'};

static const jchar *JNICALL chars(JNIEnv *env, jstring text, jboolean *copy) {
  (void)env; (void)text; (void)copy;
  if (replace_on_copy) {
    pthread_mutex_lock(&travaEvento);
    novaRetomada(-1);
    pthread_mutex_unlock(&travaEvento);
    replace_on_copy = 0;
  }
  return value;
}
static jsize JNICALL length(JNIEnv *env, jstring text) {
  (void)env; (void)text; return 3;
}
static void JNICALL release(JNIEnv *env, jstring text, const jchar *v) {
  (void)env; (void)text; (void)v;
}
int main(void) {
  struct JNINativeInterface_ api = {0};
  api.GetStringChars = chars;
  api.GetStringLength = length;
  api.ReleaseStringChars = release;
  JNIEnv env = &api;
  jstring host = (jstring)(uintptr_t)1;
  ativo = 1;
  unsigned old = novaRetomada(-1);
  replace_on_copy = 1;
  Java_space_nuvio_nativelegacy_NvPlayer_nativeStats(&env, NULL,
      256, 800, 1200, 1, 22, 0, 30, host, 100, 10, 1, old);
  assert(!statsValido && !redeFit.armado);
  Java_space_nuvio_nativelegacy_NvPlayer_nativeStats(&env, NULL,
      512, 900, 1200, 1, 22, 0, 30, host, 100, 10, 1, sessao);
  assert(statsValido && statsAndroid.bytes == 512);
  travaLeg = SDL_CreateMutex();
  assert(travaLeg);
  old = sessao;
  replace_on_copy = 1;
  Java_space_nuvio_nativelegacy_NvPlayer_nativeLegenda(&env, NULL, host, 3000, old);
  assert(!legTexto[0] && !legAte);
  Java_space_nuvio_nativelegacy_NvPlayer_nativeLegenda(&env, NULL, host, 3000, sessao);
  assert(!strcmp(legTexto, "old") && legAte);
  SDL_DestroyMutex(travaLeg);
  puts("PASS: delayed subtitle publication belongs to its opening");
  puts("PASS: source replacement rejects late telemetry and accepts the new sample");
  return 0;
}
