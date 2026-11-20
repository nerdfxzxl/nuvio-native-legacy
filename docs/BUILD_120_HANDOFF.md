# Build 120 handoff

Continue Nuvio Legacy Build #120 from the latest commit on `feature/official-stream-selector`. Read `docs/BUILD_120_HANDOFF.md` first. Do not restart the project or repeat completed work. Implement remaining refinements, run the required checks, and continue the APK build process.

## Baseline and build

The starting branch HEAD is exactly Build 119, 1b29dab675d80fc0bb2cc52aa06fcb28ee3479ae. [Successful baseline run 44570475296](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44570475296). Official dev reference ff7fbd747955a1a6108ac23c296b79ad851a14b1. Build 120 full APK requested. See the latest build checkpoint below. Use `git rev-parse HEAD` for the latest checkpoint including this document. The document cannot contain its own commit hash.

## Status

| # | Refinement | Work on this branch since 119 |
|---|---|---|
|1|Single artwork and framing|Implemented; host syntax and source guards passed. TV pending.|
|2|Interruptible focus and neighbor motion|Implemented; host syntax and source guards passed. TV pending.|
|3|Smooth or instant preference|Implemented; host syntax and source guards passed. TV pending.|
|4|Back / Hero Down|Implemented in native code; device validation pending|
|5|Fullscreen Hero|Implemented in native code; device validation pending|
|6|Multi-catalogue Hero rotation|Implemented; device timing and relaunch pending|
|7|Fast Left protection|Implemented in native code; device validation pending|
|8|Short / long OK|Implemented in native code; device validation pending|
|9|Resume pill track|Implemented in native code; device validation pending|
|10|Continue Watching limits|Row-specific caps removed in native discovery, provider ingestion, progress storage and rendering. 620-record reload and 96-item provider checks passed. TV performance pending.|
|11|Row counters|Implemented in native code; device validation pending|
|12|Per-row per-style size editor|Implemented; persistence and cancel tested, TV interaction pending|
|13|Shared poster bounds|Implemented; native compile and source guards passed. Actual edge alignment, rapid reversal and late-art blending need TV testing.|
|14|Persistent Home artwork cache|Implemented; native budget, trim and clear tests passed. Offline TV startup and 4K memory measurements pending|

## Execution notes

Use the status table above and the newest checkpoint below. Run the native ARM64 preflight before the full APK workflow. The full workflow also runs the Android storage unit tests. No physical TV is attached to this session.

### Poster focus checkpoint

Changed src/home.c, src/ajustes.c, src/ajustes.h, src/ajustes_ux_padrao.inc, src/ajustes_ux_tela.inc and tests/native_home_refinements.py. The shared artwork uses the existing decode-time saliency and centered fallback. Title overlays are suppressed on identified poster fallbacks and styles with their own title renderer. Embedded provider text in arbitrary backdrops cannot be identified from metadata alone. Interrupted expansions retain their displayed widths. The new named preference uses Legacy's existing persistent settings table. Host GCC syntax check for Home/settings, native_home_refinements.py and ajustes_secoes.sh passed. Handoff baseline commit 726729c1b3d530f3fb9350cabedac9856aec4c4d. No APK or physical verification yet.

### Navigation, input, Resume and shared bounds

Previous implementation commit ac24e9c9e9e4874150446ffef50bb5aaba05db70. Changed src/home.c, src/app.c and src/detail.c. Down from Hero resolves the first navigable row, resetting its column. TV Hero scroll targets move the shelf outside the viewport. Removed current/total headings. The central router consumes held-Left sidebar requests after component navigation. Home classifies OK on release or at hold threshold; opening options supplies the long-press feedback. Resume uses a band clipped to the original pill silhouette and its stack clip. Detail now traverses actual bounds on a reversible 380 ms timeline; removed the competing Home shelf offset. Source-art fallback stays on screen if the backdrop is missing. Still verify close after catalogue movement and late-art blending. Passed host GCC syntax checks for all three files, tests/detail_netflix.py, tests/native_home_refinements.py, and git diff --check. No Android build yet.

### Hero selection and rotation

