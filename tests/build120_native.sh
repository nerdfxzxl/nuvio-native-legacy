#!/usr/bin/env bash
# Linux host regression checks using the Android UI code path and SDL2/GLES headers.
set -euo pipefail
cd "$(dirname "$0")/.."
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT
for suite in home detail; do
  cc -O1 -DNV_ANDROID -ffunction-sections -fdata-sections -Wl,--gc-sections \
    -Isrc "tests/build120_${suite}.c" -lm -o "$dir/$suite"
  "$dir/$suite"
done
cc -O1 -g -ffunction-sections -fdata-sections -Wl,--gc-sections -Isrc \
  src/catalogo.c src/progresso.c src/cwordem.c src/cwretido.c tests/build120_history.c \
  src/cotacat.c src/js.c src/metaprov.c src/colecoes.c src/redeurl.c src/catordem.c \
  -pthread -lm -o "$dir/history"
"$dir/history"
