#!/bin/sh
# P3 host C11 unit tests; works without SDL, Android SDK, or an APK.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
CC="${CC:-cc}"
"$CC" -std=c11 -O1 -g -Wall -Wextra -Werror \
  tests/playback_p3_reload.c -o "$tmp/p3"
"$tmp/p3"
