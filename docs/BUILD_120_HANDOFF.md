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
