#!/bin/sh
# P2-C host-only C11 checks. No Android/Gradle/GitHub Actions invocation.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
CC="${CC:-cc}"
"$CC" -std=c11 -O1 -g -Wall -Wextra -Werror \
  tests/playback_p2c_nav.c -o "$tmp/nav"
"$tmp/nav"
