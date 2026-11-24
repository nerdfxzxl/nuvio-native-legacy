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


## Dedicated source acceptance review — 2026-11-24 (latest verdict)

**PARTIAL / NOT SOURCE ACCEPTED. P7/P8 NOT AUTHORIZED.** Starting HEAD verified `700b242bc53a40e9d5dbdbf9e0d5e0a696f81d8b`. Production/test-only CI input `79ca121a541c5d8d2f348cd3e0f126ecef0e698d`: compared with starting HEAD, 25 commits ahead, zero behind, 21 changed paths. The owner reported successful previous P6 CI at `c0d83b8264091c0d8005e510b4095e7672f335dd`; the commit workflow endpoint returned an empty PR-filtered result. No independently verified run ID or complete logs for that push. Do not transfer the previous green status to this changed revision.

### Demonstrated source defects corrected

- Q20: the cold Next path wrote `buscaNextAlvo` before `buscarParaPlayer()`, whose `limparFonteVOD()` erased it. Publish the exact authorized target after reset; only that matching, still-open Next bypasses the manual first-play sheet. Ordinary Play/Resume/Change Stream retains the prior selector rules. Guard native and post-play request consumption while hidden. Home clears unconsumed transport requests.
- Q44: a synthetic window-focus loss previously invoked real background pause. Separate checkpoint-only event from real SDL lifecycle, cancel uncommitted D-pad seek preview, persist backend-confirmed position (not optimistic native seek position), and retain delayed-open pause intent.
- Q45: `watched_p6.h` now accumulates bounded disjoint viewed intervals rather than repeatedly crediting a rewound scene. `medirAssistido` and `p6WatchReached` use confirmed Android positions; wait for current chapter discovery before using the fallback (EOF may use unknown-marker fallback). `progresso.h/.c` distinguish locally verified viewing (2) from unproven actual checkpoint (3), independently of history. `catalogo.c/.h`, `detail.c`, `home.c`, `continuar.c`, `descoberta.c`, `app.c` and `player.c` use matching P6 checkpoints for real resume instead of treating the watched cutoff as end-of-file. Local P6 checkpoints remain available when a tracker cannot safely store a high-percentage pause. `atividade.c` no longer independently announces completion at 90%. Manual unwatch is checked before writes and polled at bounded frequency during playback.
- Q50: post-play update requires active main-source real duration and ready playback; source replacement/return to waiting clears old overlay/countdown. No changes to the native overlay appearance or previous EOF-only automatic transition policy.
- Q48: examined `proximopreparo_consumir` token/list/policy detach before old player close, `video_parar` generation invalidation and `pararMantendoPreparo`, Kotlin `liberar` removing callbacks/listeners, invalidating session, pausing audio before async release, releasing route transaction and AFR; `encerrar` invalidates pending tickets. Preserve these paths rather than rewrite accepted infrastructure.

### Executed evidence

Exact production header blob `9c22c86938547bd8959ee117b378372f57dd9449` and test blob `a17303f38f80cb9e4e656017b4ba0856063a50c9` were materialized in the local container; both git blob hashes matched GitHub. Actual `cc -std=c11 -Wall -Wextra -Werror tests/watched_p6.c -lm` execution: **35/35 PASS**; repeated with UndefinedBehaviorSanitizer: **35/35 PASS**, no reported sanitizer findings. This covers marker priority, configured fallback, repeated/overlapping viewed intervals, seek-only false completion, invalid input and bounded coverage. Additional persisted progress tests are committed but not yet executed here. Repository network checkout unavailable in the local container. New dedicated test-only workflow path update triggers verification of exact input above; includes prior tests, expanded ARM64 syntax preflight (including post-play, CW, chapter, journal and social modules without modifying the normal APK preflight script), API35/Media3 compile and prior fidelity replays. [Workflow](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/workflows/p6-lifecycle-test-only.yml). New CI result not yet verified.

### Requirement-level verdicts

| Q | Verdict | Production evidence / remaining source gate |
|---|---|---|
| Q20 | PARTIAL | Fixed exact-target cold Next authorization and Home cancellation; prepared resource transfer is detached before close. The in-flight preload branch still waits on the old player's toast rather than immediately entering the next cinematic. Verify integration with pending preload, Back and duplicate intent, including manual selection settings. |
| Q22 | PARTIAL | Native recovery latch + Kotlin request-owned intent distinguish ordinary recovery and background pause; prior policy CI owner-reported successful. New exact revision and real route/decoder/surface retry interactions still require verification, not a physical autoplay claim. |
| Q44 | PARTIAL | Actual backend checkpoint, preview cancellation, real lifecycle versus focus loss, delayed-open pause ticket. Activity destruction/recreation and process/resource reclamation restoration still need an explicit source path audit and test. |
| Q45 | PARTIAL | In-play IMDb history, configured fallback, strict current-generation MKV proof, unique viewed intervals and real resume/CW separation; 35/35 helper assertions pass. Remaining: exact frozen episode identity for history when provisional numbering changes, opaque non-IMDb IDs (legacy history key truncation; no durable new automatic history claim for these), ensure progress/remote reconciliation does not revive a manual unwatch, and new persisted progress regressions. IntroDB lacks trustworthy release match; correctly treat as unknown. |
| Q48 | PARTIAL | Existing detach/async release, route/AFR cleanup, generation rejection and cancellation preserved; still verify failure/cancellation after prepared token detach and Activity destroy with pending release. |
| Q50 | PARTIAL | Real-EOF countdown and native overlay preserved, invalid source and replacement guards. Existing bounded automatic credits skip may still use release-unverified third-party bounds; ensure Q45/Q50 post-credit safety for those intervals. Verify final-episode/movie dismissal and pending Next integration. |

