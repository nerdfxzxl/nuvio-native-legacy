#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
kotlinc android/app/src/main/java/space/nuvio/nativelegacy/RecriaHdr.kt \
    tests/p5/SurfaceTransitionP5CTest.kt -include-runtime -d "$tmp/test.jar"
java -jar "$tmp/test.jar"
