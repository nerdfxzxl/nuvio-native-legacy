#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
T="${TMPDIR:-/tmp}/nuvio-p1-identity-tests"
cc -std=c11 -O1 -g -Wall -Wextra -Werror tests/playback_p1_identity.c -o "$T"
"$T"
