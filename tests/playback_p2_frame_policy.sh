#!/usr/bin/env sh
# P2-B host-only policy check. No Android SDK, NDK, APK or GitHub Actions.
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
kotlinc \
  android/app/src/main/java/space/nuvio/nativelegacy/FrameHoldP2B.kt \
  tests/FrameHoldP2BTest.kt \
  -include-runtime -d "$tmp/framehold.jar"
java -jar "$tmp/framehold.jar"
