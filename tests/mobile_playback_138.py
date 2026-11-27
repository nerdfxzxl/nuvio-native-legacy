"""Isolated mobile startup deadline and protected TV dispatch contracts."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
src = root / "android/app/src/main/java/space/nuvio/nativelegacy"
player = (src / "NvPlayer.kt").read_text()
renderer = (src / "HdrCodecFallbackP5B.kt").read_text()
app = (root / "src/app.c").read_text()
assert player.count("if (r.phonePcm) MediaCodecSelector.DEFAULT else videoCodecSelector") == 2
assert "r.phonePcm,\n                        { at ->" in player
assert "HdrPolicyP5.ordinaryDecoders(stock, mobile," in renderer
assert 'getenv("NUVIO_INTERFACE_AUTO")' in app
watch = app[app.index("static void vigiarAberturaManual(void) {"):app.index("static int aberturaVencida(Uint32 desde) {")]
assert watch.index("if (g.falhou)") < watch.index("nv_mobile_startup_deadline")
assert "video_download_bps(), g.carregando" in watch
assert "fontevolta_abertura_vencida(&g, prazoManual)" in watch
code = r"""
#include "mobile_startup.h"
#include <assert.h>
int main(void) {
  assert(nv_mobile_startup_deadline(0,25000,100000,1)==25000);
  assert(nv_mobile_startup_deadline(0,25000,0,1)==25000);
  assert(nv_mobile_startup_deadline(1,25000,-1,1)==25000);
  assert(nv_mobile_startup_deadline(1,25000,0,1)==25000);
  assert(nv_mobile_startup_deadline(1,25000,10,0)==25000);
  assert(nv_mobile_startup_deadline(1,25000,10,1)==90000);
  assert(nv_mobile_startup_deadline(1,100000,10,1)==100000);
}
"""
with tempfile.TemporaryDirectory() as d:
    exe = Path(d) / "test"
    subprocess.run(["cc", "-std=c11", "-Wall", "-Werror", "-I", str(root/"src"), "-x", "c", "-", "-o", str(exe)], input=code, text=True, check=True)
    subprocess.run([str(exe)], check=True)
print("Mobile progress deadline and TV/phone decoder dispatch PASS")
