#!/bin/sh
# P2-D host unit test; compiles the REAL cinematic implementation, not a mirror.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
CC="${CC:-cc}"
"$CC" -std=c11 -O1 -g -Wall -Wextra -Werror \
  -Itests/p2d_stubs tests/playback_p2d_handoff.c src/abrecine.c \
  -lm -o "$tmp/p2d"
"$tmp/p2d"
