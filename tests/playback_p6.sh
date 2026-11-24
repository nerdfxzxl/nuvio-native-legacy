#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cc -std=c11 -Wall -Wextra -Werror tests/postplay_p6.c -lm -o "$tmp/postplay"
"$tmp/postplay"
kt=android/app/src/main/java/space/nuvio/nativelegacy
kotlinc "$kt/PlaybackIntentP6.kt" tests/p6/PlaybackIntentP6Test.kt -include-runtime -d "$tmp/intent.jar"
java -jar "$tmp/intent.jar"
