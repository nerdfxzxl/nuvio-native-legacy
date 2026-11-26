#!/bin/bash
set -eu
cd "$(dirname "$0")/.."
out="$(mktemp)"
trap 'rm -f "$out"' EXIT
cc -Isrc -Wall -Wextra -Werror -O1 -g -fsanitize=address,undefined tests/player_badge_p8.c -o "$out"
"$out"
