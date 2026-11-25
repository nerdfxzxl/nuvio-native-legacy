#!/usr/bin/env bash
# Preserve the suite status and fail on sanitizer diagnostics even when an
# existing fixture returns success during asynchronous process teardown.
set -u
log=$(mktemp "${TMPDIR:-/tmp}/p7-suite.XXXXXX") || exit 125
trap 'rm -f "$log"' EXIT
timeout --signal=TERM --kill-after=20s 10m "$@" 2>&1 | tee "$log"
statuses=("${PIPESTATUS[@]}")
if grep -Eq 'AddressSanitizer:DEADLYSIGNAL|ERROR: (AddressSanitizer|LeakSanitizer|UndefinedBehaviorSanitizer)|SUMMARY: (AddressSanitizer|LeakSanitizer|UndefinedBehaviorSanitizer)|UndefinedBehaviorSanitizer:|runtime error:' "$log"; then
  echo 'P7: sanitizer diagnostic detected; suite failed regardless of exit status' >&2
  exit 1
fi
if [ "${statuses[1]}" -ne 0 ]; then exit 125; fi
exit "${statuses[0]}"
