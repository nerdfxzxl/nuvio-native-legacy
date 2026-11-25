#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
work=$(mktemp -d "${TMPDIR:-/tmp}/p7-diag.XXXXXX")
trap 'rm -rf "$work"' EXIT
cc -std=c11 -Wall -Wextra -Werror -g -fsanitize=address,undefined -Isrc tests/playback_diag_p7.c -o "$work/test"
"$work/test"
