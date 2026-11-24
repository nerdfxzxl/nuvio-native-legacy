#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
case "$(uname -s)" in Darwin) link=(-Wl,-dead_strip);; *) link=(-Wl,--gc-sections);; esac
cc -std=gnu11 -O1 -g -Isrc $(sdl2-config --cflags) -DNV_TEST_ACCOUNT_PULL -ffunction-sections -fdata-sections \
  src/contapend.c src/vistoep.c src/contalib.c src/js.c src/jsw.c tests/contapend.c "${link[@]}" -lpthread -lm -o "$tmp/account"
"$tmp/account" account