Previous checkpoint 283ed53cfe6c7097d213d9fdfb4af009f220cb6f. Changed src/ajustes.c, src/ajustes_ux_ilha.inc, src/fileiras.c, src/fileiras.h, src/descoberta.c, src/home.c and src/layout.h. The existing picker toggles any registered catalogue using profile-specific named exceptions and Select All includes future rows. A session-seeded round robin samples distinct titles from each source. The discovery worker fills absent sources using metadata, with up to ten nonempty supplemental feeds. Only the upcoming Hero artwork is warmed. Manual navigation wraps; the six-second timer respects the existing playing-trailer gate. An empty selection falls back to available titles. A new launch reshuffles; there is no cross-launch recent-title blacklist. Passed host compiler checks for affected translation units, fileiras.sh and herocat_regras.sh. No APK yet.

### Row Style editor

Previous commit d7295a425fac478458279259c0da853652d1b916. Changed src/ctxmenu.c, src/fileiras.c/.h, src/home.c, src/ajustes.h, src/ajustes_ux_desenho.inc, src/ajustes_ux_tela.inc and the corresponding settings/Home/row tests. Long OK on a resizable style opens the numeric glass panel with a live, memory-only preview. Left/Right adjust one dp; Up/Down focuses reset, OK activates it or saves, Back cancels. Sizes are stored by catalogue key and style family within the active profile; legacy variant IDs remain readable. Removed the three global controls and S/M/L selectors. Host compilation, settings coverage and source guards passed. Real on-disk reload, A=185/B=145, style round trip, cancel, Select All exceptions and new-catalogue selection passed in tests/fileiras.sh under ASan/UBSan (LeakSanitizer unavailable under ptrace). Device UI remains untested.

### Artwork cache checkpoint

Previous commit 16549eb8476faf5454e748e0bc94ab9550272d79. Changed src/tex_cache.c, cachearte.c, androidcache.c, perfiltv.c, home.c, docs/cache-capas.md and tests/android_storage.c. The existing worker pipeline reads compressed images on disk before network and bounds decoding/upload. Fixed double eviction by the general Android cache. Android artwork budget now uses up to 250 MiB, limited by free space. Disk hits preserve fetch age for idle background revalidation. Fullscreen playback suppresses Home prefetch. Media3 seek caching is untouched. Host compiler checked the five native units. cache-contrato, native LRU, explicit clear, durable file and APK staging tests passed. Kotlin cleanup test could not start locally because its Gradle jars are absent. TV launch latency, offline availability with cached metadata, and playback memory use still need measurement.

### Shared-element completion

Previous checkpoint 11eb24e2fe686cd33599631dfcd3f36243893167. src/app.c, detail.c/.h and home.c/.h distinguish Home launches, convert physical bounds into the Detail viewport, revalidate the returning title by ID, and reuse a ready texture variant and the cached horizontal subject bias. A late backdrop has its own 180 ms fade. Source guards and native compiler passed. An unavailable return target with a changed identity closes in place. TV frames have not been captured.

### Long histories and integrated preflight

Previous implementation commit 9c9ea928d9092abf5717bac4a616f5a0b076c546. Changed src/home.c, descoberta.c, catalogo.c, cwordem.c/.h, js.h, progresso.c/.h, simkl.c/.h and trakt.c/.h. Discovered provider metadata determines allocations. Continue Watching, future episodes and the optimistic refresh preserve complete index lists. Home recycles visible animation slots and visits nearby cards for render/residency/prefetch. The on-disk progress store grows on demand and preserves old records. Global native catalogue capacity, free memory and the amount served by a provider still apply. Dynamic allocation failure during history assembly keeps the published row.

Host syntax checks passed for all changed history translation units. tests/progresso.c round-trips 620 distinct records. tests/simkl_cw.c long-history passes 96 paused movies plus four suggestions. The standard Simkl suite reached a separate profile stats assertion. Some older test scripts assume Darwin link flags or contain duplicate stubs. The local phone pairing test could not open its listener. The Android build's preflight runs that test again on a GitHub runner. tools/android/preflight-c.sh now checks all touched native translation units. No Kotlin source or Media3 cache changes. Next: monitor the scoped no-APK preflight, fix any actual failure, trigger the full debug APK workflow once green, verify the artifact and record both run links. No user decision pending.

### Full APK requested

