#!/usr/bin/env python3
"""Linux-only compatibility adapter for unchanged legacy macOS test scripts.

Translate toolchain flags, never source files, test arguments or assertions.
Full-app suites use the same NV_LINUX_DESKTOP backend as tools/linux.sh.
"""
import os
import subprocess
import sys

if sys.platform != "linux":
    raise SystemExit("P7 legacy adapter is Linux-only")
compiler = os.environ["P7_REAL_CC"]
full = os.environ.get("P7_FULL_APP") == "1"
deps = ["sdl2", "SDL2_image", "SDL2_ttf", "glesv2", "egl", "zlib"]
def pkg(flag):
    return subprocess.check_output(["pkg-config", flag, *deps], text=True).split()

args = iter(sys.argv[1:])
out = []
dead_strip = False
for arg in args:
    if arg in ("-I/opt/homebrew/include", "-I/opt/homebrew/include/SDL2", "-L/opt/homebrew/lib"):
        continue
    if arg == "-framework":
        framework = next(args)
        if framework != "OpenGL" or not full:
            raise SystemExit("Unexpected framework: " + framework)
        # Actual Linux desktop backend links GLESv2/EGL below.
        continue
    if arg == "-Wl,-dead_strip":
        out.append("-Wl,--gc-sections")
        dead_strip = True
    else:
        out.append(arg)
compile_only = any(flag in out for flag in ("-c", "-E", "-S"))
prefix = pkg("--cflags")
if full and "-DNV_LINUX_DESKTOP" not in out:
    prefix.append("-DNV_LINUX_DESKTOP")
if dead_strip:
    prefix += ["-ffunction-sections", "-fdata-sections"]
if not compile_only:
    if full:
        out += pkg("--libs") + ["-ldl"]
    out += ["-pthread", "-lm"]
if os.environ.get("P7_ASAN") == "1":
    out += ["-fsanitize=address", "-fno-omit-frame-pointer"]
command = [compiler, *prefix, *out]
print("+ " + __import__("shlex").join(command), file=sys.stderr, flush=True)
os.execv(compiler, command)
