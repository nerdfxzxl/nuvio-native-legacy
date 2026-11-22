# Nuvio Legacy — Build #133 Verified Handoff

Date: 2026-11-22 UTC
Repository: `nerdfxzxl/nuvio-native-legacy`
Working branch: `feature/official-stream-selector`
GitHub Actions run: https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44985801606
APK source SHA: `f09592932a8e441c9fdc092cd8faff3995da793f`
Outcome: **SUCCESS**, both jobs including upload verified via GitHub Actions.

## Artifact
- Name: `nuvio-native-legacy-android-debug` (ZIP containing Android debug APK)
- GitHub artifact ID: `14740485096`
- Size: 79,487,160 bytes ZIP
- Download API: https://api.github.com/repos/nerdfxzxl/nuvio-native-legacy/actions/artifacts/14740485096/zip
- Expiration: 2026-12-06 04:42:53 UTC
- No physical Android TV playback test has been performed; no visual pixel-parity claim.

## Changes since #130
### Subtitle source selector
1. Fixed dead D-pad OK and click on source pills: source handler now precedes stale Legacy hold-OK logic, and pills get real pointer targets.
2. English-only subtitle tracks: filter using `ling_casa(language, "en")`; addon data outside selector unchanged, unknown-language candidates not silently included.
3. Normalize provider pill display names from installed manifests, special-case recognizable AIOStreams/SubDL labels; never show configuration URLs. Source tabs are variable width with smooth bounded horizontal scroll, independent focus/selection, and clipping.
4. Removed obsolete hold-for-options, secondary-subtitle and language drilldown affordances in this new selector; removed footer descriptions, preserving subtitle counter.
5. Back from Subtitles, Style or Sync closes panel (except first Back exits a focused adjustment or Font popup).

### Subtitle Style backend
1. Vertical Offset: -20%..+20% with 0.5% steps, native `offsetHalfPct` -40..40, applied to app-drawn subtitles (positive upward).
2. Font Size: 100% displayed when existing 120 native unit baseline is loaded. Editable 10%-relative increments, preserved exactly in preference file.
3. HDR Brightness: 25..100% of app-rendered subtitle text output, only in HDR.
4. Shadow Opacity: 0..100%; Shadow Size: 0..14 px; feeds SDL and ASS style, persisted.
5. Original authored ASS styles remain default unless user touches the corresponding override; firmware-owned subtitle renderers may not respect all controls.

### Audio
- Existing full Tracks / Adjustment page from #130 remains. Removed rounded box behind Show All / Show Less; label only.

### Video seekbar
- Thicker slim bar: 8px normal, 10px focused; removed seek thumb/dot and chapter lines.
- Elapsed/duration time above seekbar in seek-only/idle OSD, below seekbar with full controls (playing or paused).
- Preserve last observed buffered fraction for seek-only screen, reset when playback source changes; never fabricate buffer data.

### Parental guide
- Replaced Dynamic Clock expansion with a native top-left warning overlay.
- Mirrors official NuvioTV `dev` `ParentalGuideOverlay.kt` staggered animation: 300ms container, 400ms vertical line, warning labels enter sequentially, 5-second hold, reverse fade out. English category/severity names.

## Checks and workflow failures resolved
- Short ARM64 preflight #25: SUCCESS (run 44985088795), including additional syntax checks of parental, ASS, plain subtitle and Android renderer and `tests/player_panel_131_contract.py`.
- APK workflow #131: FAIL preflight only, Robolectric runtime fetching hit Maven repo1 HTTP 429; native compiled.
- APK workflow #132: FAIL preflight only, Robolectric runtime fetching hit Maven repo.maven.apache.org HTTP 429.
- Fix: `tests/android-card/build.gradle.kts` sets Robolectric's runtime repository to Google's Maven Central mirror (`maven-central.storage-download.googleapis.com/maven2`) following Robolectric's own Nov 2026 fallback. All original Android graphics tests remain enabled.
- APK workflow #133: SUCCESS, native ARM64 compile, artwork validation, Android graphics unit tests, required secrets presence, cached native deps, full Gradle/native build, APK upload.
- Do not change Android native Gradle/FFmpeg/P2P cache keys without technical need.

## Honest limitations and acceptance
- APK presence proves compilation and packaging, not TV D-pad behavior or visual fidelity. Owner should test a known movie with AIOStreams/SubDL English subtitles, D-pad across addon strip and OK, tab closing from all pages, Style sliders on app-drawn subtitles, seek-only/paused time and buffering, and parental guide on a rated title.
- Separate firmware-rendered embedded captions may have different styling capabilities. Android external/overlay subtitles are the primary target of new style controls.
- Reading NuvioTV source establishes structure/timing but does not equate to viewing physical Compose animation.
- Keep master untouched; do not merge feature branch without owner approval.
