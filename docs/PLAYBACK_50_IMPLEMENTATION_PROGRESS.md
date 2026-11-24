# Nuvio Legacy — Q01–Q50 Implementation Progress

**Last updated:** 2026-11-24
**Status:** P0–P5 source accepted; **P6 PARTIAL / NOT SOURCE ACCEPTED**. Q18 owner-deferred. P7/P8 NOT AUTHORIZED. No APK/full native link; physical TV/ARC gate reserved for P7/P8.
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
| P2 | Cinematic/arc/status/speed | **SOURCE ACCEPTED (P2-D final review); Q18 explicitly deferred, runtime gates pending** | Original P2, P2-A/B/C plus 15 P2-D source/test commits; full register below | P2-D 41/41 host C11 GCC + Clang ASan/UBSan; P2-C 15/15; P2-B Kotlin 17/17; live source checks; **no NDK/Media3 build or TV tests** |
| P3 | Selector/reload/recovery | SOURCE ACCEPTED | 17 source/test commits through `03acb8861d583ef7fec6c10d65673399bd3e7343` | Final review below, hardware pending |
| P4 | Buffer/network/cache/seek | **SOURCE ACCEPTED — Q18 explicitly deferred; P7/P8 full integration/device gates** | P4-D/A/B, P4-C and test-only CI commits; exact SHAs below | Actual API35/Media3 Kotlin compile PASS; 18/18 P4-C contracts; 36 proof checks; disk integrity/session JVM PASS; **10/10 Robolectric real Media3 cache adapter regressions**; native VOD host PASS; no APK or TV verification |
| P5 | Fidelity/HDR/surface/audio route | SOURCE ACCEPTED — P5-A/B/C/D | See final P5 acceptance report | Exact CI #45344027529 SUCCESS; full link and physical verification pending |
| P6 | Background/credits/post-play | PARTIAL — NOT SOURCE ACCEPTED | Review source/test/CI input `79ca121a541c5d8d2f348cd3e0f126ecef0e698d` | Exact local watched 35/35 PASS + UBSan PASS; latest test-only CI and identified source gates pending |
| P7 | Integration/regression, post-#134 | NOT STARTED | — | — |
| P8 | One cached APK + device tests | BLOCKED — NEEDS BUILD AUTHORIZATION | — | — |

**Q01–Q50 coverage:** P1's ten source-decision IDs were checkpointed; P2's assigned scope has now passed its **source-only acceptance review**, with Q18 current-playhead embedded-P2P readiness percentage explicitly deferred. This is **not** 50/50 implementation, full Android compilation, APK integration, or TV verification. P3 passed its source checkpoint. See P4 below. P5 source accepted; P6 partial; P7/P8 not authorized. “Committed” ≠ “compiled” ≠ “APK built” ≠ “device tested”.

## Change log

- **2026-11-22 / planning:** Owner selected Option A: GitHub checkpoints and fresh chats, one bounded work package per chat. Approved all 50 design choices, with Q50 Option A. Requested creation of canonical GitHub spec, tracker, package boundaries and preservation rules. Explicitly prohibited implementation and APK workflow during planning.
- **2026-11-22 / documentation:** Created master decision register, bounded work-package plan and this progress tracker. No feature code, CI trigger or APK run requested.

## Immediate next action

Verify exact P6 review CI and resolve only the outstanding source gates in the latest dedicated PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md review. P7 and APK are not authorized.

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


## P2 final source acceptance review — 2026-11-23 (HISTORICAL FAILURE; superseded by P2-D review below)

**Assigned boundary:** Source-only final audit of P2, P2-A, P2-B, P2-C. No P3 feature implementation, APK, GitHub Actions, workflow triggers or player-source modifications authorized or undertaken. **Initial verified branch HEAD:** `a909e4a9e729fcb23852b1196ad0cbbbc86db7b5` (P2-C documentation checkpoint). Review performed against actual committed `feature/official-stream-selector` GitHub source, all five authority documents and every P2 checkpoint. This update is documentation-only.

**Decision: NO — P2 source implementation checkpoint is NOT accepted. P3 source implementation may NOT begin.** At least one specific non-deferred P2 source control-flow defect remains; Q18's separate owner-authorized deferral is NOT the reason for denial.

### Proven non-deferred P2 source gap: loading escape disappears before visual handoff (Q10/Q14/Q26/Q27)

- `src/video_android.c::video_pronto()` can return true after `ANDROID_FURO_PRAZO_MS = 3000u` of `tocando` following `EV_PRONTO` even with **no rendered first frame** (the fallback predates the newer explicitly verified audio-only state). It also returns true after a first frame while `video_superficie_estavel()` is still false during pending surface stabilization/recreation.
- `src/player.c::player_carregando()` is `esperandoFonte || (comVideo && !video_pronto())`. Thus it can become false although cinematic video handoff is unfinished. `abrecine_passo()` correctly retains `cine.veu` while `imagem` is false (actual rendered frame AND stable surface is required), but sets `cine.viva=0` once `player_carregando()` becomes false.
- `src/player.c::cinemaEscapeDisponivel()` uses `nv_loading_change_ready(agora, cine.desde, cine.viva)`: with `cine.viva=0`, the **15-second Change Stream action becomes unavailable while the cinematic still covers the video**. The loading-exclusive D-pad/OK-details/Down-focus branch in `player_evento()` is also gated by `player_carregando()`, so it is skipped. Back may take the normal OSD dismissal branch instead of the intended immediate loading-cancel path.
- This follows directly from the committed branch predicates, not a claim of reproduced TV failure. It can happen with an indefinitely missing frame or a first frame awaiting hardware surface stability. The 15/15 navigation policy unit test correctly covers elapsed-time arithmetic but **does not cover this integration-state counterexample**.
- **Required before P2 source acceptance:** retain the real first-frame/stable-surface gate, but keep the loading escape and Back/OK/Down navigation owned by the *unfinished cinematic handoff*, not solely the Media3 `video_pronto()`/three-second fallback or `cine.viva`. Add a host/source contract for (a) READY/tocando 3 seconds without frame, (b) frame observed but surface invalidated/not stable, (c) delayed 15-second Change Stream, (d) explicit Down→OK vs OK-details, (e) immediate Back/cancel and generation invalidation, (f) correct reset once the overlay truly ends. Do not start P3 until this source gap is fixed and reviewed.

### P2 requirement disposition (source check versus runtime)

| IDs | Source-review disposition | Evidence / remaining gate |
|---|---|---|
| Q01/Q07/Q17/Q23/Q34 | **Source implementation present, static checks passed** | Shared arc and reduced-motion geometry; four-stop full-height gradient/TopEnd crop; static title art and missing-art black; independent ~200ms selector entrance and frame-gated exit. Pixel accuracy, layering, focus, TV motion unverified. |
| Q02/Q12/Q13 | **Source implementation present, static checks passed** | Event-owned Media3 phases, subtitle fetch and P2P milestones; 180ms status transition, small rebuffer/reconnection indicator. Remote source types and visible TV output unverified. |
| Q05/Q06/Q15/Q21/Q28/Q29 | **Source implementation present with deliberate platform limitations** | Source-selected cinema, rendered-frame + stable-surface gate, audio mute until reveal/audio-only exception, small rebuffer arc, bounded source-owned SDR PixelCopy hold and seek recovery. HDR/DV surface replacement deliberately does not fabricate an SDR snapshot; device-specific black flashes, A/V timing, seek and ARC behavior unverified at P7/P8. |
| Q10/Q14/Q26/Q27 | **SOURCE INCOMPLETE — BLOCKING** | Post-READY/no-stable-frame state can retain the cinematic yet lose the loading-owner escape, diagnostics/Down→OK and direct Back cancellation, as proved above. |
| Q16 | **Source scopes inspected; runtime unverified** | Full cinema restricted from Live TV (compact tuning status); independent `src/trailer.c` / detail trailer paths retained. Actual trailer/live behavior needs device evidence. |
| Q18 | **EXPLICITLY DEFERRED, NOT IMPLEMENTED** | Real selected-file piece statistics (file size, verified-from-start prefix, verified total, guarded stream/file/generation) remain in P2-A, but they are *not* a current-playhead contiguous playable-buffer % or trustworthy denominator after seek/eviction. Owner primarily uses TorBox and authorizes this remaining meter as low-priority deferred. Continue indeterminate arc; NO synthetic %/readiness bar or false Q18 completion. |
| Q24/Q25 | **Source implementation present, static checks passed** | Per-resource Media3 network TransferListener for serial/HLS/DASH/revalidation, parallel-socket counter avoids double counting, JNI session gate, ~2.5s smoothing, B/s/KB/s/MB/s, negative/unknown omitted. **Do not certify throughput accuracy, cache/redirect edge cases or network types without P7/P8 runtime tests.** |

**Other protected source:** P0/P1 source ownership, exact selected-source identity/generation safeguards, P2-A verified-piece metrics, P2-B HDR/DV guards, P2-C shared spinner and all unbuilt post-#134 refinements remain on this branch. GitHub comparison from built #134 source `bad57b2f9c9fecdfd8399372d576834e26406023` to initial audit HEAD shows **ahead 121, behind 0**, without changing workflow files/triggers in this review. The **pre-existing Android VOD `video_buffer_fim()` returning 0** is still a separate P4/P7 honest buffer-rail issue, NOT a resolved P2 item.

### Verification actually executed by this review

- **PASS: 34/34 P2 core and 13/13 secondary/preservation live-branch structural assertions** via GitHub connector over committed `player.c`, `abrecine.c`, `plrui.c`, `gfx.c/.h`, `app.c`, `streams.c`, `perfilsel.c`, `p2pmotor.c/.h`, `p2pmotor_motor.c`, `legenda.c/.h`, `video_android.c`, Kotlin `NvPlayer.kt`, `FrameHoldP2B.kt`, and protected refinement files. These positive presence checks **do not override the proven control-flow defect**.
- **PASS: P2-C 15/15 C11 navigation checks**, host GCC with `-std=c11 -O1 -Wall -Wextra -Werror`; **PASS: P2-B 17/17 isolated Kotlin frame policy checks** with host `kotlinc` and `java -jar`. The four test/helper files were manually materialized into a temporary host working directory and their `git hash-object` values matched the exact branch blob SHAs: `src/loading_nav_p2c.h` `7a33a757...`, `tests/playback_p2c_nav.c` `06205e9f...`, `FrameHoldP2B.kt` `dfd75747...`, `tests/FrameHoldP2BTest.kt` `c3a3020a...`. **No authenticated full checkout** was available (host GitHub DNS fails), so this is NOT execution of the full checked-out repository nor Android compilation.
- **NOT RUN:** checked-in Python regression scripts in a complete checkout, P1/P2/P2-A full native integration tests, `tools/android/preflight-c.sh`, full NDK C/SDL and Kotlin/Media3/Gradle compilation. **Compiling/validating Android/NDK/Media3 is deferred to P7.**
- **NOT RUN:** APK, GitHub Actions, physical Android TV, HDR/Dolby Vision, ARC audio quality/synchronization, actual HTTP/HLS/DASH/TorBox/embedded P2P networking, remote-navigation and loading visuals. **Device/integration checks remain P7/P8 and require appropriate authorization**. An honest source assertion is not Android TV compatibility.
- No source code or workflow YAML/trigger files changed in this review, and no APK/Actions workflow was started.

### Exact next action / gate

**NO P3.** A separately authorized *narrow P2 source-fix session* should resolve and regression-test the post-READY unfinished visual-handoff/escape bug above without touching Q18 readiness %, P2-A/B/C working code or unbuilt post-#134 refinements. Then repeat the source acceptance review. Only when **all other non-deferred P2 source requirements are satisfied** may the tracker record **“P2 source implementation checkpoint accepted, with Q18 explicitly deferred and Android/device validation pending.”** P7 compilation and P7/P8 TV tests are independent future gates; P8 APK/Actions remain blocked without separate build authorization.
## P2-D implementation + FINAL P2 source acceptance review — 2026-11-23 (SOURCE ACCEPTED)

**Initial HEAD:** `a28987f6c7863bf9ea67f7de98f636d6465f1f00` on `feature/official-stream-selector`. **Last verified source/test HEAD before this tracker commit:** `281f5d400c25d0d100ad7aeaf5641eddadae88a6`. The documentation commit that records this verdict follows it; always resolve the latest branch ref before future work. Reviewed all five authoritative documents, the entire prior failed source review, P2/P2-A/B/C checkpoints and latest committed P2-D source. **P3 was not implemented.**

**FINAL SOURCE VERDICT: P2 SOURCE IMPLEMENTATION ACCEPTED — Q18 explicitly deferred; Android/NDK/Media3 compilation and device validation pending P7/P8. P3 source work authorized.**

### Confirmed P2-D correction (Q10/Q14/Q26/Q27)

The historical source failure was genuine: Android's `video_pronto()` returned READY after an elapsed three-second fallback even without a first video frame, or before a rendered frame had a stable hardware surface. Legacy then stopped owning loading navigation while the cinematic veil remained. The fix is a real **separation of media readiness, current-SurfaceView first-frame/stability, backend startup, and incomplete cinematic handoff**:

1. `src/video_android.c`: removes the three-second false-positive READY shortcut; backend media ready now requires active + READY + first rendered frame or positively identified audio-only. Adds current-surface `quadroSuperficie`, invalidated on a SurfaceView recreation and set again by `EV_PRIMEIRO_QUADRO`; retains historical session first-frame status for ongoing playback, and preserves strict source-generation event guards.
2. `src/loading_handoff_p2d.h`: shared pure C predicates distinguish Media3 readiness, current stable frame/confirmed audio-only, and loading input ownership; none infer pixels from an elapsed timer.
3. `src/abrecine.c/.h`: `abrecine_navegacao()` owns loading navigation while opening OR while the veil has not fully disappeared, including the 200ms exit. An unstable surface during the fade resets the fade to full coverage and waits for a real stable current-surface frame; reduced-motion still skips only the animation, not the readiness requirement.
4. `src/player.c`: `player_carregando()` retains navigation through the unfinished visual handoff, while `abrecine_passo()` receives separate backend-opening state to avoid a self-perpetuating fade. The 15-second Change Stream predicate retains source-clock ownership while the cinematic is actually pending. It never auto-focuses: Down focuses; only focused OK selects; direct OK opens diagnostics. Loading Back remains immediate, with source-generation/P2P/pending-work invalidation in the existing `app.c` handler. Focus resets after full handoff, explicit new source, error or exit.
5. Genuine audio-only does not wait for a video frame; ordinary playback/rebuffer/same-source retained frame and Live TV's compact tuning continue on their existing distinct paths; HDR/DV never use an inaccurate SDR snapshot.

**P2-D chronological source/test commits (15; exact SHAs):**
- `8f7b76fdae0db029c6d490fd0cf6729e07124670` — shared readiness/ownership predicates.
- `ffc69454b3ac07070c617403b9309fbe769d7ddb` — cinematic navigation API declaration.
- `97ce9bee0b4449f89c31375f02183597057ebeee` — veil lifetime and surface-instability fade restart.
- `54c76605bdf81165fe89c2c2b8b4a1c0c9f53906` — remove fake Android READY and distinguish current-surface frame.
- `554e1630b4b282a31bb905debd88ae4a86dacfcc` — player input/visual ownership and reset.
- `af71897a93a18e2a3449749bba9cbb2f2f33e7ff` — host-only SDL stub.
- `d83bf49c9ec0182b668b49277034627d7f915688` — actual cinematic C11 regression harness.
- `ccc8f1bb54dad56141a6d95983828a3a7246288f` — host C11 test runner.
- `9724a877871bf1dc43b7ef2b388664d8a583590a` — adjust P2-C source contract for corrected ownership.
- `140a82d645145d2c2acf145d0d01f1c27786c2ca` — P2-D cross-module source contract.
- `32c02eafb9762e2cfc2b7fb505d08d5de55ebda6` — source-contract drawing scope.
- `0b2b450bd73857423e1f17003d13df58cf0f10f2` — source-contract Back/arc scope.
- `1b7f5f7e4ccaa871f6bdf880eb6c0a80a1b3fe55` — disambiguate Live TV numeric-cancel from player Back.
- `c00d3894f0e29e40602e0eb8804826ea55dd7f2a` — newline matching in source test.
- `281f5d400c25d0d100ad7aeaf5641eddadae88a6` — print actual newlines in C test results.

### Focused verification (actual evidence and boundaries)

