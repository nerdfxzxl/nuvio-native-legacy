# P6 source acceptance — PARTIAL / NOT ACCEPTED

Date: 2026-11-24. Branch: `feature/official-stream-selector`.
Starting HEAD verified: `e8b523c775c93cf2d89d71506d730f410bf64c0e`.
Production/test/CI input: `b9ef36390c31e3c83ef453a3e426be907b5584ff` (18 commits ahead, zero behind; 14 changed files). P0–P5 previously source accepted; Q18 owner-deferred. No P7/P8 authorization.

## Implemented bounded changes

- Q22/Q44: `PlaybackIntentP6.kt`, `NvPlayer.kt`, `video_android.c`, `video.h`, `player.c`, `main.c`: distinguish system pause from normal paused recovery; guard queued commands with lifecycle epoch and request identity; retain pending open/stop ownership; return without requesting playback; invalidate stale scrub autoplay. Explicit Play clears recovery pause. Reset session-scoped progress timers and samples.
- Q20/Q50: `posplay.c`, `posplay.h`, `postplay_p6.h`: native recommendation card remains; automatic transition requires active real EOF, finite actual position/duration, no seek, suspension or dismissal, and enabled series autoplay. Countdown begins only after EOF; manual Next independent. Background cancels automatic requests and abandoned preload; existing prepared-source adoption architecture retained, not claimed fully verified.
- Q48: targeted lifecycle/cleanup ownership corrections; preserve previously accepted release and decoder/surface pathways.
- Tests: `tests/postplay_p6.c`, `tests/p6/PlaybackIntentP6Test.kt`, `tests/playback_p6.sh`, and actual Kotlin compile source inventory in `tests/cacheboost_android.py`.

## Verification evidence

Executed local C11 compiler with -Wall -Wextra -Werror and actual Kotlin JVM compiler, against exact retrieved production helpers and tests. All four locally reconstructed Git blob hashes matched repository blobs:
- postplay_p6.h: 7bc19841fcf8356ba43f36eb3245a8b8080a87af
- postplay_p6.c: d3321db808e2e2dd2894d2992058e7edbcd01ea3
- PlaybackIntentP6.kt: 5c22530b1bed5e9ea4e54fd354f238e9b61ab6be
- PlaybackIntentP6Test.kt: 07d32b6f3c3c58c5a2365bfefe962496efb14943

Actual results: native post-play policy **17/17 PASS**; Kotlin intent policy **26/26 PASS**. These are executable helper tests, not full integration or device tests.

Dedicated [test-only workflow](../.github/workflows/p6-lifecycle-test-only.yml) added at exact input above; push path is limited to the new workflow. It checks checkout SHA, executes P6 tests, prior native/audio/first-frame tests, ARM64 C/JNI preflight, real API35/Media3 1.8.0 compilation, PCM/HEVC replay and previous P5 policies. No existing APK workflow edited. Connector's commit workflow listing filters to PR runs and returned no runs; push run ID, status, and completed logs could not be verified here. **CI NOT VERIFIED; no compile claim for this revision.** [Workflow runs](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/workflows/p6-lifecycle-test-only.yml).

## Requirement verdicts and exact remaining blockers

| Requirement | Verdict | Remaining source gate |
|---|---|---|
| Q20 | PARTIAL | Trace explicit and automatic Next across generation-owned prepared source adoption, cinema, cancellation during Home/Back/replacement and duplicate transition prevention. Verify no destruction of transferred resources. |
| Q22 | PARTIAL | Policy tested; audit real native retry and Media3 decoder/surface/route fallback, pending opens and newer pause/Back across request generations. |
| Q44 | PARTIAL | Verify pause during startup/source resolution (including before comVideo), seek, buffering, recovery, resource reclaim and activity recreation; ensure position/cache and paused return even if resolution completes after foreground. |
| Q45 | NOT ACCEPTED | Existing completion still exit-oriented; credEfetivo can use neighbor/learned/preview and capmkv_creditos can return unnamed last chapter. IntroDB duration matching alone is not release verification. Existing fixed 90% scrobble path and local watched thresholds conflict with configurable ajustes_cw_concluido; remote stop thresholds and contradictory-duration behavior need review. Implement verified credits vs unknown separation, reliable configured fallback, idempotent in-play watched marking without exit, progress and manual overrides across all sync targets. No guessed thresholds or release claims. |
| Q48 | PARTIAL | Audit destruction/recreation, cancellation, pending recovery, network jobs and adopted preload ownership on real integration paths; no full cleanup acceptance yet. |
| Q50 | PARTIAL | Native overlay preserved; actual-EOF autoplay policy passes helper tests. Verify integration of recommendation-only estimates, movie/series/final episode, post-credit scenes, dismissal, Back, and duplicate countdown/Next suppression. Review existing tests that expected countdown on card visibility. |

## Next bounded continuation

Verify exact P6 CI run and logs; fix demonstrated regressions only. Complete the above unresolved P6 source gates, especially Q45 and Q20/Q44 integration; then run updated exact-source CI and final P6 review. No P7 until P6 accepted; P8 remains build-authorization gated. No APK assembly, signing, upload, publication, default-branch merge or physical-device verification performed. Prior P0–P5 accepted checkpoints are preserved, not reopened.


## Continuation checkpoint — 2026-11-24 (supersedes prior remaining implementation list where addressed)

Starting HEAD: `e436435bc75c74bc6093a7524e17f84fb24c8d5f`. Owner reported the previous P6 test-only run succeeded; exact prior run logs were not independently available. Updated production/test/CI input: `c0d83b8264091c0d8005e510b4095e7672f335dd`.

- Q45: added strict, current-generation MKV closing-credit chapter proof distinct from recommendation hints; cached/unnamed/preview/neighbor markers do not authorize completion. IntroDB release identity remains unproven by its current response, therefore use the configured percentage fallback rather than guessing. Actual viewed media and trusted same-duration resume proof govern watched marking during playback. Preserve actual checkpoint and multipart post-credit resume, prevent duplicate exit marking, persist local/anonymous IMDb history through the existing journal and respect newer manual unwatch and profile identity. Trakt/Simkl no longer turn an unaccepted exit into a watched event by their independent STOP thresholds.
- Q44/Q22: propagate background pause through delayed native source resolution and one-shot Android open request ownership; distinguish ordinary retry and system pause; explicit command for the same ticket takes precedence.
- Q20/Q50/Q48: cancel pending Next/related-title requests and background preload advancement on Home; preserve existing cinematic and resource adoption pathways.
- Tests: exact matching Git blob content executed locally using strict C11 and Kotlin JVM: post-play 17/17 PASS; watched policy 23/23 PASS; intent policy 33/33 PASS (73 assertions). New chapter provenance, anonymous/profile history journal and persisted resume-proof tests added to the dedicated test-only workflow, along with the earlier Android/Media3 compilation and P5 regressions. New CI results are not yet verified. No APK build.

### Remaining source acceptance gates

P6 remains PARTIAL / NOT SOURCE ACCEPTED. Verify the new exact-input CI and fix any regressions. Review real integration of the watched transition across all resume/CW presentation and sync paths (including non-IMDb/personal-server behavior, multipart/seek proof, and stale overrides), post-credit behavior and auto/manual Next ownership, late open/background recovery, and cleanup. Existing CW percent-based routing is not claimed fully reconciled with the separate watched journal. IntroDB timing may be accepted only with trustworthy release matching, otherwise fallback. Device lifecycle, HDMI and actual rendering tests remain future authorized integration/device gates. Do not treat passing isolated policy tests as full production acceptance. No P7 or P8 authorization.
