# Nuvio Legacy — Q01–Q50 Implementation Progress

**Last updated:** 2026-11-22
**Status:** P0 COMPLETE; P1 SOURCE IMPLEMENTATION COMPLETE; **P2 SOURCE ACCEPTANCE REVIEW FAILED — P2 REMAINS INCOMPLETE (Q10/Q14/Q26/Q27 post-READY navigation defect); Q18 PLAYABLE-READINESS % EXPLICITLY DEFERRED BY OWNER**. P2-A/B/C source checkpoints preserved. Full NDK/Android/Media3 compilation and device validation deferred to authorized P7/P8. No APK/Actions authorized.
**Branch:** `feature/official-stream-selector` (not `master`).
**Prior feature baseline:** `6171645e05d2137d2a1e9ed1b113687246acd7ca` — docs: lock subtitle filename Format A and unbuilt wrapping/parser test handoff.
**Latest planning commits:** master specification `7d3c81d81aee309843dd2f0b767c6ef3c2d59723`; work packages `b059a7a9a42ae2b91d87bff0a03bfeb314d0f647`; this progress tracker is an additional documentation-only commit. **Recheck actual HEAD at the beginning of every session.**

## Authoritative references

- [Approved Q01–Q50 master specification](PLAYBACK_50_MASTER_SPEC.md)
- [Bounded package plan and session protocol](PLAYBACK_50_WORK_PACKAGES.md)
- [Existing post-#134 unbuilt refinements](PLAYER_PENDING_REFINEMENTS_AFTER_134.md)
- [P0 verified source inventory, preservation audit and known risks](PLAYBACK_50_P0_BASELINE.md)
- [Official NuvioTV dev reference](https://github.com/NuvioMedia/NuvioTV/tree/dev)

## Existing work to preserve (not part of Q1–Q50 implementation progress)

**Latest built and device-tested artifact per handoff: #134.** After #134, code for eight refinement groups was committed but **not built**. They include official parental guide, landscape mobile hero, one-Back panel exit, English subtitle rows and Format A complete decoded release filenames with UTF-8-safe wrapping, 2.5% font increments, held-OK acceleration, absolute buffering rail and responsive focused player pill. The handoff records source-level checks only; native/Android/TV verification is still outstanding. **All must be in final integrated workflow alongside the 50-question refinements.**

## Work package status

| Package | Scope | Status | Implementation commits | Verification |
|---|---|---|---|---|
| P0 | Baseline, inventory, preservation | COMPLETE — source audit only | [`d0ff5a1`](https://github.com/nerdfxzxl/nuvio-native-legacy/commit/d0ff5a1f44fc42034c867ca149a21e1814d2f77c) (documentation) | GitHub branch diff/source/test scripts inspected; **no tests, compile or APK run** |
| P1 | State/source identity/cancellation | **SOURCE IMPLEMENTATION COMPLETE; Android APK/TV unverified** | `abe5479` through `0df37f5` (all 13 exact SHAs in P1 checkpoint below) | Native GCC/Clang identity PASS; 32/32 source assertions PASS; all 7 modified C/header preprocessor balances PASS; full Android compilation unavailable |
| P2 | Cinematic/arc/status/speed | **IN PROGRESS — P2-A/P2-B/P2-C source checkpointed; Q18 and runtime gates open** | Original P2, P2-A, P2-B and P2-C chronological commit registers below; latest tracker HEAD | P2-C 45/45 live source checks, 15/15 mirrored C11 GCC/Clang/ASan+UBSan, 9/9 #if balances; **full Android/TV tests NOT RUN** |
| P3 | Selector/reload/recovery | NOT STARTED | — | — |
| P4 | Buffer/network/cache/seek | NOT STARTED | — | — |
| P5 | Fidelity/HDR/surface/audio route | NOT STARTED | — | — |
| P6 | Background/credits/post-play | NOT STARTED | — | — |
| P7 | Integration/regression, post-#134 | NOT STARTED | — | — |
| P8 | One cached APK + device tests | BLOCKED — NEEDS BUILD AUTHORIZATION | — | — |

**Q01–Q50 implementation count: 10/50 addressed in the P1 source implementation; P2 has partial work and is NOT counted complete** (Q03, Q04, Q08, Q09, Q11, Q14, Q19, Q22, Q35, Q48). **Zero P1 changes have Android APK/TV device verification**. Some behaviors already exist in Legacy; inspect and classify them during P0 rather than duplicating code or claiming they are absent. “Committed” ≠ “compiled” ≠ “APK built” ≠ “device tested”.

## Change log

- **2026-11-22 / planning:** Owner selected Option A: GitHub checkpoints and fresh chats, one bounded work package per chat. Approved all 50 design choices, with Q50 Option A. Requested creation of canonical GitHub spec, tracker, package boundaries and preservation rules. Explicitly prohibited implementation and APK workflow during planning.
- **2026-11-22 / documentation:** Created master decision register, bounded work-package plan and this progress tracker. No feature code, CI trigger or APK run requested.

## Immediate next action

**P0 and P1 source checkpoints are complete; P2 is now partially implemented and MUST be finished before P3 begins.** Start from this branch's latest HEAD; retain P1 source/session gates and the eight protected post-#134 refinements. **Do not infer native/TV validation from source checks.** At the first authorized future native/Android environment, run `bash tools/android/preflight-c.sh`, the targeted P1 harnesses and Android compilation; at authorized P7/P8 device QA check native/Media3 track restoration, source selector focus/reload, Q22 versus Q44 pause, first frame, ARC and saved Resume. No workflow or APK may be triggered before a separate explicit build authorization. P8 remains blocked.

**P0 known integration issue:** Android `src/video_android.c:video_buffer_fim()` currently returns nonzero only for live streams, while `src/player.c` uses that API for the VOD buffer rail; `src/video.c` updated after #134 is a non-Android target path. See [P0 baseline](PLAYBACK_50_P0_BASELINE.md) and ensure P4/P7 resolves and tests actual Android VOD buffer endpoint. The early native syntax preflight omits several newly changed files; add coverage before P8. Linux compatibility of subtitle C harness scripts also needs checking. Full detailed Q01–Q50 prose document is not yet archived in GitHub; the in-repo master register is condensed.

- **2026-11-22 / P2 source checkpoint:** Added reusable native animated loading arc, source-scoped Media3 startup phase events, measured byte-rate and P2P engine telemetry, first-frame gated fades and native tests. This is NOT the complete P2 acceptance gate; see appended P2 checkpoint.
- **2026-11-22 / P1 source implementation:** Completed 13 scoped source commits, identity harness compilation and 32/32 source assertions; see detailed checkpoint. No GitHub Actions, APK or workflow changes.
- **2026-11-22 / P0 source baseline:** Verified HEAD `5a9975c` and source commit `6171645`, reviewed build #134 source `bad57b2`, compared 26 post-build commits / 19 changed paths, inspected native, Android, test and workflow files; committed [baseline report](PLAYBACK_50_P0_BASELINE.md). Found Android VOD buffered-end gap and fast-preflight coverage issues. **No feature code, tests, native compilation, CI run or APK triggered.**

## P1 checkpoint — 2026-11-22

- **Assigned package:** P1 — Playback State, Source Identity & Recovery, Q03–04, Q08–09, Q11, Q14, Q19, Q22, Q35, Q48. **P2 and subsequent packages not implemented.**
- **HEAD before P1:** `4f3ab0fc26d141ec7a2562798f9967d5464ccd91` (completed P0 handoff).
- **HEAD at P1 source completion (before this documentation checkpoint):** `0df37f522974e789dc9b7ee3a6fe9ac0dd099cdc`.
- **Source commits, in chronological order (full SHA):**
  1. `abe5479bf39ffefe4f6d1017b72f33805d8aec1c` — immutable addon/release/file identity; validate both source URL renewal lookup and publication; pure C unit test.
  2. `fd7d46afdea9038010dbe682a0a076728f5c0307` — capture Media3 listener generations across the C/Kotlin JNI boundary; reject stale playback events and queued source-mismatched pauses.
  3. `6abd9ee9860c40a1f30c2deeecce088715973b9a` — stop Android automatic source substitution after a selected VOD fails; open manual recovery selector, cancel pending work on Change Stream and retain playback time.
  4. `73601ba9ca359e9554e93c8a1637afa8daba7bd6` — preserve slowly progressing REMUX buffers, Android background pause, Q22 same-source recovery autoplay/new-pause override.
  5. `ccae707fb4f8f9c4dcdfdcc045dae415cbdf1333` — preserve compatible original audio/subtitle track identity, zoom and time across explicit same-episode stream replacement.
  6. `2cb7e23bb8442b5f76a1ae43ed36ea7717d45033` — platform-scope Android recovery, pause old Media3 before asynchronous decoder release; commit source contract harness.
  7. `a353155c7ae8c4a918d54607fb7ae4e05aac2a61` — give explicit Change Stream precedence over a completed but not yet applied URL-renewal worker.
  8. `419e9cf8bacfc9c5a2548eb616b9520da3e65363` — new user pause parks scheduled same-source reconnect; actual buffer progress vetoes false recovery timeout.
  9. `5a0af64d73cd7aa37d9e90c79b62d0cb41050693` — retain saved Resume failure ownership and user-focused source selection.
  10. `1eca00f9db48432025327f917c97ffd8f87362fa` — Q11 refresh selector and focus first **available** result when it arrives, never auto-start another source.
  11. `8fe76e3743d42748515e8d26d21aedb956114792` — preserve last verified position across failure-to-manual-selector handoff scoped by exact episode/title.
  12. `5bf08f0b1e49499ea2dc00c2431c75bb5a451520` — cancel saved recovery position on exit and preserve non-Android selector semantics.
  13. `0df37f522974e789dc9b7ee3a6fe9ac0dd099cdc` — deny missing/zero-generation native playback events; require current session identity.

**Changed source files:** `src/app.c`, `src/player.c`, `src/player.h`, `src/streams.c`, `src/streams.h`, `src/video_android.c`, `src/video.h`, `src/fonteid_p1.h`, `android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt`. Added `tests/playback_p1_identity.c`, `tests/playback_p1_identity.sh` and `tests/playback_p1_contract.py`.

**Tests/checks actually executed:**
- **PASS:** GCC native C11 compilation and execution: `cc -std=c11 -O1 -g -Wall -Wextra -Werror tests/playback_p1_identity.c -o <temporary-binary> && <temporary-binary>`. Tested a materialized standalone copy of the committed C header + harness, not the full repository.
- **PASS:** Clang native C11 compilation/execution with `-fsanitize=address,undefined` and the same strict warning flags; printed `P1 immutable release identity: PASS` without sanitizer findings.
- **PASS:** 32/32 source-level assertions against live GitHub files at HEAD `0df37f5` via connector-driven checks (release identity, both renewal checkpoints, JNI signature and strict generation, queued cancellation, no silent VOD substitution, refreshed/manual selector, position/track/zoom handoff, pause precedence, progress watchdog and unchanged protected files).
- **PASS:** Balanced conditional preprocessor directives in seven modified C/header files: `app.c`, `player.c`, `video_android.c`, `streams.c`, `player.h`, `video.h`, `streams.h`. This is **not** a native application compilation.
- **PASS:** Branch compare from P0 baseline found **zero changes** to `.github/workflows/*` or `docs/PLAYER_PENDING_REFINEMENTS_AFTER_134.md`.
- **Committed but not executed in repository environment:** `python3 tests/playback_p1_contract.py`; `sh tests/playback_p1_identity.sh`. The equivalent native C identity exercise and 32 source assertions were performed from materialized sources/connector data.
- **Unavailable, NOT PASSED:** Full NDK C syntax preflight, Media3/Kotlin/Gradle compilation and runtime Android TV regression. The execution environment had no authenticated checked-out repository and no Android NDK/Gradle/Media3 build setup. **No APK was built or tested.**

**Preservation and limitations:** P0 and all eight unbuilt after-#134 refinements remain in source; no P2 cinematic/UI or P3 overhaul and no APK workflow change. No real device verified first-frame/audio focus, selection timing, newly selected track compatibility, background lifecycle or network renewal timing. Initial/saved Resume failure recovery is intentionally user-controlled; refreshing the picker must never be construed as permission to auto-play a sibling source. Source-compilation validation is a follow-up gate before declaring an APK integration green.

**Build/CI:** `NO` — neither GitHub Actions nor an APK build was triggered; existing workflow trigger was untouched.

**P2 readiness:** **Ready to begin P2 source work on request**, keeping P1 source-level checkpoint intact. **P1 is not device-certified.** Full native/Android/TV verification remains mandatory at the first authorized compilation/device gate, and P8 requires independent authorization.

## Mandatory checkpoint template for future sessions

- Session date and assigned package:
- HEAD before / after:
- Exact requirement IDs addressed:
- Files changed and commit SHAs:
- Checks executed (command, outcome, limits):
- Existing features/regressions checked:
- Not verified / TV-only / known blockers:
- Remaining scope and next precise action:
- Workflow/build triggered? (default NO):


## P2 checkpoint — 2026-11-22 (PARTIAL; continue P2 only)

- **Assigned scope:** P2 — Cinematic Loading, Spinner & Telemetry. Requirements Q01–Q02, Q05–Q07, Q10, Q12–Q13, Q15–Q18, Q21, Q23–Q29, Q34. **Do not start P3 until this package's acceptance gaps are cleared or expressly handed off as blockers.**
- **HEAD before P2:** `2bec292057ec304de777dc897169bc83a441e0a3` (P0 and P1 documented source checkpoints).
- **HEAD at latest source/test checkpoint before this tracker update:** `5eca8ab4db09fa09c8e4a703cbade682aadc183e`.
- **Source/test commits, chronological, exact SHA (22):**
  1. `a277b053757ef31d4b469d1ba0e26c6a27f57b57` — portable official-geometry arc timing and reduced-motion fallback.
  2. `dcbb0cc1360b6d257d600b5e48751c2b3899c8b3` — replace shared native 12-dot spinner with reusable rounded arc.
  3. `91dae50ec025d60d70044cce3493a81c4ebe0068` — expose shared arc rendering API.
  4. `51887fa8f8445bca2f7adf04e03815f05fd3dc64` — initial frame-gated exit instead of forced timeout.
  5. `7b794211f7892be746b423b251bc3bc46a473b4c` — static cinematic logo, independent entrance fade, no fake HTTP percent, manual source escape.
  6. `41b896345b3fb550585137e286835570d41e538c` — measured-byte download rate, ~2.5s smoothing, honest display units.
  7. `134987350db55d35b705eda1c06ebef597fda44a` — generation passed through Kotlin telemetry JNI.
  8. `8be4eaab295ff112009fb18d713e69b6f74e73c3` — source-generation guard and native HTTP rate sampling.
  9. `bb2d92acdcd34d74c7dfacd9a881aa9208d11911` — public Android measured-rate getter.
  10. `edd93e0159987b0d8e33c305d797b999fb5e59b6` — cinematic/rebuffer speed readout and real engine peer counters.
  11. `834ce35dd1e1a40ff8115a48b16dab31833e8406` — start 200ms exit timer at verified frame readiness, not prior READY.
  12. `45734b9c1384d1d1b179962fb3146fd5966b3b9b` — clarify frame-gated cinema comments.
  13. `775c7c1e35e713c5f33d35765688ffab2d326f33` — event-driven Media3 build/detect/starting phases plus positively identified audio-only.
  14. `1c5e3d39a3655f541c007ffacb62c6f6a7f76223` — guarded phase/audio-only JNI event handling.
  15. `bac20c28e77a4dfe6369581cacf82dd4155ddcaa` — correct audio-only callback order before READY notification.
  16. `7b89a30e38840d8db7ada42921d54e261e7ca46b` — public phase/audio-only read APIs.
  17. `d8b85fe0796fb64c722d7b9ca4aa56dcf083584c` — detailed/cinematic status labels, remove residual title logo movement, allow positively classified audio-only handoff.
  18. `7c185ce8ef235db7734910bea87b3d1b5bb9b8dc` — no video-frame wait when verified audio-only.
  19. `3d607defa7b6443215b1dcb1d8c0399d114e4c88` — genuine optional loading diagnostics and shared status helper.
  20. `bc9d9a8c8d1534f3430994440d7b6e50edd29015` — native P2 arc/download/first-frame timing test harness.
  21. `a12dd0b971bc978c4ec31aa1709ef170af097dbe` — host-only C11 build/test script with temporary SDL type stub.
  22. `5eca8ab4db09fa09c8e4a703cbade682aadc183e` — remove stale 4-second forced timeout declaration and comments.

### What the committed code does (source level, not device-certified)

- Extends native Legacy's existing cinematic backdrop and logo/text fallback instead of touching the Dynamic Clock selector, and preserves the saved Off / Art / Blurred Art choice. Logo artwork itself is static; the playing/loading arc animates.
- Existing shared `plrui_anel`, `plrui_anel_solto`, `plrui_respira` paths now use native arc math adapted from official NuvioTV dev's `LoadingIndicator.kt`; it is usable by existing callers including Seekr/player and DV steps. Reduced motion displays a non-spinning static arc.
- Android tracks real Media3 build/detect/start phases through source-generation checked JNI events. Generic delayed `Só mais um instante…` is removed. **A Fetching subtitles status is intentionally not claimed:** only show it after instrumenting a real subtitle fetch.
- Android HTTP download rate uses actual measured `ParaleloDataSource` progressive bytes, 1s samples, ~2.5s smoothing and B/s/KB/s/MB/s. HLS/other paths lacking this counter are **unknown**, not zero. Embedded P2P engine reports `baixando_bps` and `pares` when active; no P2P progress percentages are guessed.
- The existing optional detailed loading card now includes real downloaded bytes, buffered seconds and peer/speed info when present. Startup and rebuffer overlays reuse the telemetry helper. Ordinary rebuffer retains the existing video plane with a small arc; reconnect indicates the distinct `Reconnecting…` state without a pill.
- The native cinema has a ~200ms entrance opacity ramp and a distinct 200ms exit ramp that starts **only at actual frame + stable Android surface**, or positively detected audio-only. Missing frame does not trigger a blind 4-second reveal. Down can focus the 15-second `Change Stream` escape; OK changes source only after explicit focus, otherwise reveals loading details.
- P1 source-generation validation for playback/telemetry, identity-safe source choice and cancellation, no automatic sibling substitution, saved resume, selected track/position recovery are retained in source. The eight post-#134 unbuilt refinement groups and Dynamic Clock architecture remain untouched.

### Verification actually run

- **PASS — GCC, Clang, and Clang ASan+UBSan host C11** using `-std=c11 -O1 -g -Wall -Wextra -Werror -lm` (sanitized run adds `-fsanitize=address,undefined -fno-omit-frame-pointer`): manually materialized exact relevant source logic from `src/abrecine.c`, `src/loading_arc_p2.h`, and `src/loading_telemetry_p2.h` with a small SDL/animation dependency stub. Both executables printed `P2 arc, measured download, first-frame/reduced-motion timing: PASS`. This is NOT a native application build or a verified direct checkout of the GitHub tests; running `sh tests/playback_p2_timing.sh` against an authenticated checkout remains required.
- **PASS — 30/30 live-repository source assertions** at HEAD `5eca8ab...`: original P1 identity/selector entry points present, shared arc and static logo, real-frame gating, audio-only and phase events, JNI generation contract, measured speed source and units, no fake HTTP buffer%, 15-second escape, rebuffer/reconnection visuals, reduced-motion and detail diagnostics, saved post-#134 document, test harness present.
- **PASS — 7/7 preprocessor conditional balances** on changed C/C headers: `player.c`, `plrui.c`, `abrecine.c`, `video_android.c`, `video.h`, `abrecine.h`, `plrui.h`. Not equivalent to compiling these complete files.
- **NOT RUN / NOT PASSED:** full C/SDL build, NDK preflight, Kotlin/Media3/Gradle compilation, Android TV playback and HDMI ARC testing. Local runner has GCC/Clang but no authenticated full checkout, Android SDK/NDK, or device. No device screenshot comparison against official NuvioTV loading backdrop.
- **Build/CI**: No workflow trigger file, workflow YAML, GitHub Actions invocation or APK build changed/run. The build remains **unauthorized**.

### Open P2 acceptance gaps — do not mark P2 complete

1. **Q01/Q17/Q23/Q34 design accuracy:** verify cinematic backdrop crop/top-end alignment, official four-stop full-height black gradient, logo dimensions, and source-accepted timing against official dev `LoadingOverlay.kt`; current native backdrop uses prior Legacy ramp rather than a pixel-verified match. Inspect transition layering with the Dynamic Clock selector (should fade after explicit choice, not upon Play). Verify missing artwork fallback and avoid transient selector flashes.
2. **Q02 stage completeness:** wire true subtitle-fetch and actual P2P stages from real callbacks (not a time estimate). Status transition/crossfade parity and all startup types must be verified. Some states are currently coarse.
3. **Q18 actual P2P readiness percent:** existing `P2pmEstado` reports bytes/sec and peers but NO verified torrent readiness/selected-file buffer percentage. Add genuine metrics to engine/API and clamp only the verified incomplete value near ~95%; until then the progress bar stays absent. Remote Stremio server may have no comparable metric.
4. **Q24 HTTP telemetry coverage:** current actual socket counter covers `ParaleloDataSource` progressive requests; HLS, DASH, serial HTTP and other Media3 data sources report unknown. Add accurate counters via per-generation transfer listeners/data-source instrumentation without double counting or creating bitrate guesses.
5. **Q05/Q06/Q12/Q13/Q28/Q29:** native first-frame fade waits for rendered frame and stable surface, but **audible audio is not yet held until visual readiness** for video tracks; do not claim perfect A/V-synchronized reveal. Verify source-agnostic seek preservation, last decoded frame over rebuffer/reconnect/seek, audio-only detection and error handling on TV including HDR/DV surface recreation; P1 reconnect may still replace an Android SurfaceView and expose a brief black frame.
6. **Q26/Q27/Q10:** verify 15s Change Stream focus, OK-details priority, Back cancellation and pending URL renewal generation on TV; spinner/accessibility/reduced motion across *all* app non-player loading sites. Global refactor replaced the common plrui island spinner API but not necessarily every distinct custom spinner.
7. Run full `sh tests/playback_p2_timing.sh`, `bash tools/android/preflight-c.sh` and P1 harnesses against a working checkout where possible. Review full native and Kotlin compilation errors before any APK attempt; all post-#134 source features must survive.
8. **Do not progress to P3**, do not mark P2 complete or start an APK workflow. On the next authorized session, continue from the current tracker HEAD and the first unfinished P2 acceptance item; use small focused commits and update this tracker with exact new SHAs/checks. Follow the package boundary.

**P3 handoff:** NOT READY. P2 incomplete and awaiting additional source work/verification. Existing P1 is source-complete but unbuilt, and P8 remains blocked until distinct APK authorization.


## P2 continuation checkpoint — 2026-11-22 (incremental source; NOT complete)

**Branch:** `feature/official-stream-selector` in `nerdfxzxl/nuvio-native-legacy`.
**Verified initial HEAD:** `8bb26e5b5b15a8c0e65f751582a05db0ef2f010d`.
**Last new implementation/test HEAD before THIS tracker edit:** `b495772657c1451144167ddc992fc868f505c6a7`.
**Scope:** P2 ONLY. P0/P1 and all 22 previous P2 source/test commits are preserved and not repeated. The eight unbuilt post-#134 groups are retained, and P3 was NOT started.

### Additional focused source commits (chronological)

1. `a43c3d93cb03f57929d06c3eac550e57827d3b95` — `src/gfx.h`, `src/gfx.c`, `src/player.c`: dedicated cinematic backdrop crop with **TopEnd** anchoring, full-height native shader matching official NuvioTV dev black-alpha stops **0.3019608 @ 0, 0.6 @ .35, 0.8 @ .70, 0.9019608 @ 1**, replacing the previous half-screen fade. Preserve saved Off/Art/Blurred Art and static title logo. No extra independent logo animation.
2. `cfb191fb7a79117cf565f2748002d758ec1d9918` — `NvPlayer.kt`: session-owned `TransferListener` socket payload counter for serial HTTP, HLS/DASH manifests/segments and live HLS revalidation. When the parallel source was used, its existing true socket counter remains authoritative; parallel transfer delivery is explicitly excluded to avoid counting bytes twice. Prefetch/released sessions own separate counters; JNI generation filter preserved. Source wiring has **NOT** been Media3-compiled.
3. `759000abd9cc0f1a5323c03bf6faaf2ccc0f9d0b` — `NvPlayer.kt`: mute *audible output only* while Media3 audio/video decode clocks and loading continue, then release after actual first video frame AND stable hardware surface; verified audio-only release in READY. Native session/audio volume is preserved through a requested-volume variable. The existing warm frame overlay is not dismissed before stable surface handoff. ARC/passthrough/HDMI behavior remains **unverified**.
4. `2d5858bfc5b0312723b3bfe9097e0450b73efcdf` — `src/p2pmotor.h`, `src/p2pmotor.c`, `src/player.c`: explicitly published engine lifecycle states (starting, metadata, selecting file, preparing stream, first-byte probe, ready) guarded by torrent request generation and actual operations/events. Still no unsupported torrent selected-file progress percent.
5. `8dfff566953083169d704f41dcf978f5f0ccb4dd` — `src/legenda.h`, `src/legenda.c`, `src/player.c`: `Fetching subtitles…` only while the current external subtitle download/parse or owned prefetch is actually in flight; tracked under subtitle generation mutex, cleared on completion/failed thread creation/switch-off. Old fetches cannot mark a later generation as fetching.
6. `88bc3155650374dd8f876c2f8fcd63a75d0146b8` — `src/player.c`, `src/p2pmotor.c`: state-label 180ms crossfade resetting at every new cinema opening (instant with reduced-motion); clear P2P state on engine startup failure as well as explicit cancellation.
7. `b495772657c1451144167ddc992fc868f505c6a7` — `tests/playback_p2_source_contract.py`: a repeatable, read-only source contract script for the next authenticated checkout, checking visual shader stops, real telemetry selection, generation isolation, audio-only/first-frame gate, 15s escape and P1 preservation.

### Source checks actually performed this continuation

- **PASS: 27/27 live-repository JavaScript structural assertions** fetched directly from the actual feature branch at `88bc315...`. Checked shader modes, exact gradient knots/alpha stops, TopEnd crop, half-height veil removal, static logo, per-cinema crossfade reset, genuine subtitle fetch/prefetch and lifecycle, P2P milestone generation/cleanup, honest unknown percent, real HTTP transfer events vs parallel counter (no double count), per-resource ownership, playlist revalidation coverage, first-frame mute/unmute, audio-only exception, 15-second manual escape, shared loading arc/reduced motion, source-generation JNI and presence of the post-#134 document. This confirms expected source snippets and wiring; it is **NOT compilation, logic execution on device, exhaustive control-flow proof or pixel parity**.
- `tests/playback_p2_source_contract.py` is committed but **NOT executed** against a checked-out repository in this environment. `sh tests/playback_p2_timing.sh`, P1 harnesses, preflight, Kotlin/Media3 compilation and all device tests remain **NOT RUN in this continuation**. Historical 22-commit P2 checkpoint test claims are unchanged.
- **No GitHub Actions, Gradle/NDK compile, APK build or Android TV smoke test was invoked.** This does not waive the explicit P7/P8 gates.

### Reconciled eight P2 acceptance gaps (source vs verification)

1. **Q01/Q17/Q23/Q34 cinema:** native four-stop full-height gradient and TopEnd crop **IMPLEMENTED in source**; blur/off preferences, native logo path and separate 200ms ingress/first-frame exit retained. **OPEN:** actual visual/pixel parity, whether logo sizes correspond to official 320dp x 180dp on the TV, fallback poster/missing artwork, dynamic-clock fade ordering and HDR/blur layering, all to be checked on device. No claim of pixel-perfect verification.
2. **Q02 real loading states:** **IMPLEMENTED for native external subtitle network fetch/prefetch, Android Media3 load states, P2P embedded engine stages, per-cinema 180ms text crossfade.** **OPEN:** source types outside those hooks, remote Stremio P2P status visibility, and actual animation/transition correctness on device.
3. **Q18 genuine P2P readiness percent: SOURCE BLOCKER, NOT IMPLEMENTED.** `nuvio-engine` patched API version 4 offers total payload bytes, B/s, peers, and cache/window stats, **but not contiguous selected-file piece readiness**. Its total bytes are not a valid selected-file readiness percent. Required next source work: extend the engine + C ABI or another proven selected-file piece meter, identity-bind and generation-guard the result, establish its selected-file contiguous playable buffer denominator, show an exact percent while incomplete (only then clamp to 95% pre-ready); remote Stremio may remain unknown. Until then use a truthful spinner and no percent. Do not fake this acceptance requirement.
4. **Q24 HTTP telemetry:** **IMPLEMENTED source wiring** for nonparallel Media3 network TransferListener callbacks, including HLS/DASH/serial/revalidation; existing progressive parallel real socket total remains authoritative. **OPEN:** actual Media3 compilation, cache-hit exclusion, redirects, adaptive live streams, true byte counter reporting and JNI speed/reset behavior on TV/network. No simulated throughput accepted.
5. **Q05/Q06/Q12/Q13/Q28/Q29 A/V + held frame:** **IMPLEMENTED source gate** for output mute until stable first video frame; audio-only exception and retained warm frame through stability. **OPEN SOURCE/DEVICE:** deterministic last decoded frame during arbitrary seek, recovery and Android SurfaceView replacement (including HDR/DV) is **not guaranteed** by the current source; the device may briefly display black when a SurfaceView is replaced. Must inspect/reconcile native/Media3 old-frame retention without overdraw/quality regression, then test audio timing, frame readiness, pause/resume, error and ARC/passthrough hardware in P7. Do not label this gap solved.
6. **Q26/Q27/Q10 remote/global indicator:** existing source still has 15s `Change Stream`, Down focus, Up collapse, explicit OK selection, Back dismissal/cancel path, detailed loading card and shared reusable arc/reduced-motion. **OPEN:** exhaustive app-wide custom spinner audit and actual D-pad/Back focus, cancellation, display integration/device accessibility check.
7. **Test gates:** 27/27 live source assertions **PASS**, new reusable source script **ADDED (not run)**. **DEFER** direct `python3 tests/playback_p2_source_contract.py`, `sh tests/playback_p2_timing.sh`, `sh tests/playback_p1_identity.sh`, native C/NDK syntax and Kotlin/Media3 compilation to an authorized authenticated P7 checkout/integration phase. Defer TV device, HDMI ARC, playback on real HTTP/HLS/DASH/P2P, HDR/DV, subtitle and remote tests to P7/P8. No device pass claimed.
8. **Package boundary:** **P2 is NOT source-complete** due at minimum #3, deterministic recovery frame in #5, global spinner coverage in #6 and unresolved visual/compile integration evidence. **P3 MUST NOT START.** Do not trigger GitHub Actions or an APK without distinct authorization; P8 remains blocked.

### Exact continuation instructions

Start from the **current branch HEAD after this tracker commit**, verify HEAD, reread these five fixed documents and the P2 section, preserve 22 previous P2 commits plus the 7 new focused commits above. Prioritize (a) truthful selected-file torrent piece readiness from engine ABI (do not infer from total downloaded bytes), (b) recovery/seek last-frame persistence across Android SurfaceView recreation with no focus/overlay regression, (c) app-wide loading arc and cancel navigation audit; then source build/test verification when tools are available. P3 is explicitly out of scope. If all source gaps close, update this checkpoint and only then mark P2 source-complete; hardware/device certification remains P7/P8.

**Build/CI:** NONE. **Current source status:** PARTIAL, resumable. **TV verification:** NONE.


### P2 follow-on loading consistency audit — same 2026-11-22 session

- `75231f6fefd95c5cfa845be9f62afc014686a131` — `src/perfilsel.c`: removed the actual remaining legacy twelve-dot rotating profile-selection loader (including its independent reduced-motion brightness pulse). It now calls shared `plrui_arco` with the previous dark avatar veil. Profile focus, selection, PIN and background behavior were not edited.
- `9a340eb48feb0ed96133af4b790b1a410641ab68` — `tests/playback_p2_source_contract.py`: added a source contract specifically rejecting the profile twelve-dot loop and requiring the shared arc.
- Audited **14 candidate UI source files** via authenticated branch content inspection: `app.c`, `atividade.c`, `home.c`, `perfilsel.c`, `addonsui.c`, `busca.c`, `streams.c`, `listas.c`, `plrui.c`, `episodios.c`, `relogio.c`, `detail.c`, `guiaaddons.c`, `homevazio.c`. Many `GFX_ANEL` calls in Home/Details are **focus/border rings**, not loaders, and must NOT be blindly replaced. The Source-picker reload **arrow** rotation is a semantic button animation, not a generic loading spinner. This is a focused candidate audit, **not** an exhaustive search of all 364 C source files.
- **Live source checks:** prior 27/27 pass at `88bc315...`; additional 6/6 profile/test/tracker assertions pass at `9a340eb...`. The Python regression script itself remains committed-but-NOT-RUN; a full checkout, native/Gradle compilation and device verification remain missing.
- **Status unchanged: P2 PARTIAL.** Selected-file P2P percent, deterministic seek/reconnect held frame and exhaustive custom loader review remain open. Do not begin P3 or run Actions/APK. Continue from the HEAD of the next progress tracker commit and retain these exact commit SHAs.


## P2-A bounded checkpoint — 2026-11-22 (Q18 selected-file torrent readiness)

**Assignment:** P2-A ONLY, no P3 or unrelated P2 changes. **Initial verified HEAD:** `4e030efbd7b1278388c271cd5eefffcfd5fb944e`. **Last source/test commit before this tracker entry:** `1bf4efb1f7a6f54d189f7c48f25e5a5fae4ca55f`. The next GitHub commit of this tracker is the final P2-A checkpoint; always verify live HEAD again before resuming.

**Outcome:** truthful selected-file verified-piece telemetry is **CODED (source-only)**, but **an honest selected-file current-playhead contiguous playable-buffer readiness PERCENTAGE remains BLOCKED and is NOT displayed**. Q18 is **PARTIAL**, NOT implemented/completed. Do not infer playback readiness from total torrent payload bytes, entire-file verified fraction, elapsed time, peer count or disk cache/window occupancy. Existing unknown/indeterminate UI remains for percent.

### Actual engine/API evidence and precisely identified blocker

- Pinned source: `NuvioMedia/nuvio-engine` commit `02938d7`, wrapped by local `tools/p2p-motor/janela-334.patch` (API 4); `tools/p2p-motor/pasta.sh` reproduces it. **Correction to the earlier P2 checkpoint:** upstream API **already exposes** `nuvio_engine_get_stream_stats` and `nuvio_engine_stream_stats_init_sized` (present in upstream API 3 and retained by API 4). Its `nuvio_engine_stream_stats` has `file_index`, `file_size`, `contiguous_ready_bytes`, `verified_file_bytes`, and demand/scheduler fields. Legacy had only bound `get_stats` (global download/peers) and was not reading that per-stream API.
- Upstream `src/torrent/libtorrent_stream_bridge.cpp::progress_for` uses `lt::torrent_handle::status(query_pieces)` and piece/file overlap: `contiguous_ready_bytes` is verified **from selected FILE OFFSET ZERO**, and `verified_file_bytes` includes all verified selected-file pieces regardless of gaps. These are real piece-availability measurements, **not** a playable buffer window at current playhead. The #334 disk streaming window explicitly punches out earlier pieces and calls asynchronous `nuvio_drop_piece`; a backwards seek can therefore reduce/reset the start-of-file contiguous count while later playback is healthy. The asynchronous forget path can transiently lag disk eviction. A zero-length prefix also does not prove no future playable pieces.
- Existing `primary_demand_start/end`, scheduler/critical-window configuration and global `window_resident_bytes` are **not an authoritative selected-file current-playhead byte range plus verified contiguous ready bytes plus readiness denominator**. The C API lacks a reliably identity/timestamp-bound *current playback byte offset and target* (including post-seek/parallel demands and eviction) from which to calculate a non-synthetic readiness %. Even dividing `contiguous_ready_bytes` or `verified_file_bytes` by `file_size` would label file completion as startup buffering, which is false. The remote Stremio P2P backend also does not expose this embedded engine API.
- To unblock a **real** progress bar: extend the engine/backend and versioned C ABI to publish, for the exact active `stream_id`/`torrent_id`/`file_index`, a well-defined *current read-position contiguous verified playable range* (account for skipped/evicted pieces and concurrent seeks), and an **actual readiness target** whose origin is the engine/player's real demand/startup requirement rather than a constant guessed threshold; generation/seek identity must invalidate old readings. Cover initial buffer, seek, eviction/refetch, stream reuse, invalidation, failures, and no-data tests. Only after trustworthy numerator AND denominator can UI display a % and cap incomplete real progress at ~95%; for unknown/remote P2P continue using spinner/status/peers/speed. No engine patch was made because the required semantics/target cannot be established honestly from current APIs.

### Committed P2-A work (12 chronological source/test commits)

1. `849660ee051ec75ff7af47909a68d2f2989cdbe8` — `src/p2pmotor.h`: expose selected-file measured validity/index/size/prefix/all-verified bytes and optional per-stream operations callback; explain unknown percent.
2. `5824a41cef0c9b7798dfc521adcc862c17c30969` — `src/p2pmotor_motor.c`: bind `get_stream_stats` and sized init in static + dynamic ABI; validate true piece counters without global payload totals.
3. `b85ea33251cad1d9d2eb150b808771c6385fb6a1` — `src/p2pmotor.c`: async watchdog copies exact stream ID, selected file index/size and torrent request generation, queries engine outside lock with motor lifetime guard, rejects stale/mismatched results; clears on cancel/replace/failure.
4. `fcf4c11cb521d8ace97329b88481f152c61f8312` — `src/player.c`: show `Verified from start: X / Y MiB` in **optional** expanded details only for validated embedded P2P; keep indeterminate percent and existing spinner.
5. `e708438e70777a9d1554ebefa88a253df29bfb43` — `src/p2pmotor.c`: clear measured state immediately when async engine teardown begins.
6. `0dea27ce2eac69e8314ced52c4abfb9e75257162` — `tests/playback_p2_source_contract.py`: correct obsolete Q18 assumptions, check selected-stream engine API and identity guards.
7. `c1c4a8a8dc83deaf469395a1ed5c24aa1f6eab80` — `tests/p2pmotor.c`: explicitly set optional callback NULL in existing fake engine.
8. `84b1572893fee90c4c507afbd55576aab41f9c25` — `src/p2pmotor.h`: extract pure strict selected-file index/size/byte-bounds validator.
9. `9369667d3cb2c23dd5d9818aadcb7839955df8bc` — `src/p2pmotor.c`: use shared validator in the engine watchdog.
10. `0eeaa8be701be62d81ea1af9d550f80943c0f556` — `tests/playback_p2_piece_readiness.c`: portable host C11 test for exact file identity, genuine zero versus unknown, sparse pieces, invalid counters and full verified file.
11. `6286e1f5a480aefed7da4962d2a0f8f57252f33e` — `tests/playback_p2_piece_readiness.sh`: compiler/runner, no Android build or workflow.
12. `1bf4efb1f7a6f54d189f7c48f25e5a5fae4ca55f` — `tests/playback_p2_source_contract.py`: update contract to reusable strict selected-file validator.

### Verification and preservation — state only what actually ran

- **PASS:** 37/37 live GitHub source assertions evaluated in the active connector session against actual current branch source. Covered pinned API4 patch, static/dlopen symbols, real per-stream measurements, missing/invalid states, selected file/stream/generation lifetime guards, cancel/stop/replace reset, optional native details, absence of fake percentages, protected P1/P2 pieces and protected first-frame/subtitle/network logic. This was **not** execution of the checked-in Python file.
- **PASS:** 6/6 changed C/header/test sources have balanced `#if/#else/#endif` directives, static text check (not full native syntax).
- **PASS on separately materialized *mirrored* host-only helper/test fixture:** GCC C11 and Clang C11 with `-Wall -Wextra -Werror`, plus Clang ASan+UBSan, all printed `P2-A selected-file piece snapshot identity: PASS`. These compile the same pure validation function and C test cases but **are not compilation of a GitHub checkout**. The committed `sh tests/playback_p2_piece_readiness.sh`, `python3 tests/playback_p2_source_contract.py`, `sh tests/playback_p2_timing.sh` and P1 suite remain **NOT RUN in an authenticated checkout**.
- **NOT RUN / NOT PASSED:** full Legacy C/SDL syntax or NDK preflight, actual embedded C++ `nuvio-engine`/libtorrent build, Android Media3/Gradle compile, P2P torrent playback on TV, real seek/eviction, remote Stremio integration, APK or GitHub Actions. The available container had host GCC/Clang but neither an authenticated repository clone nor network DNS to GitHub, Android NDK/SDK, or physical TV; do not claim a full binary/runtime pass.
- GitHub compare from initial HEAD to source/test HEAD is **ahead 12, behind 0, exactly 8 changed files**: `src/p2pmotor.c`, `src/p2pmotor.h`, `src/p2pmotor_motor.c`, `src/player.c`, `tests/p2pmotor.c`, `tests/playback_p2_piece_readiness.c`, `tests/playback_p2_piece_readiness.sh`, `tests/playback_p2_source_contract.py`. No change to `.github/workflows/*`, build trigger, Dynamic Clock selector, P0/P1 source, Android player, or the post-#134 handoff. No GitHub Actions run dispatched and **no APK built**.
- **Remaining:** Q18 exact buffer readiness percentage is **BLOCKED** by the engine's current-playhead evidence/denominator; full native/Android build and device verification still open. Other previous P2 gaps are unchanged and remain assigned to later P2 work; **DO NOT start P3** from this checkpoint. Next session must recheck the final tracker commit HEAD, preserve all completed commits and implement only another explicitly assigned scope. P8 build authorization remains absent.

## P2-B bounded checkpoint — 2026-11-22 (Q06/Q28 last-frame preservation)

**Assignment:** P2-B ONLY: last displayed video frame during seeks, ordinary buffering, bounded same-source network recovery, and necessary Android `SurfaceView` recreation. **Initial verified HEAD:** `b67d28ae7bc2900bdab43c47613f85a48ac7c565` (final P2-A tracker commit). **Last implementation/test HEAD before this tracker entry:** `df72f40e7112b15d8f829743fe6bf211ba8386f4`. Recheck live HEAD before any follow-on work.

**Outcome:** **P2-B source implementation/checkpoint completed, NOT binary/device verified.** Preserve Media3/ExoPlayer hardware decode and stable `SurfaceView` for ordinary buffering, in-buffer/backward seeks and normal playback. Only an evidence-backed *forward seek beyond Media3's known buffered endpoint* requests an optional, bounded SDR PixelCopy before seek. Source-scoped transient frames may also be held across an explicit **same-source** native retry and the already-required TCL-style surface recreation, until an actually rendered first frame and stable surface. Not a guarantee that every codec/HAL avoids black; HDR/Dolby Vision surfaces deliberately **do not** use an inaccurate 8-bit SDR copy.

### P2-B commits, chronological and exact

1. `659298d8f8d36324d753cbe718b3efa952b62961` — `FrameHoldP2B.kt`: pure Kotlin eligibility, measured-buffer-endpoint and callback identity policy.
2. `3049bff29934d235990581327761293694e409c7` — `tests/FrameHoldP2BTest.kt`: 17 host-only cases for SDR/HDR exclusion, unknown/buffer-bound seeks, request/session/player/surface ownership.
3. `a9d5af66b9694ed1440dc81029b0ebf4bc1d8e9a` — `src/video_android.c`: optional distinct JNI same-source recovery call, retain normal manual-source open path, generation-checked surface-invalidated event.
4. `09142cd53e47413078a64962d639bcf0826060ba` — `NvPlayer.kt`: ephemeral source-scoped SDR PixelCopy; 160ms fail-open request; capture prior to forward out-of-known-buffer seek and same-source reopens; exact request/session/player/surface ownership; keep video plane, first-frame and audio gate.
5. `b9cde604eea72da5f99a168c884b5331ed9a2452` — `NvPlayer.kt`: preserve transient frame across a same-source release/reopen; capture before the existing necessary SurfaceView recreation, invalidate stale surface stability and wait for a new first frame. Preserve MStar/Amlogic and DV geometry/startup restrictions.
6. `385cdeec0ac993214868eaa34568a3daf1fb7619` — `NvPlayer.kt`: supersede an old pending PixelCopy seek when newer remote seek arrives; prefer this session's temporary frame to an older warm-cache thumbnail; align callback indentation.
7. `729410e47d995c0c0f4d621a12985b57a8097323` — `tests/playback_p2_frame_policy.sh`: repeatable host Kotlin test runner.
8. `dcc90580a7530a955978cbe6f43cbdd28b71d832` — `tests/playback_p2_frame_contract.py`: static Android JNI/frame/HDR/source-preservation and C preprocessor contracts.
9. `13bd6538f7ff448b7376d94d544cefebe7a86a77` — exact Format A handoff label in the static contract.
10. `df72f40e7112b15d8f829743fe6bf211ba8386f4` — exact protected action-pill handoff label in the static contract.

### Source implementation details and deliberate limitations

- `FrameHoldP2B.mayCopy` rejects missing rendered frame, invalid/foreign surface, HDR signaling or HDR transfer, Dolby Vision mime/decoder, and SDK versions where SurfaceView PixelCopy is unsupported. A successful temporary SDR bitmap is bounded to about 1920×1080 ARGB8888, placed over the **actual current video-plane geometry** (including crop/letterbox) instead of screen-stretched. It is not stored as an HDR/DV replacement, decoder output, another media item, or a new playback engine.
- `FrameHoldP2B.ownsResult` requires matching request generation, Kotlin player session, exact player and exact `SurfaceView` before publishing async capture. Queued seeking additionally uses a seek serial to prevent an old copy callback applying a stale D-pad target. Explicit Back/stop/manual stream switch clears the transient hold; only the native recovery-only entry point can preserve it over re-open. Source identity, position, tracks and P1 renewal/recovery semantics remain controlled by the existing native/Media3 pathways, not by the bitmap.
- Ordinary rebuffer continues displaying the Media3 video plane and the existing small native arc; it never invokes cinematic startup, surface recreation or PixelCopy. For in-buffer/backward/unknown-range seeks the existing hardware surface is retained without an extra decoder reset; Media3's device-specific held-frame behavior is **not guaranteed**. The single `bufferedPosition` endpoint does **not** prove all earlier ranges are buffered; this test only positively identifies some out-of-buffer *forward* seeks.
- New snapshot capture fails open after 160ms or if PixelCopy fails/returns insecure/invalid frame/OOM. It never waits synchronously on the GPU or decoder. The original HDR/DV hardware plane, decoder, signed formats, aspect/geometry workarounds, audio mute until first stable frame, async old-player release and first-frame native gate remain. During necessary HDR/DV SurfaceView recreation, black flashes **may still happen**; no false lossless HDR snapshot/conversion is substituted. Nothing changes audio tracks, passthrough or ARC choices.
- The existing `quadroBitmap` warm-resume cache remains separate and retained, as do Dynamic Clock source selection, native loading arc, P0/P1/P2-A code, unbuilt after-#134 behavior and the JNI generation contract.

### Checks actually executed and evidence

- **PASS:** host `kotlinc` compile plus Java execution of the *exact pure Kotlin policy and host test contents manually materialized from live GitHub source* in a temporary host directory: **17/17 policy checks**. This is NOT compilation of the whole Android project or execution from an authenticated repository checkout.
- **PASS:** **30/30 live-branch source assertions** in a connector-driven JavaScript audit at the last source/test HEAD. Checked Media3 SurfaceView/decoder/audio retention, native optional same-source JNI and three entry paths, native generation filtering, first-frame/surface invalidation, HDR/DV policy, 160ms capture/identity, seek supersession, geometry, rebuffer arc, warm frame and original post-#134 handoff. The analogous committed Python script was **NOT RUN** against a checked-out repository.
- **PASS (static only):** lexer-style balanced Kotlin/C braces and C preprocessor `#if/#else/#endif` nesting in the modified bridge. This is **not** a complete native syntax compilation or Kotlin/Media3 typecheck.
- **PASS:** GitHub compare from initial HEAD to last source/test HEAD: ahead **10**, behind **0**, exactly **6 paths** changed: `NvPlayer.kt`, `FrameHoldP2B.kt`, `src/video_android.c`, `tests/FrameHoldP2BTest.kt`, `tests/playback_p2_frame_policy.sh`, `tests/playback_p2_frame_contract.py`. No `.github/workflows/*`, build trigger, source-picker, P2-A torrent code, or `docs/PLAYER_PENDING_REFINEMENTS_AFTER_134.md` changed.
- **NOT RUN / NOT PASSED:** direct checkout invocation of the checked-in `tests/playback_p2_frame_contract.py` and `tests/playback_p2_frame_policy.sh`; full C/SDL/NDK preflight; real Kotlin/Media3/Android Gradle compilation; frame timing/quality/alpha compositing on TV; HDR/DV panel behavior; decoder/surface HAL resets; ARC A/V synchronization; rapid seek/recovery/stall real-device regression. No authenticated repository checkout, Android toolchain or physical device was available for this session. The GitHub API confirms committed remote content but not any independent uncommitted worktree.

### Remaining acceptance blockers / next allowed action

1. **P2-B runtime verification REQUIRED:** representative SDR in-buffer seek, forward out-of-buffer seek, consecutive held D-pad seeks, long buffering, same-source recover/reopen, explicit different-source selection during capture, Back/cancel, next-episode handoff, multiple aspect changes, and PixelCopy failure. Check that no stale frame remains after resumed video, that an old asynchronous capture cannot appear in the next source, and that `onRenderedFirstFrame` / stable callbacks actually arrive after forced SurfaceView recreation. Some HALs may still flash black. Check PixelCopy memory/latency on low-RAM TV and surface layering under SDL.
2. **HDR/DV is intentionally hardware-only:** confirm correct panel HDR activation, decoder stability, audio sync and whether necessary HDR/DV surface replacement can flash black. Accurate HDR frame retention would require additional platform-specific evidence/mechanism, NOT a downconverted SDR image. **Do not claim guaranteed no black frame.**
3. **Q18 remains explicitly BLOCKED and low-priority:** P2-A's real per-file verified piece stats do not provide a trustworthy current-playhead contiguous playable-buffer percentage/denominator. No percentages added or inferred. Other P2 visual/system integration acceptance and all P7/P8 build/device checks remain open. **P2 stays PARTIAL; P3 MUST NOT START.**
4. **Next step only when separately assigned/authorized:** run host scripts against an authenticated checkout plus Media3/NDK source integration checks, then later real TV verification under a separately authorized build/device gate. Preserve all checkpoints and the post-#134 handoff.

**Actions/CI/APK:** **NONE.** No workflow dispatch, APK build or build trigger change; no P3 and no P2-A rework. **Bounded P2-B session stops at this documentation checkpoint.**

## P2-C bounded checkpoint — 2026-11-22 (Q01/Q07/Q10/Q14/Q16/Q26/Q27)

**Assignment:** global native loading-indicator and remote-navigation audit ONLY; P2-A and P2-B source checkpoints retained; no P3, P2P readiness rework, unrelated playback changes, workflow triggers, GitHub Actions or APK. **Initial verified HEAD:** `4cc66454530c2d7ed0071b1afb10351d11a1b269`. **Last source/test HEAD:** `9e964dd496554ffcaf42e094bc0e3dcdc2070a94`. The subsequent documentation commit is the final P2-C checkpoint; always verify its actual branch SHA before continuing.

**Outcome: P2-C bounded SOURCE checkpoint completed; Android runtime/TV verification remains OPEN, and P2 as a whole remains PARTIAL.** Existing shared `plrui_arco` and `nv_arc_quadro` are still the only native active loading arc implementation. Gray arc rounded caps now use the same opacity as their curved stroke; diameter, 2-second pacing, shadow, reduced-motion fallback and all call sites retain their shared behavior. No new spinner implementation was created. Existing semantic UI animations (Dynamic Clock refresh-arrow rotation, progress/tracks, focus rings, diagnostic shimmer, audio-active bars and home skeletons) were deliberately preserved.

### Global spinner audit — exact source scope, not a device pass

- Using the authenticated branch source, inspected **all 355 root-level `src/*.c`**, **all 40 root-level `src/*.inc`**, and **all 453 root-level `src/*.h`** against spinner/arc/rotation/loader patterns; then examined **18 `src/dts` native source/header files** and **six `src/novidades/*.inc`** UI fragments. Total examined: **872** native root/subdir files; external third-party vendor trees and unrelated JavaScript/Android system splash implementations are outside the bespoke native-UI inventory. The audited tree comes from initial HEAD with source subtree `187f774ab87b907be7bdac0070d9800ea4073c6b`; edited `player.c`, `plrui.c`, `streams.c` and new header were subsequently re-fetched.
- No remaining app-drawn legacy twelve-dot spinner or independent custom native loader was identified among the source candidates. The already-corrected profile loader stays on `plrui_arco`; source `player.c`, `pessoas.c`, `recenviar.c`, `aovivoui.c`, `dvtelaui.c`, `ilha.c` and `plrilha.c` call the shared arc wrappers. `gfx_arco` is the low-level primitive, not an extra app spinner.
- Preserved: selector's semantically rotating refresh arrow and live audio bars, home/details focus borders, screen-geometry transforms, legitimate measured/indeterminate loading rails, and reduced-motion branches. System splash gray dots mentioned in `abertura.h` are an OS-level splash reference, not a native twelve-dot loop. Pattern-based static audit **cannot guarantee every visible frame**; verify perceptual parity on Android TV.

### P2-C exact chronological source/test commits (15 before tracker)

1. `065e104e84adc4510a244c860cd4263969d88993` — `src/player.c`: 15-second Change Stream focus/OK routing; explicit OK diagnostics in cinematic, expanded and fallback loading card; visible underline, no accidental addon/settings activation.
2. `e78c54e175a23fe18f1bd00665f8ac9847ab83b0` — `src/app.c`: on Back during loading/recovery, immediately invalidate pending URL jobs/P2P/diagnostic callbacks; manual Change Stream now cancels accepted-source loading/reconnection, not only unresolved addon state; selector Back can return to detail cleanly.
3. `73357e8b441f8ec356f9385c7924a69ca566a952` — `src/streams.c`: one-shot, row-only focus restoration for prior manually selected source, when still present; no autoplay/selection.
4. `f60e26f9ce34e9bd4499676a3b9d94e7307faa41` — `src/streams.h`: declares focused restoration API.
5. `33ca0a517eee6026f6b769deb7e6f82f7d4ffe20` — `src/app.c`: call that API **only for loading escape**; first-play/default and failed-recovery selector focus logic unchanged.
6. `f2be109b5754516488b6112e965f4291e81b61d4` — `src/plrui.c`: consistent shared arc rounded-cap/stroke opacity.
7. `7bd265312ee4a1fddb79a96f1939e663e22f7ece` — `src/player.c`: loading OK not intercepted by an old info-OSD or source-toast shortcut.
8. `70cf33579fa121aeb52ac8ed4171af4d66173255` — `src/loading_nav_p2c.h`: pure tick-wrap-safe 15000ms/focus confirmation policy for host testing.
9. `256b40d1da1579336981593918e459d1029748ec` — `src/player.c`: use shared policy.
10. `eee756878e9dccf28675e183792a7312f94d6529` — `tests/playback_p2c_nav.c`: 15 C11 elapsed/focus boundary cases.
11. `1df095ce408c0a6285fa31140158420a0b9f2cba` — C11 test correction: exact uint32 wraparound boundary (caught by actual host run).
12. `7e9c9946779d0d0115fe74059f61ecba1bfd5c45` — `tests/playback_p2c_nav.sh`: repeatable local host compiler runner; no CI/APK.
13. `21f2871860f2afb3f6af2b188e0c5edfb1f2faf3` — `tests/playback_p2c_navigation_contract.py`: read-only source regression contracts for shared arc, navigation, session cancellation, P2-A/B fidelity and selector isolation.
14. `c0b1f3770eb6761cae134f22b4e1861f62681f77` — `src/streams.h`: clarify distinct P1 recovery vs P2-C manual row focus contracts.
15. `9e964dd496554ffcaf42e094bc0e3dcdc2070a94` — `src/player.c`: clear stale delayed focus when cinematic loading ends, errors or exits.

### Checks actually executed

- **PASS, 45/45:** connected live GitHub source checks for 15-second policy, Down-focus-only, explicit OK confirmation, no hidden OSD/toast hijack, Back and source generation cancellation, row-only selector focus, shared arc callers, capped/real P2-A telemetry, P2-B PixelCopy/Media3/SufaceView ownership, and original first-frame/HDR guards. This is NOT execution of the checked-in Python contract.
- **PASS, 9/9:** source-level `#if/#else/#endif` nesting balance on P2-C changed C/header/test paths. This is NOT a C typecheck or a native link.
- **PASS on a separately materialized **mirrored** C11 header/test fixture:** GCC and Clang (`-std=c11 -O1 -g -Wall -Wextra -Werror`) plus Clang ASan+UBSan executed **15/15** policy boundary cases, including exact 15000ms and timer wrap. The initial mirrored test exposed an off-by-one *test expectation*, subsequently corrected in GitHub. This was **not** running the committed shell test inside an authenticated checkout.
- **PASS GitHub compare** from initial HEAD `4cc6645...` through source/test HEAD `9e964dd...`: **ahead 15, behind 0, nine changed paths** (`src/app.c`, `src/loading_nav_p2c.h`, `src/player.c`, `src/plrui.c`, `src/streams.c`, `src/streams.h`, and the three `tests/playback_p2c_*` files). No `.github/workflows/` path, Android player/SurfaceView source, P2P engine source, P0/P1 handoff document, or other P3 file was edited.
- **NOT RUN / NOT PASSED:** direct `python3 tests/playback_p2c_navigation_contract.py` or `sh tests/playback_p2c_nav.sh` in an authenticated local checkout; full Legacy SDL C syntax/NDK, Kotlin/Media3/Gradle, JNI link, APK or Actions; Android TV D-pad/Back, first-frame/HDR/DV, buffering/recovery, selector animation, screen-reader or physical HDMI ARC tests. GitHub DNS from host execution is unavailable; no complete authenticated checkout, Android SDK/NDK or physical TV was available. Do not overstate source checks as an integrated build.

### Remaining P2 acceptance gaps after P2-C (P3 NOT STARTED)

1. **Q18 BLOCKED:** P2-A still supplies real selected-file verified-byte evidence, but no genuine selected-file **current-playhead contiguous playable readiness denominator**. Unknown percentage remains indeterminate; no synthetic progress added. No new engine/API work in P2-C.
2. **Q01/Q02/Q16/Q17/Q23/Q34 visual/status parity:** official cinematic gradient/pixel color, static artwork/logo, real event-driven subtitle/torrent/HTTP statuses, title/logo fallback, startup vs rebuffer vs seek vs same-source recovery distinctions, reduced motion and fade ordering still need TV/device evidence. Some stream types/remote P2P hooks are not fully verified. P2-C made no new backdrop, logo animation or false readiness rail.
3. **Q24 and network telemetry:** real HTTP/HLS/DASH/serial byte-count correctness and cache-hit/redirect behavior require actual Media3/Android/network tests, not simulated throughput.
4. **Q05/Q06/Q12/Q13/Q28/Q29 P2-B validation:** real frame hold during in/out-buffer seeks, same-source network recovery, surface replacement, hardware decoder and A/V sync remains device-dependent; HDR/Dolby Vision still deliberately skip an inaccurate SDR snapshot and may flash black.
5. **Q07/Q10/Q14/Q26/Q27 device acceptance:** verify shared spinner geometry/opacity, card text and accessibility, every remote focus/OK/Down/Up/Back path, status changes, selector focus restored after cancellation, old async callback isolation, and Android audio-only versus video first-frame gating. Newly committed source policy is NOT TV-certified.
6. **Integration gates:** full source regression scripts, native/NDK/Media3 compilation, debug APK and actual TV smoke testing remain unrun and require separate authorization. All post-#134 unbuilt refinements must survive the eventual integrated build; no previously completed P0/P1/P2-A/B source was intentionally rewritten.

**End of P2-C assignment. P2 still PARTIAL; DO NOT start P3.** No GitHub Actions dispatched, no APK built, no workflow trigger modified. Next session must re-verify actual branch HEAD and preserve this entire P2-C sequence along with the five authoritative documents.


## P2 final source acceptance review — 2026-11-23 (NOT ACCEPTED; no P3)

**Assigned boundary:** Source-only final audit of P2, P2-A, P2-B, P2-C. No P3 feature implementation, APK, GitHub Actions, workflow triggers or player-source modifications authorized or undertaken. **Initial verified branch HEAD:** \`a909e4a9e729fcb23852b1196ad0cbbbc86db7b5\` (P2-C documentation checkpoint). Review performed against actual committed \`feature/official-stream-selector\` GitHub source, all five authority documents and every P2 checkpoint. This update is documentation-only.

**Decision: NO — P2 source implementation checkpoint is NOT accepted. P3 source implementation may NOT begin.** At least one specific non-deferred P2 source control-flow defect remains; Q18's separate owner-authorized deferral is NOT the reason for denial.

### Proven non-deferred P2 source gap: loading escape disappears before visual handoff (Q10/Q14/Q26/Q27)

- \`src/video_android.c::video_pronto()\` can return true after \`ANDROID_FURO_PRAZO_MS = 3000u\` of \`tocando\` following \`EV_PRONTO\` even with **no rendered first frame** (the fallback predates the newer explicitly verified audio-only state). It also returns true after a first frame while \`video_superficie_estavel()\` is still false during pending surface stabilization/recreation.
- \`src/player.c::player_carregando()\` is \`esperandoFonte || (comVideo && !video_pronto())\`. Thus it can become false although cinematic video handoff is unfinished. \`abrecine_passo()\` correctly retains \`cine.veu\` while \`imagem\` is false (actual rendered frame AND stable surface is required), but sets \`cine.viva=0\` once \`player_carregando()\` becomes false.
- \`src/player.c::cinemaEscapeDisponivel()\` uses \`nv_loading_change_ready(agora, cine.desde, cine.viva)\`: with \`cine.viva=0\`, the **15-second Change Stream action becomes unavailable while the cinematic still covers the video**. The loading-exclusive D-pad/OK-details/Down-focus branch in \`player_evento()\` is also gated by \`player_carregando()\`, so it is skipped. Back may take the normal OSD dismissal branch instead of the intended immediate loading-cancel path.
- This follows directly from the committed branch predicates, not a claim of reproduced TV failure. It can happen with an indefinitely missing frame or a first frame awaiting hardware surface stability. The 15/15 navigation policy unit test correctly covers elapsed-time arithmetic but **does not cover this integration-state counterexample**.
- **Required before P2 source acceptance:** retain the real first-frame/stable-surface gate, but keep the loading escape and Back/OK/Down navigation owned by the *unfinished cinematic handoff*, not solely the Media3 \`video_pronto()\`/three-second fallback or \`cine.viva\`. Add a host/source contract for (a) READY/tocando 3 seconds without frame, (b) frame observed but surface invalidated/not stable, (c) delayed 15-second Change Stream, (d) explicit Down→OK vs OK-details, (e) immediate Back/cancel and generation invalidation, (f) correct reset once the overlay truly ends. Do not start P3 until this source gap is fixed and reviewed.

### P2 requirement disposition (source check versus runtime)

| IDs | Source-review disposition | Evidence / remaining gate |
|---|---|---|
| Q01/Q07/Q17/Q23/Q34 | **Source implementation present, static checks passed** | Shared arc and reduced-motion geometry; four-stop full-height gradient/TopEnd crop; static title art and missing-art black; independent ~200ms selector entrance and frame-gated exit. Pixel accuracy, layering, focus, TV motion unverified. |
| Q02/Q12/Q13 | **Source implementation present, static checks passed** | Event-owned Media3 phases, subtitle fetch and P2P milestones; 180ms status transition, small rebuffer/reconnection indicator. Remote source types and visible TV output unverified. |
| Q05/Q06/Q15/Q21/Q28/Q29 | **Source implementation present with deliberate platform limitations** | Source-selected cinema, rendered-frame + stable-surface gate, audio mute until reveal/audio-only exception, small rebuffer arc, bounded source-owned SDR PixelCopy hold and seek recovery. HDR/DV surface replacement deliberately does not fabricate an SDR snapshot; device-specific black flashes, A/V timing, seek and ARC behavior unverified at P7/P8. |
| Q10/Q14/Q26/Q27 | **SOURCE INCOMPLETE — BLOCKING** | Post-READY/no-stable-frame state can retain the cinematic yet lose the loading-owner escape, diagnostics/Down→OK and direct Back cancellation, as proved above. |
| Q16 | **Source scopes inspected; runtime unverified** | Full cinema restricted from Live TV (compact tuning status); independent \`src/trailer.c\` / detail trailer paths retained. Actual trailer/live behavior needs device evidence. |
| Q18 | **EXPLICITLY DEFERRED, NOT IMPLEMENTED** | Real selected-file piece statistics (file size, verified-from-start prefix, verified total, guarded stream/file/generation) remain in P2-A, but they are *not* a current-playhead contiguous playable-buffer % or trustworthy denominator after seek/eviction. Owner primarily uses TorBox and authorizes this remaining meter as low-priority deferred. Continue indeterminate arc; NO synthetic %/readiness bar or false Q18 completion. |
| Q24/Q25 | **Source implementation present, static checks passed** | Per-resource Media3 network TransferListener for serial/HLS/DASH/revalidation, parallel-socket counter avoids double counting, JNI session gate, ~2.5s smoothing, B/s/KB/s/MB/s, negative/unknown omitted. **Do not certify throughput accuracy, cache/redirect edge cases or network types without P7/P8 runtime tests.** |

**Other protected source:** P0/P1 source ownership, exact selected-source identity/generation safeguards, P2-A verified-piece metrics, P2-B HDR/DV guards, P2-C shared spinner and all unbuilt post-#134 refinements remain on this branch. GitHub comparison from built #134 source \`bad57b2f9c9fecdfd8399372d576834e26406023\` to initial audit HEAD shows **ahead 121, behind 0**, without changing workflow files/triggers in this review. The **pre-existing Android VOD \`video_buffer_fim()\` returning 0** is still a separate P4/P7 honest buffer-rail issue, NOT a resolved P2 item.

### Verification actually executed by this review

- **PASS: 34/34 P2 core and 13/13 secondary/preservation live-branch structural assertions** via GitHub connector over committed \`player.c\`, \`abrecine.c\`, \`plrui.c\`, \`gfx.c/.h\`, \`app.c\`, \`streams.c\`, \`perfilsel.c\`, \`p2pmotor.c/.h\`, \`p2pmotor_motor.c\`, \`legenda.c/.h\`, \`video_android.c\`, Kotlin \`NvPlayer.kt\`, \`FrameHoldP2B.kt\`, and protected refinement files. These positive presence checks **do not override the proven control-flow defect**.
- **PASS: P2-C 15/15 C11 navigation checks**, host GCC with \`-std=c11 -O1 -Wall -Wextra -Werror\`; **PASS: P2-B 17/17 isolated Kotlin frame policy checks** with host \`kotlinc\` and \`java -jar\`. The four test/helper files were manually materialized into a temporary host working directory and their \`git hash-object\` values matched the exact branch blob SHAs: \`src/loading_nav_p2c.h\` \`7a33a757...\`, \`tests/playback_p2c_nav.c\` \`06205e9f...\`, \`FrameHoldP2B.kt\` \`dfd75747...\`, \`tests/FrameHoldP2BTest.kt\` \`c3a3020a...\`. **No authenticated full checkout** was available (host GitHub DNS fails), so this is NOT execution of the full checked-out repository nor Android compilation.
- **NOT RUN:** checked-in Python regression scripts in a complete checkout, P1/P2/P2-A full native integration tests, \`tools/android/preflight-c.sh\`, full NDK C/SDL and Kotlin/Media3/Gradle compilation. **Compiling/validating Android/NDK/Media3 is deferred to P7.**
- **NOT RUN:** APK, GitHub Actions, physical Android TV, HDR/Dolby Vision, ARC audio quality/synchronization, actual HTTP/HLS/DASH/TorBox/embedded P2P networking, remote-navigation and loading visuals. **Device/integration checks remain P7/P8 and require appropriate authorization**. An honest source assertion is not Android TV compatibility.
- No source code or workflow YAML/trigger files changed in this review, and no APK/Actions workflow was started.

### Exact next action / gate

**NO P3.** A separately authorized *narrow P2 source-fix session* should resolve and regression-test the post-READY unfinished visual-handoff/escape bug above without touching Q18 readiness %, P2-A/B/C working code or unbuilt post-#134 refinements. Then repeat the source acceptance review. Only when **all other non-deferred P2 source requirements are satisfied** may the tracker record **“P2 source implementation checkpoint accepted, with Q18 explicitly deferred and Android/device validation pending.”** P7 compilation and P7/P8 TV tests are independent future gates; P8 APK/Actions remain blocked without separate build authorization.
