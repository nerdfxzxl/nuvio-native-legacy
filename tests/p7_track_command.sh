#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
kotlinc android/app/src/main/java/space/nuvio/nativelegacy/PlaybackIntentP6.kt \
  tests/p7_track_command.kt -include-runtime -d "$tmp/track.jar"
java -jar "$tmp/track.jar"
