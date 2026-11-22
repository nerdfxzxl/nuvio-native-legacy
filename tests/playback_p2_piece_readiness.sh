#!/bin/sh
# Host-only C11 check for genuine selected-file piece telemetry invariants.
# No engine build, Gradle, GitHub Actions or APK.
set -eu
cd "$(dirname "$0")/.."
bin="$(mktemp "${TMPDIR:-/tmp}/nuvio-p2a-XXXXXX")"
trap 'rm -f "$bin"' EXIT HUP INT TERM
"${CC:-cc}" -std=c11 -O1 -g -Wall -Wextra -Werror -Isrc \
  tests/playback_p2_piece_readiness.c -o "$bin"
"$bin"
