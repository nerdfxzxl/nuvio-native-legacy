# Build 120 handoff

Continue Nuvio Legacy Build #120 from the latest commit on `feature/official-stream-selector`. Read `docs/BUILD_120_HANDOFF.md` first. Do not restart the project or repeat completed work. Implement remaining refinements, run the required checks, and continue the APK build process.

## Baseline and build

The starting branch HEAD is exactly Build 119, 1b29dab675d80fc0bb2cc52aa06fcb28ee3479ae. [Successful baseline run 44570475296](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44570475296). Official dev reference ff7fbd747955a1a6108ac23c296b79ad851a14b1. No Build 120 APK or run yet. Use `git rev-parse HEAD` for the latest checkpoint including this document. The document cannot contain its own commit hash.

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
|10|Continue Watching limits|Not started|
|11|Row counters|Implemented in native code; device validation pending|
|12|Per-row per-style size editor|Implemented; persistence and cancel tested, TV interaction pending|
|13|Shared poster bounds|Partial: bounds timing and return motion changed; stale target and late artwork validation pending|
|14|Persistent Home artwork cache|Implemented; native budget, trim and clear tests passed. Offline TV startup and 4K memory measurements pending|

## Execution notes

Initial files changed: this document only. Verified branch ancestry and workflow triggers. No implementation tests or TV tests yet. The reported Build 119 visual defects remain open. No clarification pending.

Start with src/home.c, src/detail.c and the native regression guards in tests/native_home_refinements.py. The latter encode Build 119's superseded two-artwork and global-size behavior. Trace geometry, capture actual drawn bounds, and update only guards superseded by new requirements. Then implement Home/Hero navigation and rotation, input and Resume, row editor, and artwork cache. Inspect the existing native cache before changing it. Leave Android playback seek cache intact. For each group record changed paths, actual checks and remaining device validation. Run tools/android/preflight-c.sh in the NDK environment. The scoped no-APK and APK workflows watch docs/ci/trigger-netflix-native-preflight.txt and docs/ci/trigger-netflix-detail-once.txt respectively. Only change the full-build trigger when integrated checks pass. Inspect jobs, fix concrete failures, verify artifact for the exact built SHA.

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
