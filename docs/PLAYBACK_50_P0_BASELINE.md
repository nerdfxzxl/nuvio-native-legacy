# P0 Baseline and Preservation Audit — Q01–Q50

**Audit date:** 2026-11-22  
**Result:** P0 completed as a **read-only source/branch audit plus documentation**. No feature implementation, test execution, native compilation, APK workflow, GitHub Actions dispatch, trigger modification, or TV test performed.  
**Starting branch HEAD:** `5a9975c19a88b2bb22365a5096c0fc62fd50a968`; immediately prior feature/design handoff `6171645e05d2137d2a1e9ed1b113687246acd7ca`.  
**Repository:** `nerdfxzxl/nuvio-native-legacy` / `feature/official-stream-selector`. Never modify `master` without explicit owner consent.  
**Evidence:** GitHub branch-qualified Contents/Commits/Compare API, actual source files, build scripts and checked-in tests. No local working tree was checked out, so **uncommitted local modifications cannot be certified** by this audit.

## 1. Source and artifact provenance

- Latest built APK per [#134 handoff](PLAYER_BUILD_134_HANDOFF.md): [GitHub Actions run 44987999328](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44987999328), source SHA `bad57b2f9c9fecdfd8399372d576834e26406023`, artifact `nuvio-native-legacy-android-debug`, artifact ID `14740439614`. Handoff says preflight/build passed; **actual physical-TV pixel-parity and runtime acceptance were not performed**. Do not describe #134 as fully TV verified.
- GitHub compare `bad57b2...6171645`: **ahead 26 commits, behind 0, 19 changed paths**. Changed source files: `src/addons.c`, `src/addons.h`, `src/faixas.c`, `src/home.c`, `src/legendasui.c`, `src/legendasui.h`, `src/parental.c`, `src/player.c`, `src/plrguia.c`, `src/plrui.c`, `src/text.c`, `src/text.h`, `src/video.c`; tests: `tests/addons_legendas.c`, `tests/legendasui.c`, `tests/player_home_polish_134.py`, `tests/player_panel_131_contract.py`; two docs. The later Q50 planning commits changed documentation only.
- [Post-#134 handoff](PLAYER_PENDING_REFINEMENTS_AFTER_134.md) says **COMMITTED ONLY**; no new APK generated for these changes. Its live-device acceptance requirements remain pending.
- Android package `space.nuvio.nativelegacy`, Media3/ExoPlayer Kotlin bridge, Android SDK 35, min SDK 24, Android NDK `27.2.12479018`; `android/app/src/main/cpp/CMakeLists.txt` globs `src/*.c` and filters Tizen player files. Native UI C/SDL2/GLES2; Android Media3 runs through `NvPlayer.kt`, `NuvioActivity.kt`, and `src/video_android.c`. Do not copy NuvioTV's Compose view files directly.

## 2. Current implementation surface / source ownership

| System | Files verified in feature branch | Preserve / next-stage inspection |
|---|---|---|
| App navigation, Play/Resume, episode routing, source choice | `src/app.c` (`abrirProximoEpisodio`, source-selection entry), `src/detail.c`, `src/streams.c`, `src/fontepref.c`, `src/fontecache.c` | P1 ownership generation and precise selection flow; P3 Dynamic Clock focus/reloads |
| Native OSD and first-frame visual | `src/player.c`, `src/plrui.c`, `src/plrguia.c`, `src/abrecine.c`, `src/gfx.c` | Q01–Q29 arc/cinematic; preserve centered responsive pill, title staticness and focus |
| Android playback and callbacks | `src/video_android.c`, `android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt`, `NuvioActivity.kt` | P1+P5 session lifecycle, first-frame event, decoder, audio output and surface |
| Subtitle and audio pages | `src/faixas.c`, `src/legendasui.c`, `src/legendasui.h`, `src/assrender.c`, `src/addons.c` | Preserve tab/Back/Format A/hold-OK and detailed audio controls |
| Parallel download / cache / network | `ParaleloDataSource.kt`, `CacheMidia.kt`, `src/cacheboost.c`, `src/video_android.c`, `src/video.c` | P4 separate RAM buffer selection, byte identity, real network speed and disk cache |
| Credits, recommendations and next episode | `src/posplay.c`, `src/posplay.h`, `src/intro.h`, `src/app.c` | P6 extend native post-play rather than replace; preserve scene/callback/preload |
| Artwork and layout | `src/home.c`, `src/gfx.c`, `src/detail.c` | Preserve landscape hero/gradient logic and existing artwork |
| Diagnostic surfaces | `src/diagnostico.c`, Kotlin `NvPlayer.kt`, `src/video_android.c` | Q46 truthful stats and redaction |

Existing useful behaviors **verified in source**, not yet functionality-tested in this audit:
- `src/video_android.c` documents the native JNI `EV_PRIMEIRO_QUADRO` / `onRenderedFirstFrame` path; `NvPlayer.kt` has `setEnableDecoderFallback(true)` and `STATE_ENDED`.
- `NvPlayer.kt` has explicit audio attributes and asynchronous release (`liberarEmFundo`), Media3 player, `CacheMidia` and `ParaleloDataSource` integration.
- `src/posplay.c` already has movie credits rules, delayed countdown, post-credit scene safeguards (`aguardaCena`) and next-episode presentation.
- `src/legendasui.c` already calls UTF-8 long-token wrapping for source-mode subtitles with up to four lines.

## 3. Preservation matrix for every unbuilt post-#134 refinement

| Protected item | Branch source evidence | Status before APK |
|---|---|---|
| Official-style parental guide, no custom backdrop, prominent white line, severity order | `src/plrguia.c` defines `PG_LEFT`, draws white line; `src/parental.c` ranks Severe/Moderate/Mild | **Code present**, visual and animation not device tested |
| Mobile landscape hero | `src/home.c`: `heroCompactoTelefone`, `homeRetratoTelefone`, hero drawing scope | **Code present**, actual mobile-landscape screenshot pending |
| Single Back closes entire panel | `src/faixas.c`: `aberta=0;audioTopoFoco=legendaTopoFoco=0;` on Back | **Code present**, actual remote modal cases pending |
| Subtitle Format A / English-only source row / true long-token wrap | `src/legendasui.c`, `src/addons.c`, `src/text.c`: up-to-four-line wrapping; readable release metadata and opaque-ID fallback | **Code present**, addon payloads and rendering untested |
| Subtitle font 2.5% increments | `src/faixas.c`: `int v=e->tamanho+dir*3;` | **Code present**, display/value calibration pending |
| Held OK acceleration | `src/faixas.c`: `ajusteRepetir` in adjustment paths | **Code present**, Android TV center-key repeat pending |
| Absolute buffer end / honest seek rail | `src/video.c`, `src/player.c`, `src/plrui.c` changed | **PARTIAL / ANDROID GAP DISCOVERED BELOW** |
| Responsive compact/expanded pill, exclusive button focus | `src/player.c`: `baseW+2.0f*expanded+28.0f`, anchored `baseX`, focus spring | **Code present**, actual pill/focus/animation pending |

**Critical Android-specific finding:** The changed `src/video.c` function `video_buffer_fim()` (near line 3523) is under the webOS-specific compile branch. Android implements its own `video_buffer_fim()` in **`src/video_android.c` near line 1197**, currently returning a buffered endpoint **only for live video**, otherwise `0.0`. Meanwhile `src/player.c` near 7327 paints the VOD buffer rail from `video_buffer_fim()/duracaoSeg`. Consequently, the post-#134 “true absolute buffer rail” change **does not yet provide measured VOD buffering on Android**. Do not mark the Android task complete or assume the desktop/source contract proves it; P4/P7 must supply/test an actual Android Media3 VOD buffered endpoint without fabricating values. Preserve the no-fake-buffer principle.

## 4. Preflight/test catalogue (inspected, **NOT executed**)

| Stage | Existing command / file | Limitations |
|---|---|---|
| Static home/pill/hero assertions | `python3 tests/player_home_polish_134.py` | Source contract only, not GPU/D-pad validation |
| Static panel/Back/subtitle assertions | `python3 tests/player_panel_131_contract.py` | Source contract only; does not run actual remote input |
| Native full Android C syntax | `bash tools/android/preflight-c.sh` | Requires Linux Android NDK + SDL headers; do not use `NUVIO_UI_FAST=1` for final integrated preflight |
| Existing UI/detail checks | `python3 tests/detail_netflix.py`; `python3 tests/native_home_refinements.py` | Static regression checks, no runtime parity |
| Subtitle parser fixtures | `bash tests/addons_legendas.sh` | Test harness includes macOS `-Wl,-dead_strip`, `/opt/homebrew` include paths and `rg`: verify/adapt Linux test environment before running |
| Subtitle Format A fixtures | `bash tests/legendasui.sh` | Also uses macOS `-Wl,-dead_strip`; may fail on Ubuntu for toolchain reasons |
| Further existing checks | `bash tests/stream_parser.sh`; `bash tests/audioescolha.sh`; `bash tests/fileiras.sh` | Inspect fixtures and prerequisites before targeted execution |
| APK gate | `.github/workflows/android-debug.yml` | **NOT AUTHORIZED** until P8; includes expensive native/Rust/P2P stage, caching and actual APK |
| Optional native-only CI | `.github/workflows/netflix-native-preflight.yml` | A GitHub Actions workflow; **also not triggered** during P0 |

**Important fast-gate coverage risk:** `tools/android/preflight-c.sh` explicitly compiles key UI modules (`player.c`, `faixas.c`, `legendasui.c`, `text.c`, `app.c`, `video_android.c`, etc.) but does **not explicitly list** `src/addons.c`, `src/plrui.c`, `src/posplay.c` or `src/video.c`. Android full CMake includes `src/*.c` (subject to target preprocessor guards), so some new errors may otherwise surface only after the expensive dependency stage. Before final APK, add targeted/compatible syntax coverage or source-specific checks as authorized; avoid a false green preflight. The workflow’s normal push trigger is restricted to `docs/ci/trigger-netflix-detail-once.txt` and manual dispatch; documentation commits alone do not satisfy its push path filter.

## 5. Important open issues and scope guards

1. **Android VOD buffering gap** (above) must be fixed and tested in P4/P7; preserve current honest no-fake-progress visual and avoid fake progress when Android reports unknown.
2. **Source-only tests versus physical display:** exact parental typography, glass pill animation, first-frame A/V sync, ARC quality and MEMC cannot be established from repository code or CI alone.
3. **Full detailed specification availability:** GitHub's [master register](PLAYBACK_50_MASTER_SPEC.md) is the authoritative *condensed* Q01–Q50 decision list, but the previously produced **441-line ~8,000-word full specification** has additional acceptance nuance not yet archived in GitHub. Future implementation sessions must not infer missing precision. Preserve and import/reconcile that expanded artifact before implementing an affected uncertain detail. Record ambiguities instead of guessing.
4. **Do not silently reclassify completed work:** #134+ refinements are committed but unbuilt. Some Q01–Q50 behaviors already exist partially; P1–P7 must inspect before changing rather than duplicate or count existing behavior as newly verified.
5. **UI/TV/Android differences:** Android `video_android.c` and other targets' `video.c` can diverge. Test on the actual target backend; don't infer Android behavior from webOS or desktop mocks.
6. **One final integrated build:** Q01–Q50 plus all post-#134 refinements must be built and tested *together* when the owner explicitly authorizes the workflow; no premature APK.

## 6. P0 gate decision and next session

**P0 audit gate: PASS (documentation/source inventory), with known unverified areas and the Android VOD buffer defect recorded.** No feature implementation claims. No tests or builds were run. No secrets or credentials accessed.

**Next authorized task after this checkpoint: P1 only.** In a **fresh chat**, read master spec, work packages, tracker, this baseline, post-#134 handoff; recheck HEAD; trace actual `src/app.c`, `src/player.c`, `src/video_android.c`, `NvPlayer.kt` and source-selection ownership before edits. Implement safe session/source identity, stale callback cancellation, same-source renewals and recovery/background distinction (Q03–04, Q08–09, Q11, Q14, Q19, Q22, Q35, Q48). Carry forward Android VOD buffer gap as a P4/P7 blocker. Run only source-level checks/compilation authorized for P1; **no APK or Actions workflow**. Commit and update tracker; stop at package boundary.

**Working-tree limitation:** GitHub remote API verifies committed branch state, not any uncommitted local clone; future sessions with local checkout must separately run `git status` before editing.
