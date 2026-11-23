#!/bin/bash
# Actual production pure-Kotlin decision compilation; NOT a Media3/Android sink test.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
kotlinc android/app/src/main/java/space/nuvio/nativelegacy/AudioFidelityP5A.kt tests/p5/AudioFidelityP5ATest.kt -include-runtime -d "$tmp/audio-p5a.jar"
java -jar "$tmp/audio-p5a.jar"
