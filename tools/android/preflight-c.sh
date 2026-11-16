#!/usr/bin/env bash
# Fast Android C syntax check, independent of Rust, P2P, curl and image builds.
# Fail before expensive dependency builds if UI source has compiler errors.
set -euo pipefail
cd "$(dirname "$0")/../.."

ndk="${ANDROID_NDK_HOME:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/27.2.12479018}"
cc="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
if [[ ! -x "$cc" ]]; then
  echo "::error::Android NDK clang not found: $cc" >&2
  exit 1
fi
if [[ ! -f /usr/include/SDL2/SDL.h ]]; then
  echo "::error::SDL2 development headers missing; install libsdl2-dev" >&2
  exit 1
fi

# Use the host SDL2 headers only, NOT host glibc headers. The compiler gets
# Android system headers and GLES from its real NDK sysroot.
# This avoids a glibc/Android mixed-header check.
tmp="$(mktemp -d "${RUNNER_TEMP:-/tmp}/nuvio-c-preflight.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/SDL2"
cp -L /usr/include/SDL2/*.h "$tmp/SDL2/"
# Do NOT use Ubuntu's x86_64 SDL_config.h. It advertises x86 SIMD
# intrinsics which the Android ARM64 compiler cannot compile.
# SDL explicitly provides SDL_config_minimal.h for cross-platform builds;
# it is enough to type-check SDL APIs without compiling SDL itself.
if [[ ! -f "$tmp/SDL2/SDL_config_minimal.h" ]]; then
  echo "::error::SDL_config_minimal.h missing from libsdl2-dev" >&2
  exit 1
fi
cat > "$tmp/SDL2/SDL_config.h" <<'SDL_CONFIG'
#ifndef SDL_config_h_
#define SDL_config_h_
#include "SDL_config_minimal.h"
#endif
SDL_CONFIG
echo "SDL2 config: portable minimal (no Ubuntu/x86-specific intrinsics)"

flags=(
  --target=aarch64-none-linux-android24
  -fsyntax-only -fPIC -DNV_ANDROID -DNV_TOUCH_UI
  -DNV_P2P_MOTOR -DNV_RUST_MKVCORE
  '-DNV_SUPABASE_URL=""' '-DNV_SUPABASE_ANON_KEY=""'
  '-DNV_TV_LOGIN_BASE=""' '-DNV_VERSAO="2.0.8"'
  -Isrc "-I$tmp"
  -Werror=implicit-function-declaration
  -Werror=incompatible-pointer-types
)

for source in src/main.c src/streams.c; do
  echo "::group::Early C compile: $source"
  "$cc" "${flags[@]}" "$source"
  echo "::endgroup::"
done
echo "Early Android C compiler check: passed"