- **PASS — 41/41 P2-D real cinematic C11 checks**, GCC `-std=c11 -O1 -g -Wall -Wextra -Werror` and Clang `-fsanitize=address,undefined -fno-omit-frame-pointer`, both executed. This used manually materialized **GitHub-blob-hash-verified** `src/abrecine.c`, `abrecine.h`, `loading_nav_p2c.h`, `loading_handoff_p2d.h` and `tests/playback_p2d_handoff.c`. Includes READY/no frame after three seconds, frame/unstable surface, exact 15s delayed visibility, explicit Down→OK versus direct OK, partial handoff, surface instability mid-fade, exact 200ms completion, reduced motion, confirmed audio-only, no rebuffer takeover and Live TV exclusion. A **minimal local `anim.h` math stub** and committed host-only SDL types substitute for full UI/NDK. Not a full checked-out Android application build.
- **PASS — 15/15 P2-C timer/focus checks** under host GCC and Clang ASan+UBSan, using manually materialized hash-verified `tests/playback_p2c_nav.c` and committed policy header. Includes SDL tick-wrap boundaries.
- **PASS — 17/17 P2-B frame-hold policy checks** under host `kotlinc` and `java -jar` using hash-verified `FrameHoldP2B.kt` and its original test. Verifies SDR eligibility, out-of-buffer seeks and async snapshot generation ownership; does NOT compile Media3.
- **PASS — 35 P2-D cross-file source predicates reviewed on live GitHub content** (three early false source-check results were faulty contract scoping to Live TV's digit handler and a too-narrow drawing region; fixed, and inspected against the actual Back handler/arc). The checked-in `tests/playback_p2d_source_contract.py` was authored and inspected but **not executed in a complete checkout**. **PASS — 27/27** additional P2 semantic/source-preservation assertions across shared arc, no fake percentages, UI statuses, Media3 first-frame, audio-only, network, P2P, P1 cancellation, selector isolation and P2-B frame hold.
- **PASS — 13/13** preprocessor conditional nesting checks over inspected C/header sources. This is NOT native syntax compilation. GitHub compare from `a28987f` to pre-tracker `281f5d4`: **15 commits ahead, zero behind**, only `src/abrecine.c/.h`, `src/loading_handoff_p2d.h`, `src/player.c`, `src/video_android.c` and P2-C/P2-D regression files changed. No `.github/workflows/*` files, build trigger, master branch or post-#134 handoff were changed.
- **NOT RUN:** host regression shell/Python commands in a complete authenticated checkout; P1/P2/P2-A end-to-end C integration; `tools/android/preflight-c.sh`; Android NDK/SDL/Gradle/Media3/Kotlin integration compilation; APK; GitHub Actions; hardware Android TV D-pad/Back/first-frame/surface/HDR/Dolby Vision/HDMI ARC tests; real TorBox/HTTP/HLS/DASH/embedded P2P throughput behavior. These gates remain **P7 compilation/integration** and **P7/P8 device validation**. Static evidence is not a certification of Android TV compatibility. Source checks exercise code paths and contractual ownership, not the full Android event loop.

### FINAL P2 requirement source-disposition matrix (P2 + P2-A/B/C/D)

| Requirement IDs | Final source checkpoint | Verification still pending |
|---|---|---|
| Q01/Q07/Q17/Q23/Q34 | **SOURCE VERIFIED**: shared official-derived loader arc including reduced motion; saved cinema backdrop mode/four-stop gradient and static logo/fallback; status animation and visual handoff. | Pixel-perfect compositing, elapsed transitions and TV artwork/gradient checks P7/P8. |
| Q02/Q12/Q13 | **SOURCE VERIFIED**: generation-bound Media3 loading phases; event-based subtitle and P2P milestones when reported; distinct rebuffer/reconnect indicators. | Real subtitle fetch/stream types and actual display behavior P7/P8. |
| Q05/Q06/Q15/Q21/Q28/Q29 | **SOURCE VERIFIED subject to documented hardware limits**: source-selected first-frame + stable-surface transition, audio-only proof, audio sequencing, SDR bounded source-owned PixelCopy retained frame on seek/recovery, HDR/DV no fake SDR conversion. | Hardware buffering/seek/surface recreation, Dolby Vision/HDR, audio sync, ARC P7/P8; HDR/DV may still briefly flash black on some TVs. |
| Q10/Q14/Q26/Q27 | **P2-D SOURCE DEFECT FIXED AND SOURCE VERIFIED**: startup and veil retain loading ownership, 15s Change Stream, Down→OK vs OK diagnostics, Back cancellation/generation invalidation and complete focus reset through 200ms exit. | Real remote/navigation and stale asynchronous callback/device acceptance P7/P8. |
| Q16 | **SOURCE VERIFIED**: cinematic only VOD, independent trailers and compact Live TV path preserved. | Live channel retuning/trailer on real TV P7/P8. |
| Q18 | **OWNER-AUTHORIZED EXPLICIT DEFERRAL**: actual selected-file piece statistics survive; genuine current-playhead contiguous playable-buffer readiness denominator/percentage is **not implemented**. Truthfully indeterminate, never fabricate %; owner chiefly uses TorBox. **Does not block P3 source development.** | Separate future feature decision/engine evidence if reauthorized. |
| Q24/Q25 | **SOURCE VERIFIED**: real HTTP HLS/DASH/serial TransferListener rate with parallel counter exclusions, generation-safe JNI, smoothing and B/s, KB/s, MB/s unknown omission; genuine embedded P2P speed and peers. | Network/cache/redirect telemetry truth on Android/real endpoints P7/P8. |

### Remaining issues, scope and next authorized work

**No remaining non-deferred P2 source-implementation blocker identified in this final branch review.** Prior Q10/Q14/Q26/Q27 proven defect was fixed in committed source and covered by positive host/source regressions. This does **not** erase the explicitly deferred Q18 or certify device runtime. P0's Android VOD `video_buffer_fim()` rail deficiency remains assigned to **P4/P7**, not misreported as P2-complete. Existing P1 source identities, P2-A measured piece bytes, P2-B HDR-safe frame retention, P2-C global spinner/navigation, and the nine documented groups of unbuilt post-#134 refinements remain preserved. P7/P8 must check their full integrated runtime. Full native/Android compilation is deferred to P7, TV verification to P7/P8, and any APK/CI action still requires separate authorization.

**P3 source work authorized by this SOURCE checkpoint only; P3 NOT STARTED in this session.** **No GitHub Actions, APK, workflow trigger modification or master-branch change.**

## P3 — Dynamic Clock and Stream-Selection Robustness — 2026-11-23 (FINAL SOURCE ACCEPTANCE)

**Authorized starting HEAD:** `ce474e13eddd24c4aca62ab2f74b11f5280cedef` on `feature/official-stream-selector`. **Final reviewed source/test HEAD before this tracker update:** `03acb8861d583ef7fec6c10d65673399bd3e7343`. Read all five authoritative playback documents and P2-D's final source acceptance/P3 authorization before modification. Scope was exclusively **P3**, respecting Q18's owner-approved genuine embedded-P2P readiness-% deferral. No P4 source, full APK build, workflow dispatch, workflow trigger change or merge to master.

**FINAL VERDICT: P3 SOURCE IMPLEMENTATION ACCEPTED — Android compilation and device validation pending P7/P8. P4 source work may begin.**

### Source implementation and final requirement review

| P3 requirement | Final source verdict | Specific behavior and evidence |
|---|---|---|
| **Q08/Q09** selector progression and ownership | **SOURCE ACCEPTED** | Existing per-addon `src/addons.c::progDrenar` UI-thread arrival queue and `src/streams.c::stream_lista_acrescentar` incremental ordered merge remain intact. `pendId`, profile-session scope and addon-version cancellation continue to reject obsolete arrivals. Distinct cards maintain stable underlying identity independent of display sort order. |
| **Q11/Q31/Q32/Q33** reload, empty, navigation, focus | **SOURCE ACCEPTED** | New `src/stream_reload_p3.h` represents retained-display-only versus first-fresh versus finished results. Forced **same-target** reload retains old cards but revokes native, file-pick and Android-bridge OK actions by generation/phase; fresh responses clear old cards **before** becoming selectable. Failure/empty/worker-start failure and profile-addon cancellation retire stale cards. `No Streams Found` and Reload remain keyboard/remote accessible. Preserve active addon by key, search query by its original buffer, scroll/focus from previous choice, and independently restore preferred/playing items only on matched immutable release. Search and subsequent user navigation override automatic restoration. |
| **Q19/Q20/Q30** explicit source, exact Resume and next episode | **SOURCE ACCEPTED** | Existing P1 Android manual-choice generation, stop-old-source sequencing, audio/subtitle/zoom/time carryover and no automatic alternative VOD selection preserved. Existing saved exact-source `fontevolta` account/profile/episode TTL path, optional persistence, next-episode `proximopreparo` and explicit `player_pediu_proximo` user authorization unchanged. Reload adds no autoplay, and old retained source rows cannot feed the pending automatic decision. |
| **Q34/Q35** URL identity and TorBox HTTP | **SOURCE ACCEPTED** | P1 `nv_fonte_mesma_liberacao` compares addon provenance + hash/file or supported immutable fallback, **not** expiring signed URL text. Reload focus/active/preference use the same comparator, with guarded exact-URL fallback only for otherwise unprovable rows. TorBox selected HTTP URL and per-source headers continue through existing `abrirFonteManualDireta`; same-release renewal, link age, and failure return to manual selector were not rewritten. No extra freshness network probe introduced. |
| **Q26/Q27** cinematic handoff and loading navigation | **SOURCE ACCEPTED** | Existing separate **~200ms selector-to-cinema entry** `player.c::desenharCinema` and **first-frame-and-stable-surface gated ~200ms visual exit** `abrecine.c` preserved. P2-D loading-owner stays active until the veil disappears, with 15s Change Stream, Down-then-OK, direct OK diagnostics, Back cancellation, source-job invalidation and focus restoration; no timer-based fake readiness. |
| **Q18** genuine embedded-P2P playable-ready percentage | **OWNER-APPROVED EXPLICIT DEFERRAL** | Not implemented in P3. Verified piece-byte counters remain intact; no fabricated current-playhead percentage or TorBox readiness %. Owner's main path is TorBox HTTP. Deferred and not a source acceptance blocker. |

**Defects found and corrected in this P3 session:** (1) old stream cards could be cleared before every addon responded, losing focus; (2) visual retention needed an explicit unselectable phase, generation and Android bridge guard; (3) the phase could prematurely become selectable *before* old-card teardown; (4) cancelled searches with zero partial responses could strand retained cards forever; (5) restored focus or active selection by old array index could point at a different release after reordering. All five have bounded source fixes. The established Dynamic Clock layout, provider chips, search field, existing animations and native C/SDL plus Android Media3 stack were retained. No P0/P1/P2-A/B/C/D source was removed or bypassed.

### Chronological P3 source/test commits (17 exact SHAs before the tracker update)

- `1e2e5e18e1f0f87faae5c0d2a44af1517bb56c30` — pure C11 retained/fresh reload-phase policy.
- `96baab11398d5e680204737289baa59858c5683a` — host C11 stale generation/exact-source regression.
- `dda0e954bfc7bcd795fcde82005da70a8ae8804f` — staged native and Android selector generation guards.
- `a95e89a8753dd5c628ffbfb2cbedbc02d945d538` — provider-state declaration order.
- `faa0da2b8621844f3927be99febdd16411420e2e` — progressive fresh-batch publication and forced refresh.
- `26461ce762dbd94c0b6c97d600b2ae746d51bf89` — scoped reload lifecycle declarations.
- `d9e1eca6ea532aa8b24a845e02120b48093ba5ac` — immutable release focus, filters, empty state.
- `41aee5d73c9d7b6c21ae355fabd42f6774c8a092` — atomic teardown/selection lock ordering.
- `96ae43bf8a6f1be19ddc097bdc195b4d67908cd4` — prevent auto decisions from retained stale cards.
- `21798101eaebe6da0f04b990ee4dfca814c05161` — standalone host C11 test runner.
- `62cfabd000e85f4f1ab64bd9773ae5a562182820` — cross-module P3 source contract test.
- `4d243e2132eed6a01b69082604be3859d02da936` — correct next-episode test symbol.
- `32d56df0c2ad7ccd097565e3da968b81f2ce6329` — readable stale-card warning overlay.
- `51558fab2b1e871e06582915be4ebd8a4937adaf` — cancelled zero-result worker cleanup.
- `f6b1bfe02e7608205633f6aed46c3c8d4ab77fba` — preserve latest intentional D-pad/provider focus during reload.
- `d216acf2ba97d4a57ff120a778bd76110351cc5e` — restore preferred/current actual release by immutable provenance.
- `03acb8861d583ef7fec6c10d65673399bd3e7343` — expand cancellation, focus, active-release and independent fade assertions.

### Final verification evidence and non-claims

- **PASS: 17/17 host C11 assertions** in the committed `tests/playback_p3_reload.c` when compiled under **GCC** (`-std=c11 -O1 -g -Wall -Wextra -Werror`) and **Clang ASan+UBSan** (`-fsanitize=undefined,address -fno-omit-frame-pointer`). Actual GitHub blob IDs were **matched with `git hash-object` before running**: `src/stream_reload_p3.h` = `5cc0dc79f84971dad6e3fcf9f2ecebddb5c090dd`, `src/fonteid_p1.h` = `1d6a8dda8541a12f80274f5be890cb69d018ff62`, `tests/playback_p3_reload.c` = `3294521fc31317f703ef3c4e705030e18b92c30e`. Both binaries exited successfully, no sanitizer error. Covers retained/disallowed card picks, first-fresh generation, late/repeated/out-of-order first flags, rapid reload, terminal failure/empty state, exact addon/hash/file/size distinctions, renewal-tolerant source identity and different episode rejection.
- **PASS: 35/35 P3 source predicates** inspected/evaluated against the actual latest GitHub source through the connector (out-of-order append, pending-worker scope, reload/cache bypass, error/cancel, native/Android generation, provider/scroll/focus restoration, preserved Resume/next episode/TorBox/track/zoom, two cinematic fades and loading navigation). The corresponding `tests/playback_p3_source_contract.py` **is committed but was not executed as a Python command in a full checkout**. Source predicates are evidence of relevant control paths, **not equivalent to observing TV behavior**.
- **PASS: repository comparison** from authorized start to reviewed pre-tracker HEAD was **17 commits ahead / zero behind**, with precisely `src/addons.c`, `src/app.c`, `src/stream_reload_p3.h`, `src/streams.c`, `src/streams.h` and three new P3 test files changed. **No `.github/workflows/*` file modified**, no merge, no explicit Actions dispatch, no APK.
- **NOT RUN:** existing full `tests/fonte_ordem_addon.sh` or other SDL/integration suites; complete checked-out P3 Python test; Android NDK/SDL source compilation; Gradle/Kotlin/Media3 build; Android TV or hardware remote navigation; actual TorBox HTTP, addon timeout/order, HLS/DASH, HDR/DV, subtitle and ARC behavior. A GitHub-authenticated full checkout/Android SDK/device was unavailable in this execution environment. Native/Android compilation remains **P7**, integration and actual-device evidence **P7/P8**. Previously reported P0/P1/P2 checkpoints are preserved source baselines, not freshly rerun full suites.

**Final source-acceptance ruling:** **No outstanding non-deferred P3 source blocker identified in the reviewed committed changes.** P3 is accepted **at the source implementation checkpoint only**, not asserted to be an Android APK/TV runtime PASS. Q18 remains explicitly deferred; pre-existing P0 Android VOD buffered-range rail is still assigned to P4/P7 rather than silently claimed fixed. **P4 SOURCE WORK MAY BEGIN** only as a separate authorized package. No P4 work performed in this session.

## P4-D — Android VOD buffering rail and seek ownership (2026-11-23)

Started at `127ef3e41db6ae651fdad7fb37dcbf26e83347c8`. Took the independently testable P0 defect first. Source/test commit `847b950a6b2c90561cd4bc52587a2e6156faf919`. Q28/Q43: Media3 current-window bufferedPosition reaches the Android OSD, including decreases. A masked seek destination without measured data ahead, invalid VOD samples and old source or seek callbacks yield no rail. Preserve signed Live window coordinates and existing D-pad burst accumulation. Superseded queued seeks share the original player and SurfaceView, with P2-B SDR capture on the final forward out-of-buffer seek.

Ran `python3 tests/playback_p4_vod.py` against the production native callback, reset and getter under host GCC. Passed unknown duration, invalid/negative or past-duration samples, decrease, zero ahead, stale revisions/generations, Live, and stopped playback. P1 identity, P2 timing/piece/frame policy, P2-C navigation, P2-D handoff, P3 reload host runners passed. P1/P2-B/P2-C/P2-D/P3 Python source contracts and both post-134 OSD/panel checks passed. The older P2 source script still expects the pre-P2-C inline 15s expression; the current P2-C/D contracts and compiled timer tests pass. No full Android/Media3 or device run. P4-A/B/C and final integration review outstanding. P5 remains gated.

## P4-A — configurable RAM profiles (2026-11-23)

Source/test commit `d23646013ac72877977b636537a0da69dff11098`, following P4-D tracker `451315507a7dfb270b4b2c3bb4399b40dde4e3f7`. Reference: NuvioTV dev `a740755e0f5a43874b3fee1ac808fdfc4c3884e7`, PlaybackBufferNetworkSettings.kt and BitrateAwareLoadControl.kt. Device-local settings expose independent min/max ahead, startup, rebuffer, back buffer, keyframe retention, time priority and RAM targets. All Streams inherits the saved RAM preset. Prepared and cold players snapshot the profile before dispatch. Adoption checks the snapshot. No recurring rate sampling or decoder rebuild for classification.

The accepted bitrate evidence is an explicitly unit-labelled MediaInfo General "Overall bit rate" field on the exact selected source description. Requests without that declaration use All Streams. The existing StreamFit estimate may contain size/duration derivations and is excluded from classification. The cutoff is decimal Mbps, initially 30, strictly exclusive. GCC C11 tests/playback_p4_profiles.c passed missing/invalid/conflicting metadata, exact threshold and edited cutoffs. Host kotlinc 2.2.21 compiled RamBufferProfile.kt and tests/RamBufferProfileTest.kt, JVM replay passed profile independence and legal duration ordering. Host GCC syntax-checked ajustes.c, player.c and video_android.c under NV_ANDROID using SDL 2.30.9, Khronos GL headers, desktop JNI and two SDL Android function declarations. P4-D callback test reran and passed. These are source and host checks. Full Media3/NDK integration, TV memory pressure and TorBox bitrate availability remain open. Continue P4-B/C before P4-E.

## P4-B — HTTP ranges and telemetry (2026-11-23)

Source/test commit `72e7133ade2402a65d9e29c73584db2bc38e4ad0`, after P4-A tracker `06311b6d6dba70e1a34ae8bc7237d2cc24f4b842`. Added device-wide HTTP enable/count/chunk controls, per-player physical byte counters, heap-bounded prefetched chunks and strict range/identity checks. Parallel assembly requires a strong ETag and exact offsets, lengths and total. Unverified or unreliable range service uses a single connection; a 200 at a seek consumes the exact prefix and respects the requested window. Changed validators and expired URLs cannot silently supply the remainder of a previous chunk. Preserves source headers, redirect renewal, cancellation, existing native 1 s/2.5 s speed measurement, and the dedicated limited-connection path.

Ran the actual downloader and its 24 JUnit tests on a host JVM against Media3 1.8.0 with Uri, Log and SystemClock host substitutes. All passed, including partial-read 200 responses, wrong-offset 206, mid-download fallback and byte-for-byte order, truncated body, rotating/expired redirects and cancellation. The changed player/downloader sources and dependencies also compiled with kotlinc against API 35 and actual Media3 1.8.0 jars using a minimal NuvioActivity focus-hook declaration. This is a targeted source compile. A separate whole-package attempt lacked Compose and resource-generated R. Native host syntax and the P2 source contract passed (updated superseded timer assertion to the actual P2-C/D helper and telemetry assertion to disjoint counters). No Gradle/NDK application build, APK, real CDN or device tests. P4-C and final review outstanding.

## P4-C — Disk VOD cache safety checkpoint — 2026-11-23 (PARTIAL; FINAL P4 SOURCE ACCEPTANCE FAILED)

**Start:** `3d1b05ccb5b46fa172e40ededcc1c6ae0aef7de1`. Owner authorized P4-C and final P4 review in Regular Chat after Work allowance exhaustion. Read all five authoritative playback documents and P4-D/A/B checkpoints, then verified starting HEAD through the branch-qualified GitHub compare/ref API. GitHub connector writes committed directly to the existing `feature/official-stream-selector` branch. No APK, Actions dispatch, workflow changes, merge or P5 implementation.

### Committed P4-C source/test work, chronological

1. `a597c0ba201b39131b904dc1febc47db6ed1cb99` — `CacheSessao.kt`: SHA-256 hashed per-session, full-request-URL cache key, with signed query kept out of Media3 index; rotated signed URLs cannot silently alias; automatic-size marker 2048 MB requests a conservative effective size (maximum 1 GiB; at most one-quarter of free disk beyond a 512 MiB reserve). A marker is **not** a 2 GiB allocation.
2. `096dd58721f7c6252c6897d010a91e89e681461b` — `CacheMidia.kt`: refuse reusing old cache by title/URL alone, guard free-space reserve during writes, ignore user-provided custom Media3 cache key to prevent two differently signed URLs aliasing. Media3 in-session seek hits and original upstream byte transfer instrumentation remain.
3. `7000c54c42250dd61d53ec51ecbaca8beb4ff783` — `NvPlayer.kt`: make new Media3 sessions create/release independent disk cache; do not retain unproven warm disk bytes; preserve distinct PixelCopy visual warm frame and playback stack. Accept Automatic request through current/prepared player settings.
4. `e42bf27d1e50994cdf451e3737a052420670761c` — `src/ajustes.c`: add fifth, device-local Automatic option; old Off/256/512/1024 indices and defaults unchanged.
5. `f37214cba361985e32990060198f3972377aec19` — `src/cacheboost.c`: map fifth option to Automatic request marker.
6. `31d5c9fc360c4539c76047252ccb6edc3f188555` — `src/cacheboost.h`: increase option count to five without altering cache-status enum/JNI values.
7. `ffe15227f4ab9df54d80eff8b926ecdf01336f27` — `tests/cacheboost.c`: assert Automatic mapping.
8. `a2111509f7f381ffd10ca354931e19aed360d492` — `tests/cacheboost_android.py`: adapt old plain-URL key assertion to hashed keys and signed-token isolation; exercise Automatic sizing.
9. `22e27ffba1869da1772384d1a1cabf4e74dfd644` — add standalone JVM regression `tests/CacheSessaoP4CTest.kt` for sizing, reserve, URL rotation, session disjointness, token-safe key, invalid session names.

**Test evidence executed in this Regular Chat:** one isolated Kotlin/JVM replay compiled and ran with `kotlinc` and `java -jar`, using a local reconstruction of the new pure `CacheSessao` code and the committed standalone regression; it printed `P4-C key isolation, rotation, disk sizing and reserve PASS`. **Limitation:** the local Kotlin source was reconstructed from GitHub rather than a byte-identical checkout, so this does NOT certify compilation of the actual new production `CacheSessao.kt` or `CacheMidia.kt`. Independently re-fetched the **actual branch GitHub source** and evaluated the relevant P4-C/P4-A/P4-B/P4-D presence/control-path predicates: SHA-256 key, session salt, storage reserve, LRU, conservative reuse, Automatic wiring, actual P4-B HTTP byte counter/range branches, Media3 seek revision and native VOD measured endpoint were present. These are **source inspections**, not executed Android networking or playback integration tests. The previously recorded P4-B 24 JUnit tests and targeted API35/Media3 compilation, P4-A host GCC/Kotlin checks and P4-D native VOD host test are **historical checks from their checkpoints, not rerun in this chat**. No Gradle application compile, full Java/Media3 cache test, NDK/SDL compile, real CDN, Android TV, subtitle/HDR/DV, ARC or A/V test executed now. GitHub remote contents API does not expose a checked-out working tree; no local repository `git status` claim is made.

### Final P4 source acceptance review (Q24–Q25, Q28, Q40–Q43)

| Area | Source disposition | Evidence / limitation |
| --- | --- | --- |
| Q24–Q25 true HTTP speed, smoothing and units | **Existing source checkpoint preserved** | P2/P4-B native 1 s refresh / 2.5 s window, real network-byte accounting, distinct Media3 serial/HLS/DASH vs parallel counters. Disk reads do not pass through a network transfer listener and no invented speed was added; real TorBox/cache-hit/redirect combinations still need runtime validation. |
| Q28 Android truthful VOD buffer & frame/seek protection | **Existing P4-D/P2-B source checkpoint preserved** | `NvPlayer.kt` emits `bufferedPosition`, `src/video_android.c` accepts only matching session+seek revision, uses `nv_vod_buffer_end`, clears endpoint on seek. P2-B last-frame policy unchanged. Actual hardware decoder/SDR-HDR-DV output unverified. |
| Q40 dual independent RAM profiles | **Existing P4-A source checkpoint preserved** | Strict less-than-30 Mbps only on reliable declared MediaInfo overall bit rate; unknown uses All Streams; profile snapshot is not dynamically reclassified. Kotlin player and native settings unchanged in P4-C. |
| Q41 parallel HTTP/ranges | **Existing P4-B source checkpoint preserved** | Exact 206/Content-Range/ETag and bounded chunks with serial fallback and real socket bytes; no P4-B source edited in P4-C. Historical 24 JUnit host PASS; no fresh integrated TorBox CDN test. |
| Q42 disk cache | **PARTIAL — non-deferred gaps remain** | New global Off/manual/Automatic settings, free-space guard, Media3 in-session range cache, LRU and strict no-cross-session fallback. **Cross-open stable-media identity, validated signed-URL rotation, byte representation integrity and corruption recovery are not implemented fully**. Prevented potentially wrong-byte reuse by disabling cross-open cache instead of claiming it works. |
| Q43 rapid D-pad seek coalescing | **Existing P4-D source checkpoint preserved** | Native seek-generation revision, Media3 `buscaSerial` stale-seek cancellation and source continuity retained; accumulated D-pad target remains in `src/player.c`. Real remote held-D-pad, out-of-buffer seek and output-sync checks remain P7/P8. |
| Q18 embedded-P2P playable readiness % | **OWNER-DEFERRED** | Existing explicit deferral; no invented percentages, no Q18 work. |

**Blocking source implementation gaps (P4-C/Q42):**
- Carry stable, immutable **account/profile + catalog title/episode + addon/provider + torrent hash/file index or equally strong release/file proof** across native `src/player.c` / `src/video_android.c` to Kotlin player; deny reuse when provenance is missing/ambiguous. Current title and signed URL do not suffice.
- For any reuse across URL rotation or player reopen, require **matching verified remote representation** (strong validator, exact total and byte ranges, and appropriate per-source authorization) and verify cached bytes before serving them; mismatched/expired/replaced resources must safely invalidate old ranges. Current implementation deliberately misses all rotated URLs and drops old sessions, rather than claiming verified warm reuse.
- Add explicit handling/testing of **silent cache-file corruption**, invalid/replaced span range, validator conflict, and eviction while reading. `CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR` is only an I/O fallback, not a cryptographic integrity check. Confirm that errors purge/ignore corrupt spans and cannot mix old/new representations.
- Prove safe player-generation and in-flight loader cancellation/release instead of relying only on the existing delayed `CacheMidia.liberar()` (1500 ms) cleanup; cover rapid switch, background, preloading and late callbacks.
- Execute actual checked-out `CacheMidia.kt` / `NvPlayer.kt` Media3/API35 compile plus targeted JVM/host corruption, validator, range, eviction, cancellation and telemetry tests. Distinguish those source tests from P7/P8 real TorBox playback and hardware validation.

**Final verdict: P4 SOURCE IMPLEMENTATION NOT ACCEPTED — P4 remains PARTIAL. P5 is NOT authorized.** The nine P4-C commits above are safe bounded source work, not Q42 completion. Preserve every P0–P3 protection and the previous P4-D/A/B test/source commits; do not restart Q18. No workflow trigger, APK, or master modification. **Next session:** verify new HEAD including this tracker commit, read five authoritative docs plus this checkpoint, finish P4-C source/host gaps first, rerun P4 tests, then re-review Q24–Q25/Q28/Q40–Q43. Only when all non-deferred P4 source criteria pass may tracker authorize P5. P7/P8 integration and Android TV/device tests still required afterward.

## P4-C — Q42 follow-up implementation and FINAL P4 source acceptance review — 2026-11-23 (PARTIAL; ACCEPTANCE FAILED)

**Authorized starting HEAD:** `2f375fe69462e4a0aa5c0eda35e189760119fe57`. Read the authoritative master specification, work packages, implementation progress (including the five failed P4-C/Q42 acceptance blockers), P0 baseline and all post-#134 refinements. Verified the feature branch before editing. GitHub connector commits modify only `feature/official-stream-selector`; no master merge, GitHub Actions dispatch, APK, P5 work or workflow-trigger edits. **Pre-tracker-review HEAD:** `7bc56fb7d5fcc74c4f6c589959466f500b63d7bc`, 37 commits ahead of the authorized starting HEAD, zero behind; 14 paths modified or added, none under `.github/workflows/`. The progress tracker commit itself follows this section.

### Exact P4-C source/test commits, chronological (37 before this tracker)

1. `203152e02716a15676aed1b8b00579a5c93ef6e3` — new fail-closed `CacheProvaP4C.kt`: account/profile/catalog/addon/torrent-file proof, header fingerprint and strong 206/ETag/total/encoding policy.
2. `189c0aeca5864189cf7ab04e504e541d4fcb68c4` — native source-identity bridge declaration (`src/cacheboost.h`).
3. `1ab1da0161ece4e9200b641bc43c639c8e0facbe` — non-Android source-identity hook (`src/cacheboost.c`).
4. `b84e3edec5791a75ef2ec02c4ae3c69125410b72` — selected account/profile/catalog/addon/torrent hash/file index/bytes from `src/player.c`.
5. `5480be075795a353674e7a452d372f3f4a878ebe` — JNI Kotlin cacheIdentity bridge in `src/video_android.c`.
6. `785b04d4902c35f46b97d34e21fd25fa921173e6` — nullable URI path policy correction.
7. `50432e59903ad0ba62f144aa7cd2ae80210380e4` — `NvPlayer.kt` native identity callback and header-scoped cache creation.
8. `d3176ee9c1ff0006f8b86f34457a273a16045482` — snapshot provenance before queued open, recovery, resume and prepared adoption.
9. `4af505905424a6e35da3ecb6dd27617a7ccde9e3` — per-session Media3 SHA key isolation includes release/header scope.
10. `ad39c04dddf3e31ecdd12cddbcc178a85612dd9a` — scoped `CacheMidia.kt` creation, idempotent release, non-destructive cleanup.
11. `55ae7c8f0d14c4cc9da5c88e834fc0457940d254` — cache release follows player and prepared-source release callbacks.
12. `c31b82e8d6792a385a18237e72fa7df3133a3765` — `CacheSpanP4C.kt` bounded SHA-256 manifest for completed Media3 spans.
13. `396ef1c1512fddb680caf64e5186da07744fb848` — canonical-path treatment and registered-span lookup.
14. `c891b4cb9a1ee5e08d4d4a6a07f4fadbca36207a` — Media3 span registration, validation, removal and digest pruning.
15. `67f92097de1a4b1d8195744e4c478446bcfcc08d` — exact-offset HTTP fallback wrapper for cached read errors.
16. `e9ca6e3833270b034b52212c46a6f85d13a8c39a` — `tests/CacheProvaP4CTest.kt` identity/range/renewal JVM tests.
17. `07a678f801aa4b54145d93ddb2e71dd6e57643c5` — `tests/CacheSpanP4CTest.kt` real file hash, corruption, truncation, symlink and eviction tests.
18. `23c01406ac085894878dd7b1bc6febe19889e476` — never normalize corrupt bytes by re-registering the same cache span.
19. `351f332cdbd869b5c4b6867c517bc049117b4777` — re-registration regression.
20. `54a880521878581ed04d1549aaf42aa4eb582f56` — include both production P4-C classes in the existing API35/Media3 Kotlin compile script.
21. `429f457a1a1cf0f15fedc3fcc17061f3524fd1d0` — source-side bounded HTTP 206 validator parser.
22. `a0e67fb913f1cb8462b72fd4e0750af256be47c8` — validate remote representation on prospective disk hits.
23. `c389363c3784f227cf3db61d15333869f9e341b7` — verify 206 observed lengths/ranges in JVM tests.
24. `5c2eb02bdc3989467594f3cd0cb4faacb2048119` — one-slot, proven last-title cache held open; signed URL still quarantined.
25. `6e7854a240d907a87ccf81d87274f4537bcc8b5f` — reuse old cache key only after strong 206 remote continuity probe.
26. `85085071d1f693092ce0198ba6d3cc87ce4bfb6f` — reject reduced storage eligibility; validate actual redirected CDN endpoint.
27. `e0d5bae591dcc10824ff6e3385667b883ae36980` — player stop/renewal lifecycle and safe cache handoff.
28. `70fdff8418307ae9feb42f0d868751e3e37163ec` — exact source/header scope key regressions.
29. `efc5a7139a3dd102bc50c30128a402c4966f0789` — check changed HTTP representation on cache-hole reads before bytes reach decoder.
30. `fbcc9fcbea4fbabcdd3449035b5e8b588572382d` — reject 206 total length contradictory to selected-file metadata.
31. `3ec0bec87197f79dfa1c7de22cba57071353625b` — native per-file size carried into Kotlin validator.
32. `2192ee681d6bf61e15753ed15200fd7ac0480e86` — reject malformed Content-Length.
33. `6562a1edc9b87a89547afa10c1dcec7df54153b5` — no canceled-loader retry; HTTP fallback restricted to eligible cached-span failures.
34. `0cbcc6de9315f49b43d61956b1b9d16fe9b7129b` — wire `CargaPlayer.cancelada` into cache DataSource.
35. `1ccd3c92dcbb09d952d3e57d2cc0e0d53153a8d9` — invalidation epoch cancels stale pending warm retention across profile/account/cache-off changes.
36. `ea175037c79e44cd7b1872ecd71d15585d2c4486` — committed native/JNI/Media3 ownership source-contract suite (not executed directly from checkout).
37. `7bc56fb7d5fcc74c4f6c589959466f500b63d7bc` — malformed Content-Length JVM regression.

### Actual current-session tests, exact boundaries

- **PASS:** Host Kotlin 1.9.0 compiled the *byte-identical committed production* `CacheProvaP4C.kt` (Git blob `160bee4f0e61d49d659553c29e92ea2dc79ba578`) and `tests/CacheProvaP4CTest.kt` (Git blob `d309d5d68d23c04f231a45ca71f8dbab35b0900f`); `java -jar` ran **36 passed assertions**. Checked exact identity permutations, account/profile, title/episode/addon/file/hash, headers, signed-query rotation, strong/weak/changed/missing ETag, CDN scope, range and total, Content-Encoding, malformed lengths.
- **PASS:** Same host compiled *byte-identical committed production* `CacheSpanP4C.kt` (Git blob `83bf0aa2f0c9778ed6fe2277132b6cc2fc585c20`) and `tests/CacheSpanP4CTest.kt` (Git blob `a7d8fe22f78f278dd71005b0636bb574e5d2f84d`); `java -jar` ran **16 passed assertions**, including real disk-byte replacement, truncation, out-of-root/symlink refusal, failed corruption normalization and eviction/manifest cleanup. Verified each local source SHA using `git hash-object` against the exact remote blobs before claiming source compilation.
- **PASS (source inspection only):** Independently re-fetched the production native/JNI/Kotlin P4-C files on the branch and evaluated **15 source wiring/contracts predicates**, covering JNI descriptor, native provenance, strong-validator policy, SHA disk hashes, alias guard, close-after-release, cancellation, physical network-byte scope and disk settings. This is source *inspection*, not executed Android code. `tests/playback_p4c_source_contract.py` is committed as a reproducible checkout test, **not run as a Python script** here.
- **PASS (repository comparison):** 37 source/test commits ahead / zero behind at pre-tracker HEAD; changed paths limited to existing Kotlin cache/player, native provenance hooks, and tests. No workflow-trigger edits, GitHub Actions run, APK, merge, P5 source work, media-quality/HDR/DV/audio-processing edit, or Dynamic Clock change.
- **HISTORICAL, not rerun now:** P4-D native buffered-endpoint/seek C tests, P4-A RAM profile C/JVM tests, P4-B 24 HTTP JUnit tests and its API35/Media3 targeted compile remain as recorded in their earlier checkpoints. P0–P3 and post-#134 refinements are preserved; their full tests were not rerun.
- **NOT EXECUTED:** Full/targeted new `CacheMidia.kt` + `NvPlayer.kt` production API35/Media3 compile (no authenticated repo checkout, Android SDK, Media3 jars or Gradle dependency cache available in this session); new JNI/native host GCC build; integrated fake-Media3/HTTP server tests for rotation/redirect, midstream change, LRU eviction, concurrent readers/writers, early cancellation, rapid source changes, warm slot expiry/reopen, next episode and network-byte callbacks; Android device/TorBox/HDR/DV/ARC/TV tests. The historical `tests/cacheboost_android.py` was updated to include new classes but cannot run without its Android/Media3 toolchain. Do not confuse local source-policy compilation with whole application compilation.

### FINAL P4 source acceptance disposition (Q24–Q25 / Q28 / Q40–Q43)

| Requirement | Final source disposition at this checkpoint |
| --- | --- |
| **Q24–Q25** physical HTTP throughput and truthful unit conversion | **Prior P4-B/P2 source checkpoint preserved**, including disjoint parallel/serial transport counters and 1 s/2.5 s measuring policy. P4-C wrapper forwards listeners and never uses disk-file transfer events as network bytes; no new runtime test. |
| **Q28** truthful Android VOD buffered endpoint, held frame and seeks | **Prior P4-D/P2-B source checkpoint preserved**, unchanged exact decoder/frame behavior; device verification remains P7/P8. |
| **Q40** two independent RAM profiles, strictly Below 30 Mbps | **Prior P4-A source checkpoint preserved**; disk cache does not touch bitrate/profile classification or quality. |
| **Q41** parallel HTTP, strict range correctness, bounded memory and serial fallback | **Prior P4-B source checkpoint preserved**; HTTP cache delegates upstream unchanged except for bounded new representation-validation request and safe bypass. Prior 24 tests historical. |
| **Q42** stable identity, TorBox renewal, span integrity, eviction, storage and lifecycle | **IMPLEMENTED IN COMMITTED SOURCE, BUT NOT FULLY SOURCE-ACCEPTED:** Native/JNI provenance, fail-closed identity, per-header key, strong same-origin/path 206/ETag/total/encoding validation before aliased hit, actual-redirect check, SHA-verified complete spans, safe fallback, 3h bounded single-cache retention, eviction/reserve, deferred safe release, cancel/epoch guards. Missing validators/ambiguous media, changed CDN or representation fall back to fresh HTTP. **Critical remaining acceptance evidence:** actual production Media3/API35 compilation and integrated cross-source cache/HTTP/lifecycle regressions, especially rapid switches, concurrent eviction, next-episode adoption and physical-byte count on cache hits; unknown actual-source compile or runtime defects must not be treated as passed. |
| **Q43** source ownership / rapid seeking | **Prior P4-D/P1/P3 source checkpoint preserved**; snapshot provenance, cancel guards and source generation fields added, but the combined new cache-loader paths remain integration-unverified. |
| **Q18** embedded P2P ready percentage | **EXPLICIT OWNER DEFERRAL**; no changes or synthetic value. |

**Outstanding non-deferred source acceptance gate:** The new cache `DataSource`, JNI bridge and warm retention remain **uncompiled together against actual API35/Media3/NDK production dependencies**, and essential Q42 release/rotation/concurrency/cancellation/telemetry regression behavior is not yet executed against production adapters. JVM proof/span policy checks and source predicates cannot establish byte-safe actual Media3 cache IO, true range-shifted telemetry, JNI linkage or close/reopen absence of races. Until those bounded tests demonstrate no defects, Q42 and final P4 source acceptance must remain gated. The safe implementation intentionally refuses reuse when file/hash/account/validator evidence is absent, rather than guessing identity. No Q18 or P4-A/B/D restart is warranted.

**FINAL VERDICT: P4 SOURCE ACCEPTANCE FAILED — P4 remains PARTIAL; P5 not authorized.**

**Precise resumable next action:** Verify branch HEAD including this tracker update, then run `tests/cacheboost_android.py` with Android 35/Media3 1.8.0/Kotlin dependencies and fix any *actual production* compilation errors. Run the committed `tests/playback_p4c_source_contract.py`, `tests/CacheProvaP4CTest.kt`, `tests/CacheSpanP4CTest.kt`, `tests/CacheSessaoP4CTest.kt`, focused native C regressions, and add/execute integrated Media3/HTTP range/revalidation/cache-hit/cancel/reopen/preload tests using a local deterministic range server. Inspect and fix any failed cases, then repeat the FINAL P4 review of all Q24–Q25/Q28/Q40–Q43 in the same subsequent bounded session. Until those tests establish correctness, do not authorize P5 or claim Android playback/NDK build. P7/P8 remain the separate real Android TV/TorBox/HDR/DV/ARC and complete integration gates.

## P4-C — integration-verification attempt and FINAL P4 source acceptance, 2026-11-23

**Verified starting and pre-write branch HEAD:** `4c2cad1c6f6205ab202661d4f4f9ab14fcd5c83a` (GitHub compare identical, zero new commits). **Scope:** verification only; no P5, APK, Actions dispatch, workflow-trigger edit, merge, or source change. The existing 37 P4-C follow-up source/test commits and P4-D/A/B source checkpoints remain untouched.

### Exact current-session evidence

1. **Source-only — PASS, 18/18 inspected predicates.** Fetched the real committed branch-qualified `src/player.c`, `src/video_android.c`, `src/cacheboost.h`, `NvPlayer.kt`, `CacheMidia.kt`, `CacheProvaP4C.kt`, `CacheSpanP4C.kt`, `CacheSessao.kt`, and `ParaleloDataSource.kt` through the connected GitHub API. Directly evaluated all 18 predicate expressions corresponding to `tests/playback_p4c_source_contract.py` against those **actual fetched file contents**. 18 passed: native identity and JNI descriptor, immutable selected-source proof, signed URL/header separation, strong 206/range and redirect checks, guarded aliasing and revocation, SHA-256 span inspection and non-re-registration, LRU/space reserve, cancellation/error fallback, changed-representation fail-closed behavior, release and seek ownership, disjoint network byte instrumentation, Automatic size cap. **This was a connector-side source-predicate evaluation, NOT execution of the committed Python script, actual Kotlin compilation, JVM cache IO, Android runtime, or device testing.** Source predicate success cannot demonstrate concurrent byte safety.
2. **Production API 35 / Media3 1.8.0 compile — BLOCKED, NOT RUN.** Read the committed `tests/cacheboost_android.py` (actual source). Local runtime has OpenJDK 21 and host `kotlinc-jvm 1.9.0`, but **does not have** Android 35 `android.jar`, the test's pinned Gradle Kotlin 2.0.21 compiler/stdlib artifacts, Media3 1.8.0 datasource/exoplayer AARs (nor the complete compiler/Android dependency cache), or an authenticated checked-out repository. Direct `git ls-remote` failed because container DNS/network access to github.com is unavailable; the GitHub connector permits source reading/writing but does not mount the repo or its required compilation jars. These missing prerequisites prevent executing `tests/cacheboost_android.py` as written and prevent certifying the actual `CacheMidia.kt` / `NvPlayer.kt` / `ParaleloDataSource.kt` combination against the real Media3 and Android interfaces. No substitute stubs or reconstructed fixtures have been represented as a production compilation PASS.
3. **Integrated cache/HTTP/lifecycle regressions — BLOCKED, NOT RUN.** A runnable production Media3 `SimpleCache` / `CacheDataSource` / `CacheDataSink` test harness and checkout are unavailable. Therefore signed-URL rotation through the actual redirecting HTTP range server, disk span corruption/replacement/eviction during reads, interrupted writes, concurrent readers/writers, concurrent player release, rapid source switch, background/foreground, account/profile invalidation, next-episode preload/adoption, cache-hit versus real network transport bytes and stale-callback cancellation were **not executed**. Remote source review confirmed existence of guards but did not establish their integrated runtime effectiveness.
4. **Prior suites — HISTORICAL, NOT RERUN.** The earlier checkpoint's 36/36 production `CacheProvaP4C.kt` JVM assertions, 16/16 production `CacheSpanP4C.kt` disk assertions, `CacheSessaoP4CTest.kt` isolated replay, P4-B 24 Media3 HTTP JUnit tests and P4-A/P4-D native/JVM tests retain their original recorded evidence; none was re-executed in this verification attempt. The committed `tests/playback_p4c_source_contract.py`, the three Kotlin test files and the wider P0/P1/P2/P3 preservation regressions were **not** run as checked-out test programs. No bug fix is asserted without a demonstrated failing production test.
5. **Device-only deferrals — P7/P8:** Android TV renderer, physical decoder / first-frame timing / retained frame, HDR/DV, TorBox production CDN/network conditions, subtitle output, HDMI ARC audio and D-pad UI, full Gradle/NDK APK integration, and the final post-#134 unbuilt refinements must be verified at their separately authorized gates. Q18 embedded-P2P playable-readiness percentage remains explicitly owner-deferred and is **not** a P4 failure.

### FINAL P4 SOURCE ACCEPTANCE REVIEW (current evidence)

| ID | Disposition |
| --- | --- |
| Q24–Q25 | Historical P2/P4-B source implementation and transport-counter evidence preserved; cache-hit vs network probe/retry telemetry integration **not run**. |
| Q28 | Historical P4-D native VOD buffered endpoint/seek safety and P2-B frame policy preserved; no new tests here, device verification P7/P8. |
| Q40 | Historical independent RAM profile and strict reliable below-30-Mbps classification preserved; no new tests here. |
| Q41 | Historical bounded parallel HTTP, validator and serial fallback source/JUnit evidence preserved; cache integration with actual HTTP adapters **not run**. |
| **Q42** | **SOURCE ACCEPTANCE GATE OPEN:** provenance, remote representation validator, SHA-256, warm slot, cancellation and reserve guards are present in committed source (18/18 predicates). **Actual Android API35/Media3 1.8.0 production compilation and essential cache/HTTP/lifecycle integration regression behavior remain unverified.** |
| Q43 | Prior seek-coalescing/session safeguards preserved; source predicate presence passes, but new cache-loader cancellation integration **not run**. |
| Q18 | Owner-deferred embedded-P2P readiness; not a blocker. |

**FINAL VERDICT: P4 SOURCE ACCEPTANCE FAILED — P4 remains PARTIAL; P5 not authorized.** This is an **essential environment/evidence blocker**, not a newly demonstrated regression: no unsupported assertion of a source bug, compiler pass, runnable Media3 cache IO or Android TV success. Required unblock: an actual accessible checkout with Android SDK API 35, Media3 1.8.0 and compatible Kotlin toolchain; execute and fix `tests/cacheboost_android.py`; add/run deterministic production-adapter Media3/HTTP/cancellation/rotation/eviction/telemetry tests; rerun focused P4-A/B/D and P1–P3 source regressions; then repeat the final P4 source acceptance review **before** P5 authorization. Do not use GitHub Actions or build an APK without separate owner approval.


## P4-C dedicated test-only GitHub Actions verification — 2026-11-23 (TARGETED API35/MEDIA3 COMPILE PASSED; P4 STILL PARTIAL)

**Authorization and restrictions:** Owner explicitly authorized GitHub Actions for a test-only cache verification, with NO APK. The dedicated workflow .github/workflows/p4-cache-test-only.yml is scoped to the feature branch, has read-only contents permissions and triggers on its own file and dedicated helper only. No existing Android APK workflow paths were changed, and no APK, artifact upload, release, workflow dispatch to an APK job or master merge occurred.

**Successful run:** [P4 cache verification, run 45148742410](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45148742410), verified successful job 137604448087 at source HEAD ec159278d63b596d47aa0207a582a349c9bbe294. Actual authenticated checkout, JDK 17, Android API 35, Media3 1.8.0 AARs and Kotlin 2.0.21 compiler were used.

- **PASS**: python3 tests/playback_p4c_source_contract.py, all 18 P4-C production source predicates.
- **PASS**: python3 tests/playback_p4_vod.py, actual native Android VOD/Live buffer callback, seek-revision ownership and stale-source host regression.
- **PASS**: targeted production Kotlin compile via tests/cacheboost_android.py with explicit NUVIO_CACHE_COMPILE_ONLY=1, including the actual NvPlayer, CacheMidia, CacheSessao, CacheProvaP4C, CacheSpanP4C, ParaleloDataSource and other current support files, against Android API35/Media3 1.8.0. A minimal NuvioActivity stub was used because SDL Java was absent. NOT a complete Gradle application, native JNI link or APK build.
- **PASS**: tests/p4_cache_ci_policy.py compiled byte-for-byte checked-out production Kotlin classes and ran CacheProvaP4CTest (36 provenance/strong HTTP validator cases), CacheSpanP4CTest (real on-disk hash/corruption/truncation/eviction cases) and CacheSessaoP4CTest (signed URL/session isolation and storage sizing). All separate JVM programs exited successfully.
- **Actual source bug caught and fixed**: CacheMidia.kt SinkProtegido called estado from member methods but did not retain constructor parameter as a property. Commit 4719245d1afef5a31e2ea4bff06c9a2b9469d8e3 added private val; later production API35/Media3 Kotlin compile passed.
- **Test-harness changes**: c6093d56ce7aca74c8eaa77b4c2c48dc68f8ae94 added current player support Kotlin and Media3 HLS to the compilation list; a228b918aa88745fc8d8f89edeec9339e949e292 separated cache compile from unrelated audio replay; 064cd25ab3201554dfe0a2f9795817011d8d3590 corrected host Kotlin compiler JVM classpath. Other CI-only commits provisioned pinned dependencies. Earlier failed runs revealed missing harness JARs, missing compile sources and one genuine cache scope error.
- **Unrelated audio regression remains unverified**: the default historical cacheboost_android.py audio replay failed at Replay.kt line 165. The successful cache-only run intentionally omitted this independent GanhoAudioProcessor assertion and does not claim it passed. Investigate separately in audio/regression scope.

**Still unverified / blocking full Q42 source acceptance:** End-to-end actual production Media3 CacheDataSource + controlled HTTP range server tests for signed URL renewal, changed response across cached holes, concurrent readers/LRU eviction, canceled loaders and stale callbacks, warm reopen/expiry, next-episode preload and disjoint network byte counts. These were NOT executed by the green workflow. Also no full Android NDK/JNI link, actual TorBox/device/HDR/DV/ARC or APK execution; the hardware/integrated APK gates stay P7/P8.

**P4 final verdict remains PARTIAL / NOT SOURCE ACCEPTED for Q42 adapter-level integration. P5 NOT AUTHORIZED.** P4-A/B/D previously checkpointed. Q18 remains explicitly owner-deferred. Do not infer completion of remaining adapter tests from successful targeted Kotlin compilation.


## P4 FINAL SOURCE ACCEPTANCE — 2026-11-23 (SOURCE PASS; P5 AUTHORIZED)

**Baseline for this final CI work:** f06e12914ea77c57a527c4184648b1947dc17a41, feature/official-stream-selector. Owner authorized **test-only** GitHub Actions; no APK, release, master merge, existing debug-workflow trigger change, P5 implementation or hardware test was authorized or performed.

### Q42 production adapter implementation and verified regressions

The first actual production API35/Media3 compilation was already passing at successful test-only Actions run 45148742410; the last unresolved P4 source acceptance gate was integrated Media3 CacheDataSource/HTTP-range/cancellation behavior.

New isolated source-level integration harness: tests/p4-cache-integration/ (standalone Android library + Robolectric), compiling the **exact committed production CacheMidia.kt, CacheSessao.kt, CacheProvaP4C.kt and CacheSpanP4C.kt** copied by the test-only workflow. This exercises real Media3 SimpleCache, CacheDataSink and CacheDataSource adapters rather than fake policy classes. Tests use a deterministic in-memory HTTPS 206 origin to model TorBox-style signed URL rotation, and a real loopback TCP HTTP/1.1 206 range server for DefaultHttpDataSource. They never access external TorBox credentials.

**Confirmed new non-deferred P4-C source defect:** CI 45154230136 (four tests; three pass) demonstrated that CacheMidia.kt's Media3 DataSource wrapper did not recheck source cancellation in read(). A cancelled loader could return disk or upstream bytes. **Fixed** in commit 90be1327131155b575dfb7cfdb88a39b636a0f1b by checking cancellation before/after reads and after precise-offset fallback. Integration regression confirmed the fix.

**FINAL green run:** [P4 cache verification (tests only - no APK), GitHub Actions #45155757955](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45155757955), exact tested source HEAD **f1c16dbb4eef783dfbe73225f34b5eaf7c39102a**; job 137628130155 **SUCCESS**.

Direct log evidence from actual checkout and CI runner:
- **PASS** 18/18 committed P4-C source ownership/provenance/validator assertions in tests/playback_p4c_source_contract.py.
- **PASS** native P4-D Android VOD/Live buffered endpoint, seek revision and session replacement test in tests/playback_p4_vod.py.
- **PASS** actual checked-out production Kotlin cache/player compilation against Android API 35, Media3 **1.8.0**, Kotlin **2.0.21**, JDK 17 via tests/cacheboost_android.py with NUVIO_CACHE_COMPILE_ONLY=1. This targeted compile includes real cache/network/player support files; SDL activity is minimally stubbed where its generated Java sources are absent. This is **not** a full Android Gradle app build or native JNI linking.
- **PASS** committed real-production-source policy JVM checks: 36 strong provenance/HTTP validator assertions; SHA-256 span corruption/truncation/symlink/eviction and failed re-registration regression; signed URL/header/session disjointness, storage reserve and Automatic sizing.
- **PASS: 10/10 tests** from actual production Media3 cache adapter in P4Media3CacheTest under Robolectric. The ten executed scenarios are:
  1. Fresh HTTP read / persisted Media3 disk-cache read without full redownload.
  2. Signed-URL renewal with strong matching representation; changed ETag safely rejects stale content.
  3. Silent cache-span byte corruption is rejected before decoder delivery.
  4. Cancel before DataSource.open and after open before read rejects loader bytes.
  5. Cached first half plus uncached second-half HTTP hole keeps exact ranges.
  6. Changed remote representation after a partially cached file never mixes old/new bytes.
  7. Profile/account switch revokes retained session bytes.
  8. Real loopback HTTP 206 Content-Range through Media3 DefaultHttpDataSource returns precisely requested bytes.
  9. Concurrent SimpleCache eviction while a reader is open produces correct content without mixed bytes.
  10. Next-episode identity cannot adopt the previous title's retained cache.

**Additional committed test infrastructure/source work:** 6c694a5e6090839418d7bdb95d1e145b16baed63, b6d147b794e573617cd04a75a68d275b9b5b6f5e, 473e020962dfa21fec135fbe5a8f179b5ca408ac, df3362907beec284b035aaebe0956ecbd66bcc00, d31e5369b439ea17356b4ea2208484e496ffa036, 111a659a250094b9fe4ed74d00a8f4fcbb59f814, 347d250a6cc657abf2adaed9939c533cb036215f, 7b9dbbd6f494f32198e7532968daf3cb6869c091, f1c16dbb4eef783dfbe73225f34b5eaf7c39102a; exact chronological change register in git. Earlier failed CI attempts were test harness dependency 429/Java module scope issues or the confirmed cancelled-read bug; none is presented as a current PASS until the final green run.

### FINAL Q24/Q25/Q28/Q40–Q43 source acceptance table

| Requirement | Final source verdict and evidence |
| --- | --- |
| Q24–Q25: genuine throughput, smoothing, correct units | **SOURCE ACCEPTED:** historical P2/P4-B real physical network-byte transport source and host tests preserved; verified real Media3 cache hits avoid full redownload and cache wrapper preserves existing upstream transfer listeners. Live TorBox measurement remains P7/P8. |
| Q28: seek visuals and buffered VOD rail | **SOURCE ACCEPTED:** P4-D native callback/seek source checkpoint preserved and actual host VOD/Live/seek replacement test passed in final CI; P2-B last-frame ownership code preserved. Hardware first-frame, HDR/DV and D-pad tests remain P7/P8. |
| Q40: independent RAM profiles, reliable below 30 Mbps | **SOURCE ACCEPTED:** P4-A source/host checkpoint preserved, target Kotlin compiled; unknown bitrate stays All Streams and no classification thrash was introduced. |
| Q41: parallel HTTP validated range and fallback | **SOURCE ACCEPTED:** P4-B 24 recorded JUnit tests and targeted compile remain historical (not all rerun), P4-C uses original upstream chain and new loopback HTTP range test verified exact bytes. Real TorBox CDN behavior pending P7/P8. |
| Q42: disk VOD cache | **SOURCE ACCEPTED:** proven source identity, strict same-origin/path/strong ETag and exact length, SHA-256 completed disk-span checks, conservative signed-URL reuse, storage reserve/LRU, safe session lifecycles, exact range/changed-hole detection, tested cancel-after-open path, 10 real Media3 adapter regressions and production compile as above. Missing/weak proof fails closed rather than aliasing bytes. |
| Q43: fast seek, cached range and source continuity | **SOURCE ACCEPTED:** existing P4-D/P1/P3 seek coalescing/identity snapshots preserved, actual VOD seek-generation host test and P4-C real partial-cache range/cancellation regression passed. Remote-held D-pad/output A/V sync remain device gate. |
| Q18 | **Explicit owner deferral:** genuine embedded P2P current-playhead playable percentage. No fabricated readiness, not a P4 blocker. |

### Explicit remaining P7/P8 and adjacent work (NOT claims of P4 source incompleteness)

- No whole-app Gradle assemble, full NDK/C/JNI link, Android TV device, actual TorBox CDN/signed-link behavior, HDR/Dolby Vision/hardware decoder, HDMI ARC, physical remote long-held D-pad or post-#134 integrated device regressions executed here. **P7/P8 retain these independent release gates**, including broader concurrency, background/preload and network telemetry performance under real Android scheduling.
- The older unmodified cacheboost_android.py standalone **GanhoAudioProcessor** PCM replay had failed at Replay.kt line 165 in an earlier run; the successful P4 cache compilation used the explicit cache-only mode and did not claim that independent audio test passed. Investigate in P5 audio validation or P7 regression before release; no source defect asserted without analysis.
- P4-A/B/D historical host tests were not fully rerun in this final CI session. Their documented accepted source checkpoints remain; untouched source paths and targeted API35 compile were checked.
- This acceptance is **source implementation acceptance**, not end-to-end TV playback proof. Conservative cache misses when strong HTTPS representation/immutable identity proof is missing are intentional safety behavior.

**FINAL VERDICT: P4 SOURCE IMPLEMENTATION ACCEPTED — Q18 explicitly deferred; full Android integration and device verification pending P7/P8. P5 SOURCE WORK AUTHORIZED.**

Older P4-C and P4 final reviews marked FAILED or PARTIAL above are historical checkpoints **superseded by this final successful source-review checkpoint**. Continue with P5 only as separately authorized source work; do not trigger an APK build without separate owner consent.

## P5-A first bounded source checkpoint + P5 source gate review — 2026-11-23 (PARTIAL; NOT ACCEPTED)

**Repository/branch:** `nerdfxzxl/nuvio-native-legacy`, `feature/official-stream-selector`; verified starting HEAD `5d1b0afaa618d3b6ac3f0adca71d159ed3cabe29`. P4 source was accepted at this starting HEAD. P5 work below is *only the first audio-processor correction*; none of P5-A/B/C/D may be presented as complete.

**Full owner specification located:** `NUVIO_LEGACY_MASTER_PLAYBACK_SPEC_Q01-Q50_2026-11-22.md` (441-line approved expanded spec) in the owner's ChatGPT File Library. Its §2.6 and Phase 5 were read alongside the five canonical repository documents. The in-repo condensed spec remains authoritative for repository handoffs. No conflicting new owner decisions inferred.

### Committed P5-A source/test changes

1. `4454a3293540f7755e302ec9e320fe661e207872` — `android/app/src/main/java/space/nuvio/nativelegacy/GanhoAudioProcessor.kt`. Investigated the historical `tests/cacheboost_android.py` embedded `Replay.kt:165` PCM failure: the gain processor used `(y * 32767f).toInt()`, which truncates 16-bit gain 2x of sample 1000 to 1999, contrary to the correctly expected 2000. Its amplified high-level float PCM was also *hard clipped* to -1, failing the replay's soft-limit expectation. Reused existing production `GanhoMath.amostra(y, volume)` tanh soft limiter on gain >1, and 32768-scale rounding with `roundToInt` and safe 16-bit clamping. Preserved bit-exact no-DSP fast path and existing compressed passthrough behavior; no encoder/transcoder inserted.
2. `2c12d4d0d3e2ee2d00a5e14819b418e83458b405` — `tests/cacheboost_android.py`. Extended the existing real-processor JVM replay with an amplified positive/negative float PCM peak case; retained the original exact 16-bit gain, bypass and encoded AC3 rejection assertions. The replay test was **committed but not executed** on an Android/Media3 Kotlin classpath in this session.

**What was actually executed:** 14/14 checks in a connector-side JavaScript source-predicate/independent numeric-model verification, against the committed production source and replay source fetched from this branch. This checked gain paths, bypass, PCM16 rounding, historical assertions, AC3 guard and independent tanh/PCM arithmetic scenarios. It is **NOT** the actual Kotlin `Replay.kt` JVM test, not an Android API35/Media3 compilation, and does not verify audio on a TV. The host environment had `kotlinc` but no production Media3 dependency classpath; the standalone `tests/cacheboost_android.py` replay was not honestly runnable here. The prior P4 green cache-only CI also excluded this replay; there is still **no verified 2026-11-23 Kotlin replay PASS**. The source cause is supported by code inspection and numerical reproduction; runtime acceptance of the fix remains open.

### Actual source audit, remaining non-deferred gaps

- **P5-A (Q36) remains PARTIAL.** `src/audioescolha.c` has original-language and codec-based prioritization, but `src/video_android.c` builds candidates with `pode=1` after Kotlin filters merely renderer-supported tracks; no route-specific fidelity proof or complete output-format validation is established. `NvPlayer.kt` constructs Media3 sink/decoder with FFmpeg fallback and existing PCM processing but cannot guarantee compatible HDMI ARC delivery from the track label. Native `src/player.c` responds to unsupported audio by a short warning while silent video may continue, and its error card has two actions, not the required explicit **Audio Playback Error — Retry / Change Stream / Back**. Track metadata remains incomplete (e.g. genuine sample rate and actual route/encoding not passed to UI). Do not claim original-language, lossless route or no silent fallback accepted.
- **P5-B (Q37/Q39) NOT IMPLEMENTED/ACCEPTED.** Media3 built-in `setEnableDecoderFallback(true)` already exists and should remain; it does not prove every compatible fallback or correct distinction between DRM/decoder/output/surface failure. `NvPlayer.kt` currently classifies the selected format's ST2084 as `HDR10` even for a Dolby Vision MIME without an independently validated same-release HDR10 base layer. Missing DV profile/base-layer proof, supported-display/decoder path checking and explicit **Video Playback Error — Retry / Change Stream / Back**. No resolution/stream source substitution authorized.
- **P5-C (Q29/Q38/Q47) PARTIAL/OPEN.** Existing genuine `onRenderedFirstFrame`, surface-stability gate, and muted-audio handoff are preserved and not changed. `logTaxaDeQuadros()` only logs fps and available display modes; **there is no source-verified opt-in AFR setting/mode switch/restore lifecycle (default OFF required)**. Existing `NvPlayer.kt` has device-specific forced `SurfaceView` recreation for SDR/HDR geometry; its safety on Xiaomi TV and requested in-place preference remain unverified. Detailed decoder/seek/surface recreation generation tests and output timing are absent here.
- **P5-D (Q49) NOT IMPLEMENTED/ACCEPTED.** No debounced ARC/Bluetooth/TV-speaker route-change handling or sink capability renegotiation found in inspected `NvPlayer.kt` / `NuvioActivity.kt`. Preserve `setAudioAttributes(..., false)` workaround that protects HDMI volume/audio focus. Need in-place sink recovery where possible without video/network restart; enforce no silent lossy change and explicit error otherwise.
- **Cross-cutting/P1–P4 preservation:** No P1–P4 cache, TorBox identity, subtitle, Dynamic Clock, video-surface, first-frame, master workflow or P6 file was edited in this checkpoint; only the PCM processor and its existing standalone replay test were modified. Compilation, physical HDR/DV/ARC and integrated UX regressions remain unverified.

### Final P5 source acceptance disposition (this checkpoint)

| Scope | Verdict |
| --- | --- |
| P5-A PCM regression | **Source correction committed; 14/14 independent predicates/model checks; actual Media3 Kotlin replay NOT RUN.** |
| P5-A original audio, sink/ARC and errors | **OPEN / BLOCKING.** |
| P5-B Q37/Q39 HDR/DV and decoder policy | **OPEN / BLOCKING.** |
| P5-C Q29/Q38/Q47 AFR, first-frame and surfaces | **OPEN / BLOCKING.** |
| P5-D Q49 route recovery | **OPEN / BLOCKING.** |
| Q18 | **Owner explicitly deferred; do not restart.** |
| Android full Gradle/NDK/TV verification | **P7/P8 hardware/integration gate; NOT RUN.** |

**P5 SOURCE ACCEPTANCE FAILED — P5 remains PARTIAL; P6 NOT AUTHORIZED.** Continue P5-A with real Media3 JVM replay/compile, genuine track and route capability policies, and the explicit audio error actions. Then implement/test P5-B (DV/base-layer/fallback), P5-C (AFR and surfaces/first frame), P5-D (debounced route renegotiation), and rerun final P5 source acceptance **before** authorizing P6. No P5 GitHub Actions run, APK build, workflow trigger modification, or master merge occurred.


## P5 continuation — Q37 Dolby Vision transfer-function provenance (2026-11-23; P5 PARTIAL)

**Starting HEAD:** `ae3e9e6880717a7919ef1da7cc66a04c4109eb01` (unchanged from first P5-A source checkpoint). The canonical five repository documents and owner-approved 441-line File Library specification `NUVIO_LEGACY_MASTER_PLAYBACK_SPEC_Q01-Q50_2026-11-22.md` were inspected, including §2.6, the prior P5-A partial checkpoint and its source acceptance blockers. The branch was verified before writing.

**Bounded production fix:** `1a1f88dcdf3957cc509793066960824b52a31bb9` — Q37 HDR metadata provenance. New `HdrPolicyP5.kt` now classifies a selected Dolby Vision MIME as Dolby Vision only with a recognized DV decoder; ST2084/PQ metadata alone is not accepted as proof of an HDR10-compatible base layer. For DV MIME without verified decoder and without independent same-file HDR10 base-layer proof, the classifier conservatively reports no confirmed output mode, not fabricated HDR10. The additional verified-base argument defaults **false** and is **not** currently supplied by a production container inspector. Normal non-DV HDR10/HLG classification remains. `NvPlayer.kt`'s `publicarHdr()` uses the new classifier, preserving selected release/source identity. No transport/cache/selector changes.

**Changed production files:** `android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt`, new `HdrPolicyP5.kt`. **New regression source:** `tests/p5/HdrPolicyP5Test.kt`, `tests/p5_hdr_policy.sh` (run explicitly via `bash`). Existing `GanhoAudioProcessor.kt` and `tests/cacheboost_android.py` P5-A changes are preserved unmodified.

**Executed test:** Locally compiled the exact Q37 pure-Kotlin decision cases with `kotlinc-jvm 1.9.0` and JRE 21; executed `HdrPolicyP5TestKt`, **10/10 assertion cases passed** (DV decoder recognized; no fictitious PQ/HLG fallback; explicit verified base; ordinary HDR10, HLG and unknown). This is a genuine Kotlin/JVM policy compilation and execution, **not** a production Android API35/Media3 `NvPlayer.kt` compile, decoder test or physical TV HDR handshake. The checked-in shell test runner was authored but not executed in a full repository checkout.

**Explicit tests not executed:** the earlier committed production `GanhoAudioProcessor` Media3 Kotlin PCM replay (still unverified), actual Android API35/Media3 compile/JNI link, source-level native C compile, full Gradle/APK, GitHub Actions, real Dolby Vision/MEMC/ARC hardware tests, or P1–P4 integrated runtime regressions. Media3/Android SDK classpath unavailable to the local compiler. Do not imply the earlier 14 independent numeric/source checks constitute an actual PCM replay.

**P5-A (Q36):** remains PARTIAL — original-language high-fidelity route/decoder/track-output capability, genuine track metadata, no silent unsupported-audio playback and three-action Audio Playback Error still need production implementation and tests. Existing audio PCM rounding/soft-limiter source fix is preserved, but the Media3 replay remains essential.

**P5-B (Q37/Q39):** remains PARTIAL — the one confirmed HDR10 misclassification was corrected. Independent DV profile/base-layer inspection, actual display and decoder/output capability validation, compatible quality-preserving fallback, DRM/output/surface failure classification, and Video Playback Error (Retry / Change Stream / Back) need implementation and integration tests. A conservative metadata label by itself does not reject an incompatible DV stream.

**P5-C (Q29/Q38/Q47):** NOT COMPLETE — retain genuine first-frame/surface generation protections, implement setting-backed opt-in AFR with default OFF and safe restore, avoid unnecessary device-specific surface recreation, and test transition races/first-frame sync.

**P5-D (Q49):** NOT COMPLETE — implement debounced HDMI ARC/Bluetooth/internal-speaker route observations, actual sink re-probe, in-place renegotiation where supported, track/position/effect preservation, and fail-closed output errors. Preserve `setAudioAttributes(..., false)`.

**Highest-priority next task:** run the real committed PCM processor replay with proper Media3 jars; implement P5-A native/Media3 route fidelity and distinct Audio Playback Error actions. Then complete and test P5-B, P5-C, P5-D, and perform final source acceptance before changing P6 authorization. A test-only CI workflow would require **new explicit owner authorization**; it has **not** been dispatched. Keep `docs/ci/trigger-netflix-detail-once.txt`, workflows, and master unchanged. No APK was built. Hardware/runtime integration stays reserved for P7/P8; Q18 remains explicitly owner-deferred.

**FINAL P5 SOURCE VERDICT AT THIS CHECKPOINT: P5 SOURCE ACCEPTANCE FAILED — P5 remains PARTIAL; P6 NOT AUTHORIZED.** This supersedes no successful P5 acceptance because none exists.


## P5-A continuation — conservative original-language selection checkpoint (2026-11-23; P5 STILL PARTIAL)

**Starting verified branch HEAD:** `6a6a656a8352f6e0d58351ea8577c29f45eac876`; GitHub compare showed identical HEAD before the writes. Reviewed all five canonical repository documents, the latest P5-A and Q37 checkpoints, relevant expanded owner Library spec §2.6/Phase 5, production `src/audioescolha.c` / `src/video_android.c` / `NvPlayer.kt`, and the committed native track-choice tests. No P0–P4 implementation re-started.

### Commits in order

1. `86836e1b6592e5f4d2878529af7231fe30af3451` — `src/audioescolha.c`. Fixed a narrow Q36 violation: when a known original-language track was unavailable, automatic choice previously fell through to English/Malayalam, silently changing the language. It now retains the current track selection rather than selecting a dub. Explicit remembered user choice and playable highest-quality original-language choice remain prioritized. Does not change release/source or Media3 sink.
2. `725ad5589706b118105f0d63924b69365cdd3330` — `tests/audioescolha.c`. Updated the explicit unavailable-original regression expectation to `-1` with `AUDIOESC_NADA`, rather than accepting automatic English fallback.
3. `05ecb5c5e4d9ca7c28570b38c301afb78c52f9c6` — `src/audioescolha.h`. Corrected outdated high-level selection comments; explicitly acknowledges that native codec scoring does **not** validate Android sink/output-route compatibility.

### Actual verification, with limits

- **PASS: 6/6 source-structural assertions**, executed against the freshly fetched committed production and test file contents: original-only auto-choice; remembered manual identity; `pode` candidate guard; missing-original conservative behavior; explicit changed native test assertion; documented no-dub contract. These are **not** C test execution.
- **NOT RUN: `bash tests/audioescolha.sh` and production native C compilation.** The local container has a C compiler but no authenticated checked-out repository or accessible network-mounted dependency tree. GitHub connector reads remote files but does not mount the project for native linking. A 6/6 predicate result is not a native binary PASS.
- **NOT RUN: committed actual production `GanhoAudioProcessor` Media3 Kotlin PCM replay** (`tests/cacheboost_android.py`), target Android API35/Media3 1.8.0 production `NvPlayer.kt` compile, Kotlin audio sink integration, live Android output, HDMI ARC or TV. Local host has Kotlin but no Android API35 `android.jar` or Media3 AARs; user did **not** authorize P5 GitHub Actions. Prior P5-A 14/14 independent numeric assertions and Q37 10/10 JVM policy tests remain historical, not rerun in this checkpoint.
- **No APK, GitHub Actions, workflows, release, master merge, MPV, Q18, P6, or P0–P4 cache/transport code modified.** No physical playback test. This is a narrowly bounded behavior change and its test contract; its actual C execution remains a required follow-up.

### P5 acceptance status

| Scope | Remaining non-deferred source blocker |
| --- | --- |
| **P5-A / Q36** | Actual Kotlin/Media3 PCM replay; native C regression execution; Android track sample-rate/codec/channel and sink/output-route capability evidence; correct lossless passthrough/faithful PCM policy for ARC; real unsupported-audio fail-closed behavior and three-action **Audio Playback Error** with navigation. Current native `pode=1` assumption is **not** evidence of route support. The change above prevents one automatic language substitution only. |
| **P5-B / Q37/Q39** | Genuine Dolby Vision profile, decoder/display checks and independent same-release HDR10 base-layer proof, no fabricated support, decoder/DRM/surface/error classification, bounded quality-preserving fallback and three-action **Video Playback Error**. Existing pure Kotlin `HdrPolicyP5` correction stays intact. |
| **P5-C / Q29/Q38/Q47** | First-frame/surface-generation integration regressions, actual persisted opt-in **AFR OFF by default** with supported mode selection/restoration, evidence-guided surface retention rather than forced recreation. Preserve the existing first-frame and P2-B last-frame gates. |
| **P5-D / Q49** | Real audio device callbacks, debounced HDMI ARC/BT/internal speaker re-probe, sink renegotiation without needless video/source restart, track/position/adjustment preservation and truthful error on incompatible output. Keep `setAudioAttributes(..., false)`. |
| **P0–P4** | No implementations changed beyond the native audio preference function; broad preservation tests **not rerun**. Earlier accepted source checkpoints remain as documented. |
| **P7/P8** | Full Android compile/JNI/Gradle integration, physical TV DV/HDR/MEMC, ARC passthrough/channel route, renderer first-frame timing, playback UX/D-pad and final post-#134 integrated APK pending their own authorized gates. |
| **Q18** | Explicitly owner-deferred. Do not restart. |

**FINAL P5 SOURCE ACCEPTANCE (current checkpoint): FAILED — P5 remains PARTIAL; P6 NOT AUTHORIZED.** This checkpoint does not supersede any source gate and does not claim a P5 phase passed. Next: test the actual PCM processor with correct Media3 classpath under new express test-only CI authorization or an accessible Android SDK checkout; implement end-to-end Q36 sink/route/error wiring before P5-B/C/D; compile and run committed test cases, then repeat final acceptance.


## P5-A dedicated source acceptance — 2026-11-23 (FAILED; bounded source fix)

**Starting verified HEAD:** `3b8c4473a6ada59af27e59d990aa62143e0ee1fd` (GitHub compare against branch: identical, 0 ahead/behind). Scoped to P5-A/Q36; P5-B/C/D/P6 are not authorized by this session.

**New production/source commits:**
- `b2df6a40a8a94b08960d42825502c4e39c6b9d29` — stop treating an untrusted human audio-track label as proof that the underlying codec is lossless. Source codec/MIME now has precedence for original-language quality ranking; retain DTS-HD MA subtype label handling for actual DTS codecs.
- `3a95ad1b65ef391a7ee14bc979a207a5e9cbd46c` — keep the scoring branch mutually exclusive (correct the intermediate edit above).
- `faa24f0dfe693302d9762d3d2559eabec80f8ac1` — add five native regression cases: mismatched AAC/AC-3 labels, real DTS and DTS-HD subtype, and Japanese original FLAC versus misleading AAC label. Earlier native original-language/no-dub/manual-identity cases retained.

**Evidence and limits:** production `src/audioescolha.c` and existing `tests/audioescolha.c` were read from the actual branch before editing. `tests/audioescolha.sh` requires a materialized repository checkout with `src/audioescolha.c`, `src/audioinfo.c`, `src/linguas.c` and headers; connector access does not mount these sources into the execution container, and direct git network resolution failed (`Could not resolve host: github.com`). Hence **`bash tests/audioescolha.sh`: NOT RUN; native C compilation: NOT RUN**. Static inspection is not counted as a compile/test pass. The host has gcc and kotlinc but lacks Android SDK platform android-35, Gradle Media3 1.8.0 AAR/JAR classpath and an authenticated checkout. **Production GanhoAudioProcessor Kotlin Replay.kt: NOT RUN / NOT PASSED**, even though its existing regression source was reviewed. No numerical-model substitution was counted as production validation. No Android app/NDK build or TV/ARC test was run.

**P5-A remaining source blockers:** renderer-format support is still conflated with output-route compatibility (`src/video_android.c` currently initializes candidates with `pode=1`); no end-to-end Android output-route capability/PCM-versus-passthrough evidence is passed into the native selector. There is no proven sample-rate/channel/PCM-path fidelity policy for HDMI ARC, no guaranteed soundbar codec handshake, and no honest fail-closed output state for unknown routes. The existing `EV_AUDIO_SEM_DECODER` path reports a six-second warning while video may remain silent; the approved **Audio Playback Error: Retry / Change Stream / Back** modal, bounded same-source retry and native focus ownership are not implemented. Preserve the existing manual audio choice and original-language protections, PCM processing fixes, `HdrPolicyP5.kt`, P0-P4, Q18 deferral and post-#134 refinements. DSP/passthrough real runtime checks and physical ARC verification remain for P7/P8 *after* source integration is complete.

**P5-A dedicated verdict: P5-A SOURCE ACCEPTANCE FAILED — P5 REMAINS PARTIAL; P6 NOT AUTHORIZED.** No final full-P5 review was attempted. P5-B/C/D remain pending and were not edited. Next P5-A session must implement genuine Android route capability/JNI/native selection and complete the three-action audio error on the existing native card, followed by executing real Media3 Replay.kt and native audio tests before source acceptance. **No GitHub Actions, APK, workflow modification, master merge or Q18 work.**


## P5-A dedicated source acceptance — 2026-11-23 (implementation continuation, source gate FAILED)

**Scope:** P5-A/Q36 only; P0–P4 source acceptance retained; P5-B/C/D untouched; P6 explicitly NOT AUTHORIZED. The owner-given commit `3b8c4473a6ada59af27e59d990aa62143e0ee1fd` was verified as the initial branch HEAD. Before this implementation's first write, the branch also acquired source-fidelity label fixes `b2df6a40a8a94b08960d42825502c4e39c6b9d29`, `3a95ad1b65ef391a7ee14bc979a207a5e9cbd46c`, native tests `faa24f0dfe693302d9762d3d2559eabec80f8ac1` and the then-current failed verdict `94df7db1bc934242ab61c5b207de5649e97191e3`. These were inspected and preserved. The approved 441-line Library spec, §2.6 / Phase 5, and all five authoritative repo documents were read before source edits.

### Implementation commits (source facts, not proven device behavior)

- **Generation-owned native audio retry / independent audio error card:** `bd6bd3629f835964540e376d3f3833a08d827591` (retry prototype), `15ea910f29588624d62ecb7a8fbfe7ae1b35ba16` (terminal audio renderer error and same-source generation-based reopen), `f0b84e5a06380fefbd2dd719f24149c241b25996` (native Audio Playback Error with Retry / Change Stream / Back, pause on failure), `029204943f7d99ebf27ef79aa6ff66297089d273` (audio retry flag visibility fix), `1cdb57b6be359759796ae22850b5f11f6f3be507` (avoid duplicated title), `cacf786ad03815a68720e798ec1fd4f26afd121a` (reuse tested native focus/retry policy), `ec9ef19bf164f64dda27f50f5fcc2d935a9912f5` (prevent automatic network reconnection while terminal audio failure is active).
- **Renderer / output-route separation and JNI source metadata:** `90933d168221d3537b49b79b8cd670ed654bfce0` (VideoFaixa source and route fields), `b09bc60ee086d2d904d272b1cd1ccac22801e9ae` (nativeAudioCapacidade JNI and removal of unsafe `pode=1`), `973bbcd74970b04f2750624544019f657f3c237c` (Android API33 current media route, actual advertised PCM16 formats / channel / rate / bitstream probing), `3073dcb9fd32e95ef0e5a3557c2ed36c618675f0` (Kotlin track enumeration / output gating / JNI propagation and original-language preparation selection), `473d005bab9d883abc743cfd57f4ddcbbd4d6401` (retain Context for route probing), `0c26e2c905bf6f3d75d62d8858b10f4bbcbf629a` (share codec encoding map), `0e05ba22981d30f673523303b0c5e440aaa1d773` (runtime Media3 AudioTrack PCM/encoded channel/rate/encoding validation), `f8d738eef4ca00a539d64d3f74085ce0ab9a6e3a` (native terminal route event handling), `a662a6ffa4ee6d7a64e7bbb75ed515c75b02a078` (E-AC-3 JOC and Media3 API opt-in), `bc4d17d81488721d4e252b196bce5937c1910808` (do not infer HD bitstream from ambiguous HDMI/standard ARC).
- **Original-language fidelity, manual identity and metadata:** `7559347803de59f55b64696a52a5657acab5ca72` (actual-source-quality/sink-fidelity Kotlin policy, no guaranteed lossless from codec name), `a1d0fce813c081fae31b0e89e23208f355a3409d` (highest compatible original using measured bitrate/rate/bit-depth; no accidental manual dub override; validate recovered track), `3006a9ec456b710f35377058bb078302db9ace74` (do not save unsupported manual audio track), `43b79c1916a17c2331e3a099adbcdc75c34bd43f` (Audio Tracks row shows observed sample-rate, bit-depth/bitrate where reported and only *capable/unverified* route status), `f63c4a20a4bd8c574afecf284088b820eb247b55` (avoid claiming unverified hardware losslessness). Preserved existing PCM16 rounding, limiter, DSP-disabled bypass, and encoded passthrough sources; preserved `HdrPolicyP5.kt`, original UI, native focus and no speed/delay controls.
- **Executable tests and harness integration:** `6203ca4f26f997e2196f76c04c6ad4ac81ddfd7d` (pure Kotlin decision regressions), `e7b9e3737012b8ae25a5e44c405df35a77544936` (runner), `cf7460768da54af71c0209e246076c9a439be96d` (production native three-action focus/retry helper), `3799942b099fa0bcfa6000bbfb15d7ad0193cfa6` (native test), `4c9c6bc2e77f4b8609cbf039ac2c54301401b21a` (native runner), `838b1e438404453550d2ba5177a0757e50c868ef` (existing true Media3 Replay.kt/compile harness now includes the new Kotlin P5-A files).

### Actually executed — EXACT evidence and limits

- **PASS — `bash tests/p5_audio_fidelity.sh`**, run in a locally reconstructed, minimal file tree using the **actual committed production** `AudioFidelityP5A.kt` plus **committed** `AudioFidelityP5ATest.kt`; **17/17 JVM assertions**. Exact Git blob SHA locally and remotely both `670a027e006ca4af03e49628c9d9634b8b06f05e` for production, `cc245d03baeaedd90c11bd628e7f7f1ac04056b2` for its test, and `9e928166ac1ee1c7be359d6b6a893681b6cc6999` for runner. No numeric mock or source-predicate substitute.
- **PASS — `bash tests/audioerro_p5a.sh`**, compiled the **actual committed production C focus/retry header**, `-std=c11 -Wall -Wextra -Werror -pedantic`; **10/10 C assertions**. Git blob SHAs matched: `8355cdd4033870783248f971dee74e1f5a79b4f2` production header, `a4d306d6335a264eea4d2f24e3ce337c492b5d84` test, `05eb6c87e8ae55bc35b12969811017f8a70bbdf7` runner.
- **NOT EXECUTED — actual `tests/cacheboost_android.py` production `GanhoAudioProcessor.kt`/Media3 embedded `Replay.kt`**. The host's Kotlin CLI is present, but the script requires Android platform `android-35/android.jar`, Gradle-cache AndroidX Media3 1.8.0 AAR classes, AndroidX Core/annotations, Guava, and optional SDL Android sources. None of the Android SDK/Media3 artifacts exist here. Existing PCM16 positive/negative rounding, soft-limiter, float, disabled-DSP bit-perfect and encoded bypass regressions are committed but NOT VERIFIED BY THE REAL MEDIA3 REPLAY in this session.
- **NOT EXECUTED — full `bash tests/audioescolha.sh`**: `tests/audioescolha.c`, `src/audioescolha.c`, `audioinfo.c`, `linguas.c` and dependent headers are readable through GitHub but not mounted as a runnable checkout. `git ls-remote` failed with `Could not resolve host: github.com`; no false C suite PASS is recorded. The new standalone native focus helper test PASS is **not** this full original-language C suite.
- **NOT EXECUTED — full Native SDL/JNI C compilation and Android/Media3 Kotlin compilation**: no Android NDK, SDK platform, compatible Gradle cache or Android checkout. In particular the production `video_android.c`, `player.c`, `faixas.c`, `NvPlayer.kt`, `AudioOutputProbeP5A.kt` integration is source-implemented but **not compile-proven**. No APK, GitHub Actions or workflow edits were made.
- **NOT EXECUTED — physical Android TV/JBL Cinema SB180 HDMI ARC tests**. No claim of bit-perfect delivery, actual AVR codec handshake, no downmix, physical lossless PCM or firmware route support. Android API probe declares only observable capabilities; ambiguous/old API paths fail conservatively. P7/P8 must test physical output.

### Dedicated P5-A source acceptance, and hard blockers

Audit findings: committed source now distinguishes renderer decodability from PCM/encoded route capability, publishes output metadata to native and no longer marks all renderer-supported tracks as compatible; prefers measured compatible original audio; preserves manual audio language and source identity, detects unsupported or unknown current outputs, makes unsupported audio a native three-action error rather than a six-second toast, bounds user Retry and validates new JNI source generation. The native video error remains separate and the existing audio adjustment panel and PCM implementation were not removed.

**Still not source-accepted:** Android/Media3 production compile and JNI ABI cross-check have not run; the real PCM production regression has not run; the complete original-language C regression has not run; full C/native error panel compilation and key/navigation integration remain unverified; selected PCM-versus-actual-output capability interpretation requires runtime integration checks; no full previous P4 cache/HTTP regression rerun. These are *source integration / verification blockers*, **not** merely P7/P8 physical device tests. Explicitly retain the ability to reject unknown sink capability rather than advertise lossless delivery. Recheck HDMI ARC handshakes only in P7/P8.

**DEDICATED VERDICT: P5-A SOURCE ACCEPTANCE FAILED — P5 REMAINS PARTIAL; P6 NOT AUTHORIZED.** No P5-B/C/D implementation or full P5 acceptance occurred. **P5-B cannot begin on the basis of this checkpoint.** Finish the true production PCM replay, original-language native tests and Android/Media3 + JNI integration compilation when those dependencies are available, fix any demonstrated defects, and repeat **P5-A-only** source acceptance before a new phase authorization.


## P5-A FINAL DEDICATED SOURCE ACCEPTANCE — 2026-11-23 (PASS; P5-B AUTHORIZED)

**Scope and provenance:** The previous P5-A implementation-only checkpoint at b04ee9510d4f78254e1998bf5ce7b18a6e0d13db FAILED source acceptance because the real Android API35/Media3 compiler, PCM audio replay, full original-language C regression and native/JNI syntax preflight had not run. The owner explicitly authorized a P5-A test-only GitHub Actions workflow, without an APK, release, master merge or P5-B/C/D implementation.

**FINAL GREEN CI:** [P5-A audio fidelity verification #45225191077](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45225191077) at tested feature HEAD df0e4f90d393ef51edb2e60ded03a82177b86bd3, job 137869664459 COMPLETED/SUCCESS. New dedicated workflow .github/workflows/p5a-audio-test-only.yml is restricted to its own path on the feature branch with contents:read; existing APK and P4 workflows remain unchanged. NO APK assembly, signing, upload or publication occurred.

### Actually executed, all PASS

1. bash tests/audioescolha.sh: compiled and ran the actual production native original-language selection functions and committed tests, including no auto dub, manual identity and genuine source codec priority; console result "audioescolha: ok".
2. bash tests/audioerro_p5a.sh: strict C11 compile/execution of the actual native Audio Playback Error focus/retry helper, 10/10 assertions; console "audioerro_p5a: PASS (10 production C assertions)".
3. bash tools/android/preflight-c.sh with real NDK 27.2.12479018 ARM64 Android Clang, including src/player.c, src/video_android.c, src/faixas.c and related playback/UI C source syntax and native preservation checks. Console "Early Android C compiler check: passed". This is an Android-targeted **syntax compile**, not a final NDK link or executed JNI bridge.
4. python3 tests/cacheboost_android.py with NUVIO_CACHE_COMPILE_ONLY=0: **actual Kotlin compiler success** for checked-out production NvPlayer, AudioFidelityP5A, AudioOutputProbeP5A, HdrPolicyP5, GanhoAudioProcessor, GanhoMath, AudioSync sinks, CacheMidia, parallel HTTP and supporting Kotlin, against Android API35, Media3 1.8.0 and Kotlin 2.0.21 on JDK17. Console "compile-check: ... android-35 + Media3 1.8.0: OK". A minimal NuvioActivity stub stands in for SDL Java sources not present in this test harness. This is NOT a complete Gradle app compile or APK.
5. The SAME normal-mode tests/cacheboost_android.py ran real Replay.kt JVM assertions against actual production GanhoAudioProcessor, GanhoMath and CacheSessao: positive/negative PCM16 exact rounding (+2000/-2000), float processing, soft limiting, 100% bypass, encoded AC3 guard and cache policy. Console "cacheboost_android: PASS (statvfs fit, key/names, leftover cleanup without following links, ENOSPC guard, gain exact/linear/limited/monotonic, processor 16-bit/float/bitstream)". This clears the previously unexecuted P4-origin Replay.kt:165 regression on the committed P5-A fix.
6. bash tests/p5_audio_fidelity.sh: actual pure-Kotlin production policy and committed tests compiled and executed, 17/17 assertions; console "p5_audio_fidelity: PASS (17 Kotlin/JVM policy assertions)".

**Cross-language source/ABI audit:** NvPlayer.kt declares @JvmStatic nativeAudioCapacidade with nine Int arguments (idx, hz, bits, bitrate, decoder, PCM, passthrough, faithful, uncertain), matching the nine jint arguments of Java_space_nuvio_nativelegacy_NvPlayer_nativeAudioCapacidade in src/video_android.c. Both independently pass targeted production-source compilation. Runtime JNI symbol linking/callback invocation still requires an APK/device integrated gate.

**Failures corrected (CI harness only, no new production playback changes):** Run #45223810437 failed ARM64 preflight because the SDL_image development header was not installed; fixed CI provision of libsdl2-image-dev and libsdl2-ttf-dev in d8e7fa46f3004402f970231a9d48dee09d1c5cbd. Run #45224239613 reached Kotlin compiler but its standalone source list lacked the already committed HdrPolicyP5.kt dependency of NvPlayer.kt; fixed that test harness list in 98b3c3e96c1ec2c49197ec89e8bee84b2989c98c. Test-only workflow first created in 7c4f746f3558aebf155e2dad3832aa2a16fde9a1; final rerun commit df0e4f90d393ef51edb2e60ded03a82177b86bd3 passed all steps. No speculative production logic change was needed.

**Final P5-A/Q36 source implementation review:** Prior committed P5-A source implements no silent original-language substitution or source change; output capability distinct from decoder support; real codec/channel/rate/fidelity metadata and JNI propagation; conservative PCM/encoded route probes and actual Media3 AudioTrack output validation; safety around standard ARC not eARC; preserved manual audio and effects/DSP bypass; fail-closed Audio Playback Error with Retry, Change Stream, Back and bounded same-generation retry. This CI now verifies the missing real Kotlin/Media3 PCM regressions, native selection/error C execution and ARM64 C source. **P5-A SOURCE IMPLEMENTATION ACCEPTED.**

**Explicit limitations:** No full native JNI link, complete Gradle app or APK, device callback execution, TV D-pad UI visual test, actual TorBox playback, HDMI ARC soundbar handshake, physical multichannel PCM fidelity or bit-perfect certification. Those are genuine P7/P8 integration/device gates, not claims of completed hardware behavior. Earlier P0–P4 source checkpoints are preserved, but full P0–P4 runtime regressions were not exhaustively rerun. Q18 genuine embedded P2P readiness remains explicitly owner-deferred. The green job's post-cache reserve warning did not invalidate any successful test step.

**FINAL DECISION: P5-A SOURCE ACCEPTED; P5 overall PARTIAL; P5-B source implementation AUTHORIZED; P5-C/D still pending; P6 NOT AUTHORIZED.** This supersedes only the earlier P5-A source FAILED checkpoints above; it is not final P5 acceptance. Do not begin an APK build or P6 without separate owner authorization.

## P5-B + P5-C PARTIAL checkpoint — 2026-11-24 (NOT ACCEPTED)

**Result: P5-B NOT SOURCE-ACCEPTED; P5-C NOT SOURCE-ACCEPTED; combined integration NOT SOURCE-ACCEPTED. P5 PARTIAL; P5-D and P6 not authorized.** This is a bounded partial implementation checkpoint, not a green acceptance report. Owner authorized both video subpackages and test-only CI; no APK, release, master/main merge, Q18, P5-D or P6 work.

- Starting verified feature HEAD: e69beca51633e416b6c43e4c39787fca5e863df8.
- Implementation HEAD: d719d949dfba60726fa5c1cad4feab0a3b51fad8.
- Exact combined CI verification input HEAD: b554e0c29299e486627a0c4227c03f3d8dc1f84c. The following report/tracker commit is documentation-only.
- Prior P5-A source acceptance and green run #45225191077 remain the accepted baseline; P5-A was not reimplemented.

### Production commits

| Commit | Scope and files |
|---|---|
| 6be0ecd112daae2848e9899cf058222009e1297f | Q29/Q47: NvPlayer.kt, RecriaHdr.kt, tests/p5/SurfaceTransitionP5CTest.kt, tests/p5_surface_transition.sh. Actual AnalyticsListener rendered-first-frame callback carries output and render timestamp. Reject old session, wrong/invalid Surface, and pre-invalidation frame. Invalidation remutes audio, resets stability and cancels stale handoff; retain last frame until new hardware plane is stable. Repeated detach timeout is no longer silently swallowed. |
| dc30c7b9b72e6d7af3a7aba66590e7146cc08382 | Q39: NvPlayer.kt, HdrPolicyP5.kt, src/video_android.c, src/video.h, src/player.c, src/app.c, tests/p5/HdrPolicyP5Test.kt. Separate terminal video event 17, error policy categories, bounded same-player early decoder recovery, three-action native video modal with independent retry counter; explicit same-source reopen via existing generation/track recovery; VOD selector/watchdog guards for terminal video ownership. Preserve audio-error path and existing Media3 decoder fallback. |
| 0c7e9895197f0a48ca656d91baac8e5006af8cf7 | Q38: append Android-only device-local AFR setting (Off = 0) in src/ajustes.c/h, ajustes_ux_padrao.inc, ajustes_ux_tela.inc (TV sound and video). JNI nativeAfr in video_android.c/NvPlayer.kt; FrameRateP5C.kt selects same-resolution supported cadence with HDR type guard, unknown metadata no change; disable automatic Media3 surface frame-rate voting, one window preference request per session, restore on release/system pause and reapply on explicit resume. tests/cacheboost_android.py adds real policy dependency; tests/p5/FrameRateP5CTest.kt and tests/p5_afr.sh. |
| d719d949dfba60726fa5c1cad4feab0a3b51fad8 | Q37: NvPlayer.kt, HdrPolicyP5.kt, HdrPolicyP5Test.kt. Replace Dolby-name heuristic with actual MediaCodecInfo MIME capability; selected decoder tracked during cold and prepared playback; exact parsed Media3 DV codec profile/level, size/rate and display HDR capability queried. An unverified DV output at rendered frame fails closed instead of opening cinematic gate. No same-file HDR10 fallback is advertised or enabled on unproven PQ metadata. |

CI commit b554e0c29299e486627a0c4227c03f3d8dc1f84c adds only .github/workflows/p5bc-video-test-only.yml, restricted to its own path on feature branch. Existing APK and other workflows untouched. Reuses P5-A API35, NDK27.2.12479018, Media3 1.8.0, Kotlin2.0.21/JDK17, Gradle dependency cache, C preflight and real PCM replay. Adds HDR, AFR, surface, P2 timing/handoff and prior frame policy regressions.

### Executed tests and exact evidence

- Local actual production pure Kotlin replay on Kotlin JVM 1.9.0/JRE21: **42/42 PASS** (22 HDR and video failure, 8 AFR and 12 surface generation assertions). All six files copied verbatim from the committed blobs and independently checked with git hash-object: HdrPolicyP5.kt a01faa30a45f73b0c2fb28d7a73a03f286edab05; HdrPolicyP5Test.kt c958bc6f984d0dde940d5faecd43cd191c4e54d8; FrameRateP5C.kt 1f268b688835bfbe5e6e03c12f6121b1f823bcc9; FrameRateP5CTest.kt 52e44b5b791381da5998e296ea513d148bdc121f; RecriaHdr.kt 40a190dbc2a2094ec4b4d0169a81d66423c0797f; SurfaceTransitionP5CTest.kt 8dcb0cedc91c0c665516f90aafbbf4a8efe2912f. Actual kotlinc compiled these production functions plus committed tests; kotlin executed all three main entry points. No Android/display hardware or native modal execution implied.
- [CI #45323442340](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45323442340), job 138207169760, exact input SHA above: **IN PROGRESS at this checkpoint**. Checkout, JDK, SDK setup, Gradle setup and the native regression step (audioescolha.sh, audioerro_p5a.sh, playback_p2_timing.sh, playback_p2d_handoff.sh) completed successfully. NDK provisioning/C preflight still in progress. API35/Media3 production compile, actual PCM replay and remaining Kotlin CI replay not yet verified. Retrieve final status and logs in the next session; fix any failure and rerun. Do not treat local policy tests or this partial CI result as successful full compilation.
- Local checkout via public network failed DNS; full Android SDK/NDK/Media3 compilation relies on authorized CI.

### Review A — P5-B (Q37/Q39)

**NOT ACCEPTED.** Q37 partial: genuine DV profile/level/actual decoder MIME and size/rate/display capability check now in selected-output path, conservative PQ rule preserved. But independently validated same-file HDR10 base-layer evidence and a real same-source compatible base-layer decoder selection are not implemented. The boolean pure-policy test demonstrates refusal/acceptance decisions only, not actual sample compatibility. Inspect Media3 extractor/container configuration and HEVC base-layer sample evidence; do not infer HDR10 from DV profile name or PQ alone. Audit capability-query false negatives/unknowns, non-DV HDR output evidence, HDR10+ and actual Media3 alternative MIME selection before declaring fidelity. Physical DV rendering/panel handshake is P7/P8.
**Q39 partial:** actual distinct VOD terminal video modal and separate retry budget implemented, Media3 fallback retained, early bounded same-player retry and explicit same-source retry wired. Missing dedicated executable video-modal dispatch/retry tests, exact generation/race/retry-timeout ownership verification (manual startup and VOD watchdog timestamps can remain aged after user retry), comprehensive error-routing audit (including Live TV/trailers if required by spec), actual selected-decoder ordering/quality-preserving compatible software fallback verification, and complete integration compile. Native video retry currently reuses the audio error pure retry helper, which is covered only by prior audio regressions, not a dedicated video action simulation. Ensure failed reopening preserves terminal reason and does not trigger silent selector actions.

### Review B — P5-C (Q29/Q38/Q47)

**NOT ACCEPTED.** Q29 partial: first-frame callback binds real Media3 output/timestamp/current generation; invalidation remutes and restarts surface stabilization. Earlier first-frame timing regressions pass at CI step; actual full Android compile and event-race harness pending. Audit seeks, same-player decoder retries, callback cancellation and old warm frame/overlay transitions; do not count pure timestamp tests as actual Android races.
**Q38 partial:** source-implemented persisted local default-OFF setting, supported physical same-resolution cadence, no automatic Media3 refresh votes, safe unknown rate and older API HDR no-switch, previous preference restoration. Eight pure tests pass. Need executable lifecycle/restoration/default-setting persistence tests and review changing display modes versus first-frame/render timing. Verify HDR10+ mode compatibility (current policy only distinguishes DV/HDR10/HLG); no display refresh downgrade, no needless toggles during replacement/resume. Device actual refresh and MEMC remain P7/P8.
**Q47 partial:** surface generation/identity, callback ownership, retained image and bounded repeated detach error handling implemented. Existing MStar/Amlogic exceptions preserved. Existing historical policy still permits proactive recreation on unclassified families (including Xiaomi); restrict to demonstrated need without removing actual TCL workaround, inspect normal aspect/seek and display-mode transitions, verify no hangs, black-screen regressions or unbounded recreate loops. Need production integration and transition replay; physical stability remains P7/P8.

### Review C — Combined integration

**NOT ACCEPTED.** New native error owner, Dolby capability checks, AFR window-mode request and surface invalidation must be tested together. No exhaustive previous source regression or integrated app link is claimed. In particular ensure terminal video event cannot be superseded by network watchdog or audio error, retry re-arms loading and timeouts without changing identity/tracks/position, mode changes do not permit stale-frame audio unmute, and accepted P5-A PCM/ARC behavior remains intact. Audit the two-way JNI event signatures (nativeAfr(): Boolean vs jboolean with env/cls; nativeEvento generation and event 17), verify actual production compile and all executable tests. Full linking, physical ARC, Dolby Vision, HDR, AFR/MEMC, remote focus and Xiaomi TV tests are separate P7/P8 gates.

### Next authorized continuation

Resume ONLY remaining P5-B and P5-C source implementation, focused on independently proven same-file HDR10 base-layer fallback, quality-preserving decoder and terminal-error recovery, evidence-specific surface policy and AFR/first-frame lifecycle. Check CI run #45323442340 and resolve any failed step before acceptance. Do not repeat the committed implementations or their successful local checks unnecessarily. Add dedicated executable native video modal and retry/race/lifecycle tests, run targeted real production CI, and conduct all three final reviews separately. P5-D (Q49) is NOT authorized until both subpackages pass. P5 remains PARTIAL; P6 not authorized. No APK was requested, assembled, signed, or published. Q18 remains owner-deferred.


Detailed checkpoint: [P5-B/C source acceptance audit](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md).


## P5-B/C follow-up checkpoint — 2026-11-24 (PARTIAL; NOT ACCEPTED)

Actual starting HEAD 37aaa8afe06b71eaa13e697c4f3567698c404076; follow-up verification input c03512f3214dbb3230400b869f354003503a050c; detailed evidence checkpoint d6c39060c97cec350aa4c0fee22b829bc050b15f. See [P5-B/C source acceptance report, follow-up checkpoint](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md). Changes are limited to the existing feature branch and dedicated test-only workflow. Q47 device-evidence surface policy, Q39 bounded video-error action policy/synchronous terminal preservation/deadline rearm, Q38 HDR-capability-preserving AFR policy are source-implemented. Local exact-blob executable tests: 19/19 Kotlin surface policy, 11/11 C video action policy, 12/12 Kotlin AFR policy. Prior CI #45323442340 still in progress at last check, tests prior input b554e0c29299e486627a0c4227c03f3d8dc1f84c; latest dedicated workflow path update requests exact follow-up verification at c03512f3214dbb3230400b869f354003503a050c (new run identity/completion not yet verified).

P5-B, P5-C and combined source acceptance remain NOT ACCEPTED: independently validated same-file HDR10 base-layer and actual compatible decoder selection, asynchronous retry/watchdog ownership, AFR restoration/persistence and first-frame/surface races, current native and Android/Media3 compilation and production regression CI are open. Do not repeat P5-A or already committed B/C changes. Resume these remaining B/C blockers and perform separate reviews, not P5-D. P5 overall PARTIAL, P5-D/P6 NOT AUTHORIZED. Q18 owner-deferred. No APK, release or master/main merge.


## P5-B/C continued checkpoint — 2026-11-24 (PARTIAL; source acceptance NOT PASSED)

Actual starting HEAD 08c209544d9d69aba08a504b1a56a499cb90fca6; exact current CI input 8d7c16d5866f32c9bbb529b0dee1d6a2f1113bdd; dedicated detailed report commit c669c1eed745b2d501d6c5aa88621714504ba1bf. See [updated P5-B/C source acceptance report](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md).

Previous exact test-only CI #45323442340 and #45325134406 are now COMPLETED/SUCCESS, verifying earlier production compilation/native C preflight and actual PCM regressions. New source: Q37 independently verified same-source HEVC fallback path using actual CSD/Media3 parser, actual codec configuration/capability, actual submitted clear base-layer IRAP, decoder output color metadata and current first-frame confirmation; Q39 terminal-generation latch and asynchronous explicit retry owner until real surface handoff, watchdog suppression, original error on failed retry; Q38 same-window AFR restore lease; Q29 stable-frame startup deadline and terminal audio/visual protection. Local hash-verified executed tests: 35/35 C video event/retry policy, 18/18 Kotlin AFR mode/restoration policy. Actual libx265 PQ/HLG/SDR fixture generation and ffprobe verification completed; new 23-assertion real Media3 fixture replay and current production compilation still pending. Current intermediate CI #45326397228 IN PROGRESS; exact latest CI #45326764036 PENDING at checkpoint. No APK.

Separate P5-B, P5-C and combined integration reviews remain NOT ACCEPTED until latest CI completes and residual actual lifecycle/first-frame/surface races, decoder routing/ordering and integration scope are audited. See detailed report for exact commits, CI links and open gates. Resume from this checkpoint, not the original P5-A SHA. P5 overall PARTIAL; P5-D and P6 NOT AUTHORIZED. Q18 owner-deferred. Full app linking and physical TV remain P7/P8.


## P5-B/C lifecycle and decoder continuation — 2026-11-24 (PARTIAL; NOT ACCEPTED)

Starting HEAD 680c7453f42351569c1879c3ce09af7a6c51218c; exact latest production/test input 009557a9eb9bd99fc2abfa27fac23752a8a943ca; detailed acceptance report commit 61d82b5b9055b3c333f3099f02352ebce97df766. See [updated P5-B/C source acceptance report](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md).

Q47/Q29: bounded renderer detach transaction, background pause retention, one-shot timeout and late callback rejection, genuine early reattachment frame protection. Q38/Q29: actual observed AFR mode confirmation before stable handoff, bounded timeout and restoration, no overlapping proactive surface recreation, trailer exclusion. Q39: TorBox selected-file watcher respects terminal Video Playback Error and explicit Retry ownership. Q37/Q39: hardware-first candidate ordering in actual Media3 factories, incompatible native DV and unsupported software candidates rejected before initialization while retaining same-source fallback. Local exact-blob executable Kotlin tests passed: 44/44 surface, 34/34 AFR, 27/27 HDR/decoder policy (105 total, 46 new).

CI #45326397228 still running at checkpoint; #45326764036 superseded and cancelled; [latest test-only CI #45328042919](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45328042919) pending on exact input 009557a9eb9bd99fc2abfa27fac23752a8a943ca. Prior green CI #45325134406 is earlier source only. Current real HEVC fixture replay and Android/Media3/NDK compilation unverified. All three acceptance verdicts remain NOT PASSED. Next continuation prioritize actual CI results and demonstrated integration defects, not speculative changes. P5-A preserved, Q18 deferred, P5-D/P6 NOT AUTHORIZED. No APK, release or default-branch merge.


## Final P5-B/C integration review follow-up — 2026-11-24 (PARTIAL; acceptance NOT PASSED)

Starting verified HEAD 33ccbba2b32a31202d4298c47a44ec65136695c3. Prior CI #45328042919 attempt 2 completed SUCCESS on exact input 009557a9eb9bd99fc2abfa27fac23752a8a943ca, with actual logs confirming real Media3 Kotlin/C preflight, PCM/HEVC replay and all policy regressions; starting HEAD had only two docs changes after that input. The integration review found specific residual defects and committed their fixes: Q39 bounded actual runtime decoder alternative with same-representation guard and native recovery ownership, no watchdog source substitution; Q37 generation-tagged HDR status and preserved terminal position; Q29 reject stale seek PixelCopy and rearm genuine non-no-op pre-handoff seek. Local exact-blob C test PASS 47/47 (12 new); extended Media3 Format/HEVC test 38 assertions pending CI. Detailed report commit 46f1dc63d1c9256384f651c352a5daeed292d987: [P5-B/C source acceptance report](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md).

[CI #7 / #45336498152](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45336498152) in progress on exact latest production/test input 25719c2f27c90915052e24135d494894c5eee6f5. All three acceptance verdicts still NOT PASSED at new revision. Next gate: verify this exact CI, then bounded review of actual new recovery/seek/format callback paths; resolve demonstrated defects only. No repeat of completed source or unchanged tests. P5-A remains accepted, Q18 deferred, P5-D and P6 NOT AUTHORIZED. No APK, release or default-branch merge.


## P5-B/C bounded final integration review — 2026-11-24 (exact CI gate pending)

Starting HEAD 8cdea93a14ca3c34bebf7472339a910feb3f6420. CI #7 / #45336498152 completed SUCCESS at exact prior source 25719c2f27c90915052e24135d494894c5eee6f5; verified actual log compilation, PCM/HEVC replay, 38 real HEVC/Format, 47 native video error, 27 HDR, 34 AFR, 44 surface, 17 P2-B and existing post-134 guards. Starting HEAD had documentation-only descendants.

Bounded final source review confirmed two actual Media3 integration gaps; both fixed in single atomic production/test commit bd31048dc133e886122f45badead8addef51741e. Q37/Q39 guarded platform video renderer requires revalidation of changed DV/software-decoded formats and verifies actual output before accepting DV/base output, preserving stock extensions and normal hardware playback. Q29/Q47 actual renderer reset and one-shot playback-thread seek acknowledgment distinguish real seek reset from adjusted no-op; stale/terminal seek work cannot release the handoff. Native recovery owner now covers early video resource and detach retries. Exact local Kotlin policy replay PASS 55/55 (11 new); extended real Media3 HEVC/Format replay 47 assertions pending new CI. See [dedicated source acceptance report](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md).

[CI #8 / #45337986924](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45337986924) on bd31048dc133e886122f45badead8addef51741e: IN_PROGRESS at checkpoint. Requirement-level final source audit completed, but formal B/C/combined source acceptance remains pending exact CI #8. After it passes, verify logs and exact-source equivalence, record acceptance verdicts unless a specific demonstrated defect remains. Do not restart another broad audit or expand scope indefinitely. P5-A preserved; Q18 owner-deferred; P5-D/P6 NOT AUTHORIZED until required gates (P6 remains separately gated). No APK; P7/P8 physical/runtime/full-link checks remain distinct.


## P5-B/C FINAL SOURCE ACCEPTANCE — 2026-11-24 (PASSED)

Verified exact production/test SHA bd31048dc133e886122f45badead8addef51741e and starting HEAD d09dd3f38f54ee9474a70e1b67a35beed226a21d (documentation-only descendants). [CI #8 / #45337986924](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45337986924), attempt 1, job 138254514151: COMPLETED SUCCESS. Actual logs confirm real API35/Media3 production Kotlin compilation, C/JNI preflight, PCM and HEVC/Format replay (47/47), native video error (47/47), HDR (27/27), AFR (34/34), surface/startup seek (55/55), P2-B frame hold (17/17), cinematic handoff (41 checks), P5-A audio policies and post-134 source guards. See [dedicated final source acceptance report](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md).

**P5-B Q37/Q39: SOURCE ACCEPTED. P5-C Q29/Q38/Q47: SOURCE ACCEPTED. Combined P5-B/C integration: SOURCE ACCEPTED.** The bounded source audit is closed, no unchanged CI rerun or broad repeat audit required. Next authorized package: **P5-D — Q49 audio-route recovery only.** Preserve P5-A and P5-B/C accepted behavior. P5 remains PARTIAL pending P5-D and final overall P5 source acceptance. P6 NOT AUTHORIZED; Q18 owner-deferred. Full application linking and physical DV/HDR/ARC/AFR/A-V/surface/remote verification remain P7/P8. No APK or default-branch merge.


## P5-D / Q49 implementation checkpoint — 2026-11-24

**P5-D source implemented; dedicated and overall P5 source acceptance PENDING.** Starting HEAD 0367b20953864167e2f302f3afcfbbf6d4317750. Production/test input 5d1a10d055c7caadab05196286c526f1375798fb. Actual local exact-source Kotlin route policy: 46/46 PASS (40/40 on prior revision). First [test-only CI](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45343641760) on a190d6199b0ca6d84ef5656d1db22348148995f1 passed native regression/audio-focus step and remains in progress; [latest exact CI](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45344027529) on 5d1a10d055c7caadab05196286c526f1375798fb queued at checkpoint. Neither is claimed as final successful CI. See [Q49 dedicated checkpoint](PLAYBACK_50_P5D_SOURCE_ACCEPTANCE.md) and [overall P5 checkpoint](PLAYBACK_50_P5_FINAL_SOURCE_ACCEPTANCE.md). P0–P4, P5-A/B/C accepted historical checkpoints preserved; Q18 owner-deferred; P6 NOT AUTHORIZED. No APK/workflow trigger changes or default-branch merge. Next: exact latest completed CI logs and demonstrated failures only; physical ARC/Bluetooth/speaker/HDR/A-V verification and full link remain P7/P8.


## P5-D AND FINAL P5 SOURCE ACCEPTANCE — 2026-11-24 (PASSED)

Verified [CI #2 / #45344027529](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45344027529), attempt 1, job 138278298592, COMPLETED SUCCESS on exact production/test input 5d1a10d055c7caadab05196286c526f1375798fb. Actual completed logs reviewed: real Android API35/Media3 1.8.0 Kotlin compile, NDK C/JNI preflight, PCM replay, actual HEVC/Format 47 assertions, native original-language/audio error/video error, HDMI audio-focus guard (2/2), Q49 46 assertions and source contracts, Q36 17 assertions, HDR 27, AFR 34, surface/startup seek 55, P2-B 17, cinematic handoff 41 and post-#134 guards all PASS. Pre-acceptance HEAD 35065510836b8f12edd07433af27038489f43200 differs from tested SHA only by the three reports. Final verdicts recorded in [P5-D dedicated source acceptance](PLAYBACK_50_P5D_SOURCE_ACCEPTANCE.md) and [overall P5 final source acceptance](PLAYBACK_50_P5_FINAL_SOURCE_ACCEPTANCE.md).

**P5-D Q49 SOURCE ACCEPTED. P5 OVERALL SOURCE ACCEPTED. P6 AUTHORIZED as the next source package, NOT STARTED.** All Q29/Q36/Q37/Q38/Q39/Q47/Q49 source gates accepted; no further demonstrated source blocker from the bounded review. P0–P4 accepted, Q18 owner-deferred. No production edits or CI rerun in this acceptance-only continuation. No APK, release or default-branch merge. Full link and physical Xiaomi TV / ARC / Bluetooth / speaker / HDR / AFR / A-V / remote verification remain P7/P8.


## P6 bounded implementation checkpoint — 2026-11-24 (PARTIAL)

Verified starting HEAD e8b523c775c93cf2d89d71506d730f410bf64c0e. Production/test/CI input b9ef36390c31e3c83ef453a3e426be907b5584ff: 18 commits ahead, zero behind; 14 changed paths. Added request-owned system pause versus automatic recovery policy, lifecycle epoch, stale scrub protection, session progress timer reset, real EOF gate for automatic Next while retaining native posplay, and abandoned preload cancellation. Exact blob-matched local C test **17/17 PASS**, actual Kotlin JVM test **26/26 PASS**. Dedicated [P6 test-only CI](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/workflows/p6-lifecycle-test-only.yml) committed; exact push run and completed logs not accessible through current PR-filtered workflow lookup, so CI is NOT VERIFIED. No APK or existing APK workflow modification.

**P6 NOT SOURCE ACCEPTED.** Q20/Q22/Q44/Q48/Q50 partial; Q45 remaining source implementation blocker (verified credits, configured fallback and idempotent watched marking/sync without playback exit). See [P6 detailed checkpoint](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md) for remaining precise integration gates and test evidence. P0–P5 acceptance preserved, Q18 deferred. Next: verify actual CI, complete remaining P6 gates, and close P6 source acceptance before P7. P8 remains blocked pending explicit build authorization.


## P6 continuation — 2026-11-24

Starting HEAD `e436435bc75c74bc6093a7524e17f84fb24c8d5f`; new source/test/CI input `c0d83b8264091c0d8005e510b4095e7672f335dd`. Q45 watched marking moved into playback with strict chapter provenance, configured fallback, viewing proof and journal/manual override guards; Q44 delayed-open pause ownership and Q20/Q50 background cancellation strengthened. Local exact helper tests 73/73 PASS. New native chapter/journal/progress tests and earlier Android regression harness scheduled through test-only workflow path update. CI result not independently verified. See [P6 continuation acceptance report](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md). P6 remains partial; P7/P8 not authorized; no APK.


## P6 dedicated review checkpoint — 2026-11-24

Verified starting HEAD `700b242bc53a40e9d5dbdbf9e0d5e0a696f81d8b`; exact production/test-only input `79ca121a541c5d8d2f348cd3e0f126ecef0e698d`, 25 commits ahead / zero behind / 21 changed files. Corrected Next target lost on cold source reset, stale transport after Home, focus-loss/real background distinction, confirmed checkpoint/preview cancellation, unique viewed intervals, resume/CW/actual EOF separation, and old post-play state on replacement. Exact Git blob-matched C helper tests **35/35 PASS**, UBSan **35/35 PASS**. Prior CI owner-reported success, new CI not verified. See [dedicated requirement-level review](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md#dedicated-source-acceptance-review--2026-11-24-latest-verdict) for the six remaining gates. P6 PARTIAL; P7/P8 not authorized. No APK.


## P6 credits-provenance continuation — 2026-11-24

Verified starting HEAD `b8e88de88717a2a3da6ebe91ca8f77fa1ab3ca14`, two documentation-only commits after CI #3. Independently verified completed successful P6 runs [#1](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45347395005), [#2](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45349531453), [#3](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45355672435). Reviewed actual #3 job logs: real API35/Media3 compilation, native preflight, PCM/HEVC and persisted history/progress regressions, P6 postplay 17, watched 35 and intent 33 assertions passed. Production/test revision `cfb6fbf2bfdacff521d010ce6a1f8fa3aaaee0ba` closes the demonstrated Q50 release-unverified automatic credits seek defect: exact fresh-file closing-credit interval proof, next chapter boundary, source reset, and production auto-selection regression tests. Dedicated [CI #4](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45364177583) is in progress; the native chapter/history/progress step including intro tests and native preflight are successful, full conclusion not yet established. See [P6 acceptance checkpoint](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md). P6 remains PARTIAL / NOT SOURCE ACCEPTED; P7/P8 not authorized. No APK build, signing, publication or default-branch merge.


### P6 — immediate Next cinematic and history identity continuation (2026-11-24)

Starting HEAD `8e361edd870f95877e76e4ab7446f84cf97445b1`. New production/test commits `f37e42defb1400bde6f3e5129eb2eeb343d805ee`, `91e469656a3b734e4b5c066d17fa40a5436a2415`, `bb4b1bd2c6b4659929bfac2782ba70bba04d2f81`. Q20/Q48 pending worker transfers to immediate exact-target cinematic; Q45 namespace-aware history, persisted progress rejection/reload, frozen episode identity and canonical lookup; Q50 movie EOF overlay. Independently verified CI #4 and #5 completed SUCCESS, #5 new Next 19/19 assertions; #6 new actual history/progress tests passed before SDL header prerequisite failure; test order fixed in #7. [Exact latest CI #7](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45366395775) in progress. Detailed evidence and limitations in [P6 source acceptance report](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md). **P6 remains partial**: verify #7, complete actual lifecycle/recovery/cleanup and remaining Q45/Q50 integration gates. No P7/P8, APK, signing, publication or default branch merge.


### CI #7 verification — 2026-11-24

Independently verified [test-only run #7](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45366395775), exact production input `bb4b1bd2c6b4659929bfac2782ba70bba04d2f81`, job 138349109101: **SUCCESS**, all steps completed. Actual logs confirm opaque history persistence and stale-unwatch rejection, opaque progress reload and collision rejection, `catalog_episode_identity512: PASS`, ARM64 C/JNI syntax preflight, real Android API35/Media3 1.8.0 Kotlin compile and PCM/HEVC replays. Next ownership 19/19, post-play 17/17, watched 35/35, playback intent 33/33, route 46/46, surface 55/55 and frame hold 17/17 passed. This supersedes the in-progress status of #7 and the SDL prerequisite failure of #6. No redundant rerun needed.

**P6 remains PARTIAL / NOT SOURCE ACCEPTED.** No new production code in this verification checkpoint. The remaining requirement-level integration gates in the preceding report remain open, with Q44 paused restoration after Activity/process recreation the major implementation gap. P0–P5 acceptance and Q18 owner deferral preserved. No P7/P8, APK or default-branch merge authorized or performed.


### P6 Q48 detached adoption / Q22 recovery transport checkpoint — 2026-11-24

Starting HEAD `fc5ad8fa964b3045a1be23c2d9f98974155d5f5c`. Exact production revisions `e7e2ea7de3b8fd327f584e16b9117cea2cdfd2bb` and `ccccb406d35bd1b77a5bf510d3a3a014712c47cb`. Q48 explicit detached preload ownership, exactly-once cancellation/adoption, stale callback rejection, failed-open cleanup; Q22 transport toggle uses scheduled recovery intent instead of stopped-renderer state. [CI #8](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45371074508) job 138365187415 and [CI #9](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45371423771) job 138366767122 independently verified COMPLETED SUCCESS, all steps passed. Actual latest logs: reconexao: ok; detached preload 17/17, intent 33/33, Next 19/19, watched 35/35, native post-play 17/17, real Media3 Kotlin compile and ARM64 C/JNI preflight, PCM/HEVC replay and existing P5 regressions passed. No redundant rerun. See [P6 requirement checkpoint](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md). **P6 PARTIAL; Q44 process/activity recreation restoration remains unimplemented.** Continue with exact-profile/task-bound paused checkpoint and restoration, preserving source privacy, profile gate, SDL process restart guard and explicit source consent; finish remaining integration gates. No P7/P8, APK, publication or merge.


### P6 Q44/Q45 continuation — 2026-11-24

Starting HEAD `ec1365dff2f1f17c46d4112cc1d7e18569ac7ef8`; latest test-only production input `7d838fbdb5786c347f055febe39eaeb940b50364`. Q44 one-shot task-authorized paused recreation, exact saved/fresh source, track/duration proof, duplicate lifecycle guard and bounded matching implemented. Q45 full-width identity-safe progress and actual catalog snapshot improvements committed. [#10](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45373464446) and [#11](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45373852936) verified successful; #11 restoration 29/29, all steps green (isolated Activity stub). #12/#13 actual progress regressions passed but obsolete UI guard failed; corrected in `aa6781bb03e7622ab94e3153ffb526ed81dba93b`. #14 superseded/cancelled; [#15](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45375193570) in progress, [#16](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45375394883) pending with new full production Kotlin/Activity compilation gate (guarded no APK/native build). See [P6 detailed report](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md) for exact commit/run matrix and remaining integration gates. P6 remains partial until exact latest CI and bounded review. No P7/P8 or APK.

### P6 CI #17 verification — 2026-11-24

Independently verified [CI #17](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45376840069), exact input `6816f552ee3e420c3ff7ed6ff55bf26fde3d7c02`, job `138385272302`: COMPLETED SUCCESS, all steps passed. #15 completed SUCCESS; #16 failed before actual Kotlin compilation because the no-APK task graph guard misclassified `:app:packageDebugResources`. #17 narrowly exempts that exact resource-only task, retains APK/native-build restrictions, and confirms actual full production Kotlin compilation including NuvioActivity and SDL bridge, no APK output, ARM64 C/JNI syntax preflight, catalog_episode_identity512, Media3 API35 compilation, PCM/cache and HEVC replay. Actual logs confirm P6 post-play 17/17, Next ownership 19/19, watched 35/35, intent 33/33, detached preload 17/17, paused task ticket/exact-source proof 29/29; earlier P5 policies passed. No redundant rerun needed.

**P6 remains PARTIAL / NOT SOURCE ACCEPTED:** full Kotlin compile blocker cleared, but remaining production integration/requirement review in the P6 detailed report still needs closure. No P7/P8 authorization, APK, default-branch merge, full native link or physical device validation. This checkpoint is documentation-only.


## P6 production integration audit continuation — 2026-11-24

Starting HEAD `134fbf4f1ea65c020dc8e279c84c9cd1179164f8`; CI #17 independently verified SUCCESS on `6816f552ee3e420c3ff7ed6ff55bf26fde3d7c02` (150/150 P6 policy assertions, complete actual Activity/Kotlin compile, no APK). New source/test revisions `e0a983d5fc14f9d5a6d0cfbced4ceea13207ca8d` (Q45 frozen TVDB target and journal retry) and `cb92c47dfcd2a0b7f3a3d44858868457a0977d39` (Q45 reject truncated CW/stream identities; persisted full-width record retained). [CI #18](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45387688053) in progress; [CI #19](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45387980228) queued at checkpoint. New regressions not yet verified. Detailed six-question review in [P6 acceptance report](PLAYBACK_50_P6_SOURCE_ACCEPTANCE.md). **P6 PARTIAL / NOT SOURCE ACCEPTED**; pending exact CI, remaining legacy post-play regression reconciliation and production integration evidence; no P7/P8 or APK authorized.

CI #18 subsequently completed FAILURE: actual history regression could not link because the older mock HTTP harness lacked rede_pedir, rede_resposta_limpar and rede_baixar_st_retry. These mock-only implementations are added in the next test-only commit; no production networking changed. CI #19 was already in progress on the previous revision and does not include the mock fix. Exact corrected revision requires a new CI result; P6 remains partial.

### CI #20 second attempt — verified 2026-11-24

[CI #20 attempt 2](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45388175501/attempts/2), exact input `0380b0909294f241627cbeca48ec1a3a5730288b`, job `138449084345`: COMPLETED SUCCESS, all steps green. Independently inspected actual logs: `catalog_episode_identity512: PASS`, `Q45 frozen playback TVDB identity survives catalogue change and journal retry`, `visto: tudo ok`, opaque progress reload/collision rejection, native ARM64 C/JNI preflight, actual full production Kotlin including NuvioActivity and SDL bridge, Media3 PCM/HEVC replays. P6 post-play 17/17, Next 19/19, watched 35/35, intent 33/33, detached adoption 17/17, paused task/source 29/29 (150/150) plus earlier policy regressions. #18/#19 failed on missing legacy mock HTTP functions; #20 attempt 1 passed native/history tests but failed Maven Central HTTP 429 during dependency resolution; attempt 2 passed unchanged source. Both Q45 fixes verified within this harness. No redundant rerun required.

**P6 remains PARTIAL / NOT SOURCE ACCEPTED:** Q50 legacy production overlay/countdown regression reconciliation and actual integration evidence remain outstanding, including full-width CW UI reachability limitation. No full native application link, APK, P7/P8 authorization or physical TV validation. This checkpoint is documentation-only.

### P6 Q50 actual post-play continuation — 2026-11-24

Starting HEAD `c44c3e5742763bfef1d3311518d6fe503fdb361c`; #20 attempt 2 verified successful. Corrected Q50 real EOF first-frame recommendation availability (movie without credits marker, episode with no final window sample) while retaining dismissal, rejecting seek-in-flight stale EOF, and refusing remote OK during background suspension. Reconciled legacy post-play fixtures with independently injected backend EOF and configured countdown; added focused Linux host regression linking actual posplay.c, catalogo.c and proximo.c, with mocked backend/settings/worker boundary (no actual rendering or network). CI workflow includes this regression. Exact new CI pending; older full-app host suites changed but not executed by this bounded harness. Long-ID CW full UI reachability remains open: base IDs plus episode fields are used inconsistently by downstream consumers; do not widen CatItem globally or substitute base-only composites without auditing all callers. P6 remains partial; P7/P8 and APK not authorized.

### CI #21 attempt 2 verified — 2026-11-24

[CI #21 attempt 2](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45397149051/attempts/2), exact source `58d5e3a687de9bdfd4e7479cb4eb0c49d188cb8d`, job `138507375122`: COMPLETED SUCCESS, all steps passed. Attempt 1 already passed actual post-play native integration but failed dependency resolution due to Maven Central HTTP 429; unchanged rerun passed. Actual log confirms `P6 actual posplay module: EOF, credits, dismissal, Home, seek, specials and movie recommendation PASS`, catalogue ID and frozen history retry regressions, actual production Kotlin/Activity/SDL compilation, native C/JNI preflight, Media3 fixture checks and all 150 P6 policy assertions. The new bounded production posplay integration harness is verified; the older full-app legacy suites were reconciled but have not been run. No redundant rerun needed. **P6 remains PARTIAL / NOT SOURCE ACCEPTED**: full-width Continue Watching UI reachability, remote reconciliation and remaining integration acceptance review outstanding. No physical TV execution, full native application link or APK; P7/P8 not authorized. Documentation-only checkpoint.

### P6 Q45 CW and reconciliation continuation — 2026-11-24

Starting HEAD `b5c62c80b43445c9272c09f6fefdd31259f399d7`, #21 attempt 2 verified. Local and optimistic paused CW cards retain valid full-width title and explicit season/episode pair when composite cannot fit. Card identity is not a stream ID; app CW passes explicit pair and waits for final metadata before resolving published video. Stream IDs exceeding transport buffers still fail closed. Same-percent episode change now publishes; explicit pair wins over stale composite in CW reconciliation. Same-episode newer progress refreshes timestamp/remaining time and preserves Q45 early checkpoints; changed pair clears stale TVDB; Trakt completion evidence uses exact TVDB where present. Guarded next-up seed composition; actual account watched evidence now passed even without optional gate; exact Trakt dispatch cannot fall back to numeric pair. Added CW and actual catalogue fixture tests. CI pending. Full UI and remote-account integration remain unverified; next-up side tables remain composite-only; full-width stream transport remains outstanding. P6 PARTIAL; P7/P8 and APK blocked.

### CI #22 verified — 2026-11-24

[CI #22](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45422034814), exact source `a5efc2b70050029a46774b21b10c17d894846c0e`, job `138543860185`: COMPLETED SUCCESS on attempt 1, all steps passed. Actual log confirms catalogue episode identity, optimistic CW publication with long base and explicit pair, opaque history reload, frozen TVDB dispatch/retry, actual posplay integration, native C/JNI checks, actual production Kotlin/Activity/SDL compilation, P5 fixture/policy regressions and 150/150 P6 policy assertions. Bounded paused-card/episode lookup behavior verified; oversized stream-ID transport, full UI routing and end-to-end remote account reconciliation remain unverified. **P6 PARTIAL / NOT SOURCE ACCEPTED** pending remaining integration and final review. No APK, P7/P8 not authorized. Documentation-only checkpoint; no redundant CI run.

### P6 Q45 exact published-ID and account pull audit — 2026-11-24

Starting HEAD `4431f567fa5b8a5f0fbf9647dfa1c8b5c34cb367`; #22 verified. Transport audit identified fixed 64-byte target buffers in app/player, source selector, addons worker, subtitle worker and preloading; generic display JSON reader can return truncated ID as success. Added strict root identifier decode and distinct invalid-published-video marker; cat_id_stream rejects unrepresentable published video rather than invent numeric fallback. Account watched importer rejects oversized/malformed IDs and invalid episode pairs, applies persisted watched/unwatched evidence to title and episode badges. Added strict decode and catalogue regressions and actual account importer + journal + Trakt progress reader test with mock network/disk/profile. CI pending. Source/selector direct-input boundary guards and full-width stream transport remain outstanding; do not claim IDs beyond 63 bytes playable. No global buffer widening. No full app link, physical TV or live service account test. P6 PARTIAL, P7/P8/APK unauthorized.

### CI #23 verified — 2026-11-24

[CI #23](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45427317477), exact source `65879432ef2a850b39d7f63d4486a6687a2b7a80`, job `138562724466`: COMPLETED SUCCESS on first attempt; all steps successful. Actual logs confirm `P6 exact metadata/history identifier decode PASS`, `Q45 actual account pull, persisted unwatch, newer remote, stale Trakt and profile isolation`, catalogue and CW regressions, frozen TVDB dispatch/retry, actual native posplay integration, Android C/JNI preflight, complete app Kotlin/Activity/SDL bridge compilation, P5 Media3 regressions and 150/150 P6 policy assertions. Remote history importer integration is verified against simulated remote responses and persistent journal, not a live account. Direct-input stream boundary guards, full-width transport limitation and final P6 source acceptance review remain open. P6 PARTIAL / NOT SOURCE ACCEPTED. No redundant CI run, APK, full native app link or physical TV validation; P7/P8 not authorized. Documentation-only checkpoint.
