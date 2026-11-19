# Build 120 handoff

Continue Nuvio Legacy Build #120 from the latest commit on `feature/official-stream-selector`. Read `docs/BUILD_120_HANDOFF.md` first. Do not restart the project or repeat completed work. Implement remaining refinements, run the required checks, and continue the APK build process.

## Baseline and build

The starting branch HEAD is exactly Build 119, 1b29dab675d80fc0bb2cc52aa06fcb28ee3479ae. [Successful baseline run 44570475296](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44570475296). Official dev reference ff7fbd747955a1a6108ac23c296b79ad851a14b1. No Build 120 APK or run yet. Use `git rev-parse HEAD` for the latest checkpoint including this document. The document cannot contain its own commit hash.

## Status

| # | Refinement | Work on this branch since 119 |
|---|---|---|
|1|Single artwork and framing|Not started|
|2|Interruptible focus and neighbor motion|Not started|
|3|Smooth or instant preference|Not started|
|4|Back / Hero Down|Not started|
|5|Fullscreen Hero|Not started|
|6|Multi-catalogue Hero rotation|Not started|
|7|Fast Left protection|Not started|
|8|Short / long OK|Not started|
|9|Resume pill track|Not started|
|10|Continue Watching limits|Not started|
|11|Row counters|Not started|
|12|Per-row per-style size editor|Not started|
|13|Shared poster bounds|Not started|
|14|Persistent Home artwork cache|Not started|

## Execution notes

Initial files changed: this document only. Verified branch ancestry and workflow triggers. No implementation tests or TV tests yet. The reported Build 119 visual defects remain open. No clarification pending.

Start with src/home.c, src/detail.c and the native regression guards in tests/native_home_refinements.py. The latter encode Build 119's superseded two-artwork and global-size behavior. Trace geometry, capture actual drawn bounds, and update only guards superseded by new requirements. Then implement Home/Hero navigation and rotation, input and Resume, row editor, and artwork cache. Inspect the existing native cache before changing it. Leave Android playback seek cache intact. For each group record changed paths, actual checks and remaining device validation. Run tools/android/preflight-c.sh in the NDK environment. The scoped no-APK and APK workflows watch docs/ci/trigger-netflix-native-preflight.txt and docs/ci/trigger-netflix-detail-once.txt respectively. Only change the full-build trigger when integrated checks pass. Inspect jobs, fix concrete failures, verify artifact for the exact built SHA.
