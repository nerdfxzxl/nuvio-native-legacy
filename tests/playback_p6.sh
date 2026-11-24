#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cc -std=c11 -Wall -Wextra -Werror tests/postplay_p6.c -lm -o "$tmp/postplay"
"$tmp/postplay"
cc -std=c11 -Wall -Wextra -Werror tests/next_p6.c -o "$tmp/next"
"$tmp/next"
cc -std=c11 -Wall -Wextra -Werror tests/watched_p6.c -lm -o "$tmp/watched"
"$tmp/watched"
kt=android/app/src/main/java/space/nuvio/nativelegacy
kotlinc "$kt/PlaybackIntentP6.kt" tests/p6/PlaybackIntentP6Test.kt -include-runtime -d "$tmp/intent.jar"
java -jar "$tmp/intent.jar"

kotlinc "$kt/PendingAdoptionP6.kt" tests/p6/PendingAdoptionP6Test.kt -include-runtime -d "$tmp/adoption.jar"
java -jar "$tmp/adoption.jar"

cc -std=c11 -Wall -Wextra -Werror tests/restore_p6.c -o "$tmp/restore"
"$tmp/restore"