Physical device lifecycle, rendered frame/surface stability, HDMI ARC and complete application linking remain separate authorized integration/device gates. No APK assembly, signing, publication, default-branch merge, P7 or P8 implementation. Existing APK workflow and post-#134 handoff files unchanged in comparison. The new CI must be verified at the exact input, then address only the concrete remaining source gates above; do not reopen accepted P0–P5 without a demonstrated regression.


## Credits-provenance continuation — 2026-11-24 (latest checkpoint)

Starting HEAD `b8e88de88717a2a3da6ebe91ca8f77fa1ab3ca14`. Comparison against `79ca121a541c5d8d2f348cd3e0f126ecef0e698d` shows two documentation-only commits. Prior CI evidence is now independently established, superseding the earlier owner-reported/pending status.

| P6 test-only CI | Exact tested revision | Verified conclusion |
|---|---|---|
| [#1 / 45347395005](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45347395005) | `b9ef36390c31e3c83ef453a3e426be907b5584ff` | SUCCESS |
| [#2 / 45349531453](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45349531453) | `c0d83b8264091c0d8005e510b4095e7672f335dd` | SUCCESS |
| [#3 / 45355672435](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45355672435) | `79ca121a541c5d8d2f348cd3e0f126ecef0e698d` | SUCCESS |
| [#4 / 45364177583](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45364177583) | `cfb6fbf2bfdacff521d010ce6a1f8fa3aaaee0ba` | IN PROGRESS at checkpoint |

Actual completed #3 job 138312747703 logs: NDK C/JNI preflight and API35/Media3 1.8.0 Kotlin production compile passed; actual PCM and HEVC/Format 47 assertions, native chapter/history/progress checks (including 620 distinct persisted titles), P6 postplay 17, watched 35, playback intent 33, audio fidelity 17, route 46, HDR 27, AFR 34, surface 55, P2 frame hold 17 and cinematic 41 passed. No need to rerun unchanged #3.

### New production correction

In `cfb6fbf2bfdacff521d010ce6a1f8fa3aaaee0ba`, `src/intro.c/.h` now separates exact current-file chapter interval provenance from recommendation/manual hints. `intro_auto_ativo` cannot authorize automatic credits seeking for third-party or cached URL-only bounds. `src/capmkv.c` publishes proof only for freshly read, generation-current strict closing credits with an exact matching end at the immediately following chapter boundary; a later preview boundary cannot be used to jump across an intervening chapter. Proof is reset on chapter/source replacement and shutdown. Existing opening/recap automatic skip and native overlay are preserved. `tests/intro.c` exercises actual automatic versus manual selection, cache/third-party rejection, exact and mismatched bounds, post-credit and invalidation; `tests/capmkv.c` checks direct-read versus cached publication. Added actual intro test invocation to existing P6-only workflow. CI #4 job 138341515079 has successfully completed exact-input verification, native audio/video/first-frame regressions, native chapter/history/progress regressions including the new intro tests, and NDK C/JNI syntax step. Full Android compile/replay and remaining policy steps not yet complete at checkpoint. No physical-device or full-link claim.

### Current verdicts and remaining demonstrated source gates

| Q | Verdict | Remaining gate |
|---|---|---|
| Q20 | PARTIAL | `app.c:abrirProximoEpisodio/proximoPreparoPasso` still wait in the previous player's toast during in-flight preload. Transfer pending job ownership to the next episode cinematic immediately without cancellation by `posplay_fechar`; test exact target, Back, Home, duplicate Next and manual source selection, and preserve cold fallback and prepared-result adoption. |
| Q22 | PARTIAL | Request-owned native/Kotlin intent policy compiled and passed CI #3. Still trace and test actual route/decoder/surface retry integration: ordinary automatic recovery vs Home/background and superseding explicit commands; do not reopen earlier accepted recovery infrastructure indiscriminately. |
| Q44 | PARTIAL | Confirmed checkpoint and delayed-open pause paths present and CI #3 passed. Explicit Activity destruction/recreation and resource reclamation restoration source paths and regressions remain. |
| Q45 | PARTIAL | Current strict marker, percentage fallback, viewed intervals, in-play marking and persisted progress tests passed. `player.c:p6WatchMark/p6WatchOverride` still use mutable season/episode and IMDb-only durable history. Freeze authorized episode identity across numbering changes, support opaque non-IMDb IDs without key truncation/collision, and verify remote reconciliation cannot revive manual unwatch. IntroDB lacks release proof and must remain a hint. |
| Q48 | PARTIAL | Prior generation-aware detach/async release and route/AFR paths preserved. Verify pending/failed adoption cancellation, exactly-once cleanup and Activity destruction with pending release against real paths. |
| Q50 | PARTIAL | New unverified-bound auto-seek defect corrected, chapter/intro tests passed in #4; real EOF and native overlay preserved. Final pending Next integration and final-episode/movie dismissal audit remain, and #4 full conclusion is pending. |

**P6 OVERALL: PARTIAL / NOT SOURCE ACCEPTED.** No P7/P8 implementation or authorization; P0–P5 accepted source checkpoints and Q18 owner deferral preserved. No APK build, signing, publication or default-branch merge. Full linking and physical-device checks remain separate. Next continuation should complete these specific gates and review exact new CI, without rerunning already verified #3.
