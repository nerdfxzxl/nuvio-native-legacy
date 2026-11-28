#ifndef NV_PLAYBACK_UI_METRICS_H
#define NV_PLAYBACK_UI_METRICS_H
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <SDL2/SDL.h>
#define NV_UI_METRIC_N 6
#define NV_UI_DETAILS_N 14
#define NV_UI_DETAIL_LEN 160
#if defined(NV_ANDROID) && defined(__ANDROID__)
#include <jni.h>
#include <SDL2/SDL_system.h>
static inline int nv_ui_memory(int64_t out[NV_UI_METRIC_N]) {
  static int64_t cache[NV_UI_METRIC_N];
  static Uint32 last;
  static int valid;
  Uint32 now=SDL_GetTicks();
  if (valid && now-last<1000u) { memcpy(out,cache,sizeof cache); return 1; }
  JNIEnv *env=SDL_AndroidGetJNIEnv();
  if (!env || (*env)->PushLocalFrame(env,8)<0) return 0;
  jobject act=SDL_AndroidGetActivity();
  jclass cls=act?(*env)->GetObjectClass(env,act):NULL;
  jmethodID mid=cls?(*env)->GetMethodID(env,cls,"playbackUiMemory","()[J"):NULL;
  jlongArray a=mid?(*env)->CallObjectMethod(env,act,mid):NULL;
  int ok=0;
  if (!(*env)->ExceptionCheck(env) && a && (*env)->GetArrayLength(env,a)==NV_UI_METRIC_N) {
    jlong data[NV_UI_METRIC_N];
    (*env)->GetLongArrayRegion(env,a,0,NV_UI_METRIC_N,data);
    if (!(*env)->ExceptionCheck(env)) {
      for (int i=0;i<NV_UI_METRIC_N;i++) out[i]=data[i];
      memcpy(cache,out,sizeof cache); last=now; valid=ok=1;
    }
  }
  if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
  (*env)->PopLocalFrame(env,NULL);
  return ok;
}
static inline void nv_ui_details(char out[NV_UI_DETAILS_N][NV_UI_DETAIL_LEN]) {
  memset(out,0,NV_UI_DETAILS_N*NV_UI_DETAIL_LEN);
  JNIEnv *env=SDL_AndroidGetJNIEnv();
  if (!env || (*env)->PushLocalFrame(env,8)<0) return;
  jobject act=SDL_AndroidGetActivity();
  jclass cls=act?(*env)->GetObjectClass(env,act):NULL;
  jmethodID mid=cls?(*env)->GetMethodID(env,cls,"playbackUiDetails","()[Ljava/lang/String;"):NULL;
  jobjectArray a=mid?(*env)->CallObjectMethod(env,act,mid):NULL;
  if (!(*env)->ExceptionCheck(env) && a && (*env)->GetArrayLength(env,a)==NV_UI_DETAILS_N) {
    for (int i=0;i<NV_UI_DETAILS_N;i++) {
      jstring s=(*env)->GetObjectArrayElement(env,a,i);
      if ((*env)->ExceptionCheck(env)) break;
      if (s) {
        const char *v=(*env)->GetStringUTFChars(env,s,NULL);
        if (v) { snprintf(out[i],NV_UI_DETAIL_LEN,"%s",v); (*env)->ReleaseStringUTFChars(env,s,v); }
        (*env)->DeleteLocalRef(env,s);
      }
    }
  }
  if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
  (*env)->PopLocalFrame(env,NULL);
}
#else
static inline int nv_ui_memory(int64_t out[NV_UI_METRIC_N]) { (void)out; return 0; }
static inline void nv_ui_details(char out[NV_UI_DETAILS_N][NV_UI_DETAIL_LEN]) {
  memset(out,0,NV_UI_DETAILS_N*NV_UI_DETAIL_LEN);
}
#endif
// Preview requested RAM against the existing hard cap + read-ahead budget.
// Auto is the maximum planned buffer (bitrate unknown before playback).
// Manual is the requested value, BEFORE the engine's existing safety clamp.
static inline int nv_ui_buffer_estimate(int64_t cap, int64_t parallel, int preset,
                                        int enabled, int count, int chunkMb,
                                        int *requestedMb, int *limitMb) {
  if (cap<=0 || parallel<0) return 0;
  if (count<1) count=1;
  int64_t chunk=(int64_t)chunkMb*1048576;
  int64_t bounded=parallel/(count+1);
  if (chunk>bounded) chunk=bounded;
  if (chunk<131072) chunk=131072;
  int64_t overhead=enabled?chunk*(count+1):0;
  int64_t requested=preset>0?(int64_t)preset*1048576:cap;
  *requestedMb=(int)((requested+overhead+1048575)/1048576);
  int64_t totalLimit=cap+(enabled?parallel:0);
  *limitMb=(int)((totalLimit+1048575)/1048576);
  // Flag the requested buffer against its own cap as well as the total.
  return requested>cap || requested+overhead>totalLimit ? -1 : 1;
}
#endif
