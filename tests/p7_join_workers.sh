#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir=$(mktemp -d "${TMPDIR:-/tmp}/nuvio-p7-join.XXXXXX")
trap 'rm -rf "$dir"' EXIT
cc -std=c11 -O1 -g -Wall -Wextra -Werror -fsanitize=address,undefined -fno-omit-frame-pointer -pthread -Isrc tests/p7_join_workers.c -o "$dir/test"
for n in $(seq 1 50); do "$dir/test"; done
printf 'PASS: 50 worker shutdown repetitions under ASan/UBSan\n'
