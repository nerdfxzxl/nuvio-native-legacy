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
# Ubuntu's SDL_config.h delegates to an architecture-specific config file.
# Copy its REAL config beside the public headers: -I /usr/include/x86_64-
# linux-gnu would leak glibc headers into Android's NDK compilation.
real_sdl_config="$(find /usr/include -maxdepth 4 -type f -name _real_SDL_config.h -print -quit)"
if [[ -z "$real_sdl_config" ]]; then
  echo "::error::Ubuntu SDL2 architecture config (_real_SDL_config.h) not found" >&2
  exit 1
fi
cp -L "$real_sdl_config" "$tmp/SDL2/_real_SDL_config.h"
echo "SDL2 config: $(basename "$(dirname "$real_sdl_config")") (copied for Android headers)"

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
