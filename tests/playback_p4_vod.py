"""Compile the actual Android position callback, generation reset and rail getter on the host."""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
native = (root / "src/video_android.c").read_text()
kotlin = (root / "android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt").read_text()

def function(start):
    pos = native.index(start)
    return native[pos:native.index("\n}", pos) + 2]

assert '"buscar", "(II)V"' in native and 'fun buscar(ms: Int, revision: Int)' in kotlin
assert 'if (live || validWindow) p.bufferedPosition else C.TIME_UNSET' in kotlin
assert 'geracaoNative, appliedSeekRevision)' in kotlin
assert 'p.seekTo(alvo)\n                        appliedSeekRevision = revision' in kotlin

source = r"""
#include "vod_buffer.h"
#include <limits.h>
#include <pthread.h>
#include <assert.h>
#define JNIEXPORT
#define JNICALL
typedef void JNIEnv;
typedef void *jclass;
typedef int64_t jlong;
typedef int jboolean, jint;
static pthread_mutex_t travaRetomada = PTHREAD_MUTEX_INITIALIZER;
static unsigned sessao;
static int ativo=1, falhou, durMs, posMs, posNativoMs, seekRevision;
static int retomadaInicialEstado, liveRecuperandoAte, liveSegmentoMs, liveProgressoValido, liveBufferValido;
static int sampleBytes, samplePeakBytes;
static jlong livePosMs, liveBufferFimMs, vodBufferFimMs;
"""
source += function("JNIEXPORT void JNICALL Java_space_nuvio_nativelegacy_NvPlayer_nativePos(")
source += function("static unsigned novaRetomada(")
source += function("double video_buffer_fim(")
source += r"""
#define REPORT(pos,end,live,gen,rev) Java_space_nuvio_nativelegacy_NvPlayer_nativePos(0,0,pos,end,live,0,gen,rev)
int main(void) {
  unsigned first=novaRetomada(0);
  REPORT(12000,43000,0,first,0); assert(video_buffer_fim()==43.0);
  REPORT(13000,22000,0,first,0); assert(video_buffer_fim()==22.0);
  REPORT(13000,13000,0,first,0); assert(video_buffer_fim()==0);
  REPORT(-1,22000,0,first,0); assert(video_buffer_fim()==0);
  REPORT(13000,INT64_MIN+1,0,first,0); assert(video_buffer_fim()==0);
  REPORT(13000,12000,0,first,0); assert(video_buffer_fim()==0);
  durMs=20000;
  REPORT(13000,21000,0,first,0); assert(video_buffer_fim()==0);
  seekRevision=2;
  REPORT(1000,17000,0,first,1); assert(video_buffer_fim()==0);
  REPORT(1000,15000,0,first,2); assert(video_buffer_fim()==15.0);
  unsigned next=novaRetomada(0); assert(video_buffer_fim()==0);
  REPORT(1000,17000,0,first,2); assert(video_buffer_fim()==0);
  REPORT(-2000,18000,1,next,0); assert(video_buffer_fim()==18 && liveProgressoValido);
  REPORT(1000,2000,0,next,0); assert(video_buffer_fim()==2 && !liveProgressoValido);
  ativo=0; assert(video_buffer_fim()==0);
}
"""
with tempfile.TemporaryDirectory() as tmp:
    exe = Path(tmp) / "vod"
    subprocess.run([os.environ.get("CC", "cc"), "-std=c11", "-Wall", "-Werror",
                    "-I", str(root / "src"), "-x", "c", "-", "-pthread", "-o", str(exe)],
                   input=source, text=True, check=True)
    subprocess.run([exe], check=True)
print("Android VOD/Live buffer callback, seek revision and source replacement PASS")
