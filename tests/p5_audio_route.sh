#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
kt=android/app/src/main/java/space/nuvio/nativelegacy
kotlinc "$kt/AudioRouteP5D.kt" "$kt/AudioFidelityP5A.kt" tests/p5/AudioRouteP5DTest.kt -include-runtime -d "$tmp/route.jar"
java -jar "$tmp/route.jar"
