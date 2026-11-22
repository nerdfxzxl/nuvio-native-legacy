#!/bin/sh
# Host-only native source checks, no Gradle/NDK/CI/APK.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
mkdir -p "$tmp/SDL2"
cat > "$tmp/SDL2/SDL.h" <<'SDL_STUB'
#ifndef NV_P2_TEST_SDL_STUB
#define NV_P2_TEST_SDL_STUB
#include <stdint.h>
typedef uint32_t Uint32;
typedef int32_t SDL_Keycode;
#define SDLK_UP 273
#define SDLK_DOWN 274
#endif
SDL_STUB
: "${CC:=cc}"
"$CC" -std=c11 -O1 -g -Wall -Wextra -Werror -I"$tmp" \
  tests/playback_p2_timing.c src/abrecine.c -lm -o "$tmp/playback-p2"
"$tmp/playback-p2"
