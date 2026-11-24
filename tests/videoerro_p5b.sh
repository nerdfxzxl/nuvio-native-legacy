#!/bin/bash
# Q39: actual production video modal action policy, no SDL/Android mocks.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cc -std=c11 -Wall -Wextra -Werror -pedantic -Isrc tests/videoerro_p5b.c -o "$tmp/videoerro_p5b"
"$tmp/videoerro_p5b"
