#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc "$ROOT/android/app/src/main/java/space/nuvio/nativelegacy/HdrPolicyP5.kt" "$ROOT/tests/p5/HdrPolicyP5Test.kt" -d "$TMP/p5hdr.jar"
kotlin -classpath "$TMP/p5hdr.jar" HdrPolicyP5TestKt
