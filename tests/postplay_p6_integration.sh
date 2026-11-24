#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
flags=(-std=gnu11 -O1 -g -Isrc -pthread -Wall -ffunction-sections -fdata-sections)
case "$(uname -s)" in Darwin) link=(-Wl,-dead_strip);; *) link=(-Wl,--gc-sections);; esac
cc "${flags[@]}" $(sdl2-config --cflags) src/posplay.c src/catalogo.c src/proximo.c src/velocidade.c tests/postplay_p6_integration.c "${link[@]}" -lm -o "$tmp/posplay"
"$tmp/posplay"
