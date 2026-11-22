# Nuvio Legacy — Q01–Q50 Implementation Progress

**Last updated:** 2026-11-22
**Status:** P0 COMPLETE; P1 SOURCE IMPLEMENTATION COMPLETE; **P2 IN PROGRESS — COMMITTED SOURCE CHECKPOINT, NOT COMPLETE**. P2 host timing and source contracts passed; full native/Kotlin compilation and TV QA unavailable. NO APK/ACTIONS RUN AUTHORIZED.
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
| P2 | Cinematic/arc/status/speed | **IN PROGRESS — 4 scoped source checkpoints, acceptance gaps below** | 22 exact P2 source/test commits in checkpoint below; current HEAD tracker commit | GCC+Clang mirrored header/abrecine timing PASS; 30/30 source checks PASS; 7/7 #if balances PASS; **full Android/TV tests NOT RUN** |
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