Latest preflighted implementation 9a60c5ad1a70589e35b8f04f6f1bd42c3cf7fe9a. [No-APK ARM64 run 44674369302](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44674369302) passed all native compiler, artwork staging and settings integration steps. Final adjustments in src/home.c coordinate displacement on both sides of an interior poster, move the See All tile with its neighbor, share a title-overlay decision between label painters, and preserve a complete ten-title Hero rotation by source and title ID across catalogue refresh. Home host syntax and both Home/Detail source checks passed. The full APK workflow repeats preflight on its exact build SHA. Next: inspect the Android APK (debug) run for this trigger, monitor to completion, diagnose a failed step from its own logs if needed, then record the verified artifact or blocker here.

### Run 120 and runtime checks

Build SHA 708e2aa6e609514fff1f13e69f79f52a7d4c521f. [Android APK (debug) #120, run 44674773466](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44674773466) started on the requested branch. Monitor this run before dispatching another.

Repeatable Linux host checks in tests/build120_native.sh compile real Home, Detail and discovery functions with mock inputs. Passed 101 forward and reverse rectangle samples at three interface scales and three card positions; ten unique Hero titles split 3/3/4 from three catalogues and retained their order after metadata reordering; 620 history indices recycled animation slots safely; a 122-record provider feed deduplicated and published all 121 unique titles in watch order. Additional host checks passed for 620 persisted records under ASan/UBSan, the 96-paused-title Simkl fixture, row/style storage under ASan/UBSan, stream parsing, badges, and settings coverage. The actual Resume draw block's clip calls were exercised at 0%, 1%, 50%, 99% and 100%, three label widths and three scales. These checks exercise code and geometry. No video, TV, GPU pixel capture or 4K measurements were available. The separate Simkl profile-stat assertion was reproduced on the unmodified Build 119 source.

### TV acceptance pass

1. On Home, focus the first, middle and last portrait in a row. Rapidly reverse Left/Right. Check spacing, face/title framing, and duplicate logos. Switch Smooth to Instant and restart. Compare end positions.
2. From mid-row press Back twice, then Down. Confirm first poster, fullscreen Hero, then first visible row. Hold Left into the boundary, release and press again. Repeat on a physical remote and a phone remote.
3. Pick three Hero catalogues, observe ten titles for six seconds each with the trailer idle, wrap manually both ways, revisit Home, then relaunch. Try Select All, a newly added catalogue, Clear All and an unavailable catalogue.
4. Short OK and phone double-tap should open Detail once without a hold flash. Long OK and double-tap-and-hold should open poster options once. Open and close Detail quickly from several scrolled positions and text scales, including a slow artwork load. Check all four edges on return and the selector and trailer Back path.
5. Set Landscape in row A to 185dp and row B to 145dp. Switch A to Posters and back, cancel a change, then relaunch. Inspect a short Resume label and Resume S1E5 at low, partial and almost-complete progress. Check for protruding ends and row counters.
6. Scroll past the 40th Continue Watching title; verify order, focus and remote repeat. Warm Home online, force-stop without clearing app data, relaunch offline, then play the same 4K clip before and after. Compare ready artwork, dropped frames and Android memory measurements on the same TV.

### Build 120 retry

[Attempt 1 of run 44674773466](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44674773466/attempts/1) passed the full preflight, phone pairing test, asset validation and Android graphics tests. The APK job stopped during Gradle project configuration. Maven Central returned HTTP 429 for published Kotlin dependencies including kotlin-reflect 1.9.23. The exact POM subsequently returned HTTP 200. [Attempt 2](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44674773466/attempts/2) reruns the failed job on a fresh runner at the same build SHA. The previous test/handoff commit is deb3257d0ea9be99eea7d991e037b9108c21ab52. If resuming now, inspect the latest attempt and artifact first.


### Post-120 refinements checkpoint — 2026-11-20

After successful APK #120 (SHA 708e2aa6e609), 18 focused commits were pushed through preflight SHA 59c1ec20e04d. Native-only preflight #14, run 44693847068, succeeded on that exact SHA. No later APK was triggered at this checkpoint.

Included source changes: corrected poster flight coordinate conversion to handle noncentral cards; fixed the reverse target once on Back; extended open/close transition timelines with quintic easing; separated production facts from the Quotes panel while leaving them in Details; removed stray synthetic cursor during profile/Android resume; fixed style activation on entering the live row-size editor and restored warm gray/gold track, 72 / default 126 / 200 scale labels and separate hint; reintroduced Dynamic Clock native hold-progress feedback; decoded Wikiquote HTML entities; added cached portrait saliency/focal data for all forced-cover portraits; localized empty/comment-loading diagnostic copy.

The title-review comment **content** is user-generated and may still be in other languages: existing src/extras.c sorts by the interface language and src/detail.c displays language tags; diagnostic-string localization is NOT a translation or proof that comments will be in English. Avoid claiming this part is solved without verifying its UX or changing comment selection. User must test animation alignment, clipping, cursor disappearance, comment language, framing and editor behavior on a TV. Native preflight only checks code and assets; it is not a runtime GPU test.

Workflow .github/workflows/android-debug.yml already has Gradle setup caching, the keyed native Android source/prefix/Media3-FFmpeg cache, and a separately restored/saved P2P engine cache. Build #120 logs show native ~100 MB cache hit and P2P ~71 MB cache hit, skipping P2P compilation. None of the post-120 changes modified cache key inputs. Do not invalidate these caches just to retrigger the build. Launch the normal single full APK workflow by updating docs/ci/trigger-netflix-detail-once.txt, which triggers the mandatory Android preflight first. Then confirm full build and artifact on that trigger SHA, or record an exact failure.


### Build 121 — verified green (post-120 correction APK)

- Workflow: [Android APK (debug) #121](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44705517243).
- Built exact source SHA: `84196e271ee954ab8e6a0dd969dbf81ac6d79b85`.
- Both early preflight and Android debug build jobs succeeded on 2026-11-20. Gradle reported `BUILD SUCCESSFUL in 4m 6s` and the final APK path `build/android/Nuvio-2.0.8-android-debug.apk` (about 82 MB).
- Artifact: [nuvio-native-legacy-android-debug, ID 14614982627](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44705517243/artifacts/14614982627), uploaded successfully; GitHub artifact expiry 2026-12-04.
- Real cache log confirms `Cache hit for: native-android-r27c-v1-c11d5d6...` and `Cache hit for: p2p-r27c-v1-54d2a5...`, both restored. P2P build skipped. Reuse preserved; don't change dependency hash keys without reason.
- Source construction is verified, but physical TV/phone visual behavior and review-language preference remain acceptance items. Keep post-120 fixes in place. No additional APK build is needed until TV test evidence identifies a regression.


### Build 122 corrections — after physical Build 121 TV test

The owner reported that #121 did **not** fix row sizing, poster-hold feedback, or all poster crops. The TV test supersedes CI syntax success. New requirements: enlarge the full-screen Netflix episode stills and S#E# pill, darken its glass, and keep the selected season's white background when focus moves into episodes.

Implementation sequence (on this same branch):
- `src/ctxmenu.c`: fixed the page-3 live size editor reverting the row style in `ctx_atualizar` (it previously called `estRestaurar` whenever `pagina!=1`, including the active slider). Selected style is now applied before resize, and OK explicitly commits style + exact row/style size and updates the committed context-menu origin so Back cannot silently undo it. Restored a visible normal-weight `Hold for options` hint in its own footer.
- `src/fileiras.c`: removed the incorrect `styleFamily` aliasing between independent choices, including Service vs Feature/Collection and larger/smaller variants; each `styleDp[t]` now belongs to the exact style `t` within the exact per-profile row key. Existing per-row local-file serialization and reload retained.
- `src/home.c` and `src/ilha.c/.h`: removed Build 121's special poster fill over the resting clock pill. Poster long OK now feeds the **existing** `ilha_atividade` feedback with the same 200ms delay and progress track as the primary Play hold in `src/detail.c`. No new clock animation renderer.
- `src/detail_episodes_netflix.inc`: 16:9 still grows to 560x315 at 1920, responsiveness preserved on narrower viewports, step increases from 336 to 380, S#E# text is larger with a 42px dark glass badge. The active season remains white whether focus is in season or episodes column.
- `tests/fileiras.c` now tests persistent A/B row size and independent per-style variants, and `tests/detail_netflix.py` adds guards for the editor, island, and episode UI. The cheap native preflight and full APK mandatory early gate now run both suites.
- Cropping research (not yet installed in this APK): libvips attention/entropy crops improve traditional saliency, but near-perfect framing needs face detection (Google ML Kit) plus object detection (MediaPipe) weighted against readable poster artwork/title regions. Integrate only as **offline/asynchronous once per unique artwork** with cached focal coordinates, never per frame; assess native/Java bridge, model size, fallback and TV CPU before adopting. No claim that current heuristics are nearly perfect.

Next: run native-only #15-style preflight via `docs/ci/trigger-netflix-native-preflight.txt` and inspect tests; fix any failures. Then trigger **one** full Android debug APK using existing `docs/ci/trigger-netflix-detail-once.txt` with cache intact. Confirm the artifact, record the SHA and run, and get physical TV acceptance before claiming visual correctness.


### Build 122 — successful physical-TV acceptance candidate (not yet visually verified)

- Native-only preflight #15: [run 44713443081](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44713443081), PASSED. Source revision `c5eb0eadf4540bbc616c53d6a81bf111650de495`. Logs explicitly show `PASS: Netflix detail refinements, row editor, poster-hold clock and episodes`, and `fileiras: tudo ok` including per-row and per-style saved-size reload.
- Full debug workflow #122: [run 44713786013](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44713786013), build source SHA **`3fd30b65f3f8a3231570ca7360bed07c1aff4818`**. Attempt 1 failed in unrelated `tests/android-card` Gradle dependency fetching due Maven Central HTTP 429 responses; full build was skipped. Attempt 2 re-ran failed jobs at the **same source SHA**: native/Android graphics preflight **PASSED**, full APK **SUCCEEDED**, `BUILD SUCCESSFUL in 5m 39s`.
- Artifact `nuvio-native-legacy-android-debug`, ID **14618669769**: [download ZIP](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44713786013/artifacts/14618669769). Inside `Nuvio-2.0.8-android-debug.apk`, about 82 MB. GitHub artifact expires on 2026-12-04. Both `native-android-r27c` and `p2p-r27c` caches were restored as exact HITS, P2P compilation SKIPPED. Do **not** rerun APK just to create another copy.
- The source code updates in Build 122 are those recorded above (row size transaction, exact per-style isolation, existing Play hold animation, Netflix episodes sizing + persistent season highlight). **Device acceptance pending**. Specifically test preview left/right, OK saved size, switching styles, two independently configured rows, app restart; verify normal Play and poster-hold clocks match; episode 560x315 crop, bigger dark S#E# badge and active Season remains white while an episode has focus. The previous Build 121 CI was green but visual bugs remained, so do not mark fixes as TV-passed.
- CI-only followup commit `0e75d7cdf1807d3bcb39fc75742d6ff60bdb26da`: `.github/workflows/android-debug.yml` sets `cache-read-only: false` on both Gradle setup steps so long-lived non-default feature branch can WRITE Gradle caches after successful runs, reducing repeated Maven downloads (the action defaults read-only on non-default branches). This commit is **after** Build 122, does not change compiled app code, and does not trigger another APK build.
- Remaining artwork enhancement proposal (not included in #122): offline ML Kit face bounding boxes + MediaPipe object detector for subject-aware crop, with title-overlay and multi-subject handling, fallback to the existing saliency result, and persistent per-artwork focal result. Benchmark APK size, GPU and cold-start; do not integrate untested dependencies into the visual fixes build.


### Build 123 — D-pad acceleration and moderate row-style maxima (2026-11-20)

The owner tested physical Build #122 and confirmed the existing row/style size system, Dynamic Clock poster hold, and Netflix episodes changes now work. Two follow-ups were requested and approved for Build #123:

1. Row-size editor must respond with exactly **1 dp per short Left/Right press**, then accelerate smoothly after 350ms of continuous hold, reaching up to roughly 230 dp/sec after another 1100ms. The speed uses per-frame SDL elapsed time, rather than the remote's varying auto-repeat rate; releasing the held direction stops immediately, and changing direction resets the acceleration. Old 126 dp default and per-row per-style independence are preserved. `src/ctxmenu.c` owns the held gesture, and new static regression checks guard the transition and release.
2. Moderate per-style slider maxima: regular portrait 300, large portrait 340, compact landscape 280, wide banners 260, and square/4:3 300 dp. Dynamic slider endpoint and default marker. `src/fileiras.c` defines maxima and validates bounds on preview, save and reload; local `styleDp` changed from 8-bit to `unsigned short` because 8-bit values wrap above 255. Old on-disk text remains compatible; the `styleDp <type> <dp> <row-key>` file format did not change. Additional `tests/fileiras.c` tests save 280/290/315/340, reject invalid 341/261, reload, and verify distinct rows and styles.

Native preflight #16: [workflow 44719990267](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44719990267), source `d4cceee7e436bcdc3d4942ed23a52869b5b45b1f`; results to record after completion. Trigger one `Android APK (debug)` full build only when native gate is green. Preserve existing Gradle/native/p2p/FFmpeg caches; no dependency key changes.


### Build 123 — successful cached debug APK (2026-11-20)

- Native-only preflight **#16 PASSED**: [run 44719990267](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44719990267).
- Full Android debug APK workflow **#123 PASSED**: [run 44720324672](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44720324672). Exact APK source commit **`8bb3c7ca0a1c5d407456586c64dea3d9be090de8`** (the branch may advance with docs-only commits afterward). Mandatory early Android C/assets and graphics validation passed and full APK passed with `BUILD SUCCESSFUL in 3m 26s` for main Gradle assembly; `build/android/Nuvio-2.0.8-android-debug.apk`, about 82 MB.
- Verified artifact: [nuvio-native-legacy-android-debug #14621946949](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44720324672/artifacts/14621946949), about 79.46 MB compressed, expires 2026-12-04.
- GitHub job logs confirm multiple Gradle cache hits (dependency, wrapper, transformed artifacts), **native-android-r27c cache HIT**, **p2p-r27c cache HIT**, and P2P compilation skipped. No need for another APK rebuild.
- Owner acceptance testing PENDING: short tap must move 1dp, >350ms hold must accelerate and reverse/release reset; highest range and labels must match selected style; saving 300/340 must survive relaunch without overwriting another row or style. Successful CI cannot guarantee TV input feel or avoid visual clipping for very large styles.


### Build 124 — three Home/episode refinements (Nov 20 2026)

Owner tested #123 and supplied two screenshots. Requests:
1. Remove the broad black lower-half fade under Home poster title artwork, particularly landscape/editorial row cards, without removing title logos. `src/home.c` now omits the baked-in landscape, editorial and expanded-card dark veils in shader/drawing; Continue Watching's own overlay remains; large portrait's separate synopsis-specific tinted veil is untouched.
2. Add two **independent** settings under `Posters and trailers → Poster information`: `Genre on posters` (new append-only ID `AJ_POSTER_GENERO`, key `posterGenreLocal`, On default), and `IMDb rating on posters` (the pre-existing `AJ_NOTAS_HOME` moved from Home Rows; its saved key and value remain unchanged). The Home card renderers now consult these switches directly, independently of the Hero-only genre choice. Changes are in `src/ajustes.c/.h`, `src/ajustes_ux_padrao.inc`, `src/ajustes_ux_tela.inc`, `src/home.c`.
3. Remove the empty top band and thin slice of the previous episode thumbnail above the Netflix episode list, without changing the left logo or season column. `src/detail_episodes_netflix.inc` defines `DET_EP_ROWS_TOP 48.0f`, `DET_EP_FOCUS_TOP_GAP 36.0f`, and calculates the right column scroll/clip from these. The `logotitulo_desenhar` coordinates and left season list are preserved.

`tests/detail_netflix.py` now guards settings uniqueness, persistence, defaults, Home genre/IMDb rendering and episode logo/scroll constants. Native ARM64 preflight **#17 PASSED**: [run 44727340640](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44727340640) on `f5e16bca5eca77c149411d0b1212f303c045b3c9`. Logs confirm static tests and fileiras tests passed. The full debug APK (Build #124) must still run its mandatory preflight. The physical TV appearance and potential metadata contrast over bright art need testing before claiming visual acceptance; moving to Player customization follows owner review. Preserve current native/P2P/Gradle cache keys. 
