#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
case $(uname -s) in
  Darwin) jdk=${JAVA_HOME:-$(/usr/libexec/java_home)}; platform=darwin; link=(-Wl,-dead_strip);;
  Linux) jdk=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}; platform=linux; link=(-Wl,--gc-sections);;
esac
flags=(-std=gnu11 -g -O1 -ffunction-sections -fdata-sections)
if [[ ${SANITIZE:-0} == 1 ]]; then flags+=(-fsanitize=address,undefined -fno-omit-frame-pointer); fi
cc "${flags[@]}" -Isrc -I"$jdk/include" -I"$jdk/include/$platform" $(sdl2-config --cflags) \
  tests/p7_callback_ownership.c "${link[@]}" $(sdl2-config --libs) -lm -pthread -o "$tmp/test"
"$tmp/test"
