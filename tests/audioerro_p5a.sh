#!/bin/bash
# Compiles the actual native Audio Playback Error focus/retry policy (not SDL).
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cc -std=c11 -Wall -Wextra -Werror -pedantic -Isrc tests/audioerro_p5a.c -o "$tmp/audioerro_p5a"
"$tmp/audioerro_p5a"
