# Nuvio Legacy Build #130 — Player panel handoff (2026-11-21)

## Source of truth
- Repository: `nerdfxzxl/nuvio-native-legacy`
- Branch: `feature/official-stream-selector`
- APK workflow source commit: `ba88494e50064f53a4f3199818c05ecc67705dc6`
- GitHub Actions run: https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44924133856
- Commits: `ac98ebf` (audio UI & persistence), `6ae0ae7` (subtitle tabs & sync), `b58dd87` (subtitle source pills), `d5a460b` & `c4b4fb` (headers), `d8c7164` (style editing & font popup), `23e698f` (adjacent-tab focus correction).
- Preserve `master`; do not merge this branch without owner approval.

## Implemented in native C
- Audio Tracks | Adjustment, equal-width centered tabs with single animated underline; no redundant Audio heading.
- Audio controls: Amplification 0..10 dB; Bass -12..+6 dB; Center Mix -10..+30 dB; Night Mode Off/Light/Medium/High; Persist between sessions.
- The minus/plus icons are always present. OK enters +/- focus; Left/Right navigate the controls; OK changes the value; Up/Down exits button focus and advances rows. Individual reset icon for the dB rows.
- Audio track selection stays open; existing original-language / quality-ranked automatic ordering unchanged.
- PCM gain, bass, center-downmix and dynamic-range DSP are existing backend features, not reimplemented. Passthrough still bypasses PCM processing.
- Optional persistence is now a per-profile on/off file; audio parameters only load/save when enabled, avoiding involuntary restoration on every title.
- Subtitles | Style | Sync tabs have centered labels, animated single underline, no large tab focus backgrounds. Focusing another tab never activates it; OK activates it and jumps into content.
- Subtitle source pills: Built-in when available, and each enabled subtitle-capable addon, even if no subtitle was returned. Source pills filter candidates without changing selected subtitle. D-pad Left/Right from subtitle list enters pill strip; OK switches source; Up from pill strip enters main tab bar.
- Opening subtitles or returning from other tab focuses selected subtitle when present.
- Style: Vertical Offset first, then HDR Brightness, Font, Font Size, Bold, Shadow, Shadow Opacity, Shadow Size, Reset. OK enters supported numeric +/-; Up/Down exits the controls without needing a second OK. Font opens inset dark popup, with selected checkmark and separate focus highlight.
- Sync: a 50ms-step Subtitle Delay numeric +/- row, with separately focusable buttons and reset.

## Honest limitations — do not present as complete
- Current underlying `VideoLegendaEstilo` only has eight native position levels; requested continuous -20%..+20% offset in 0.5% steps is **not yet implemented**. This would require extending the backend safely across Android/native embedded subtitle renderers.
- Subtitle font size still uses the underlying Legacy baseline (default 120), not the requested normalized 100% UI baseline.
- HDR Brightness, Shadow Opacity, and Shadow Size appear as **Unavailable** because the existing native subtitle backend does not support them. Do not fabricate values.
- Source pill names use installed addon names; confirm provider matching on device with real subtitle responses.
- Native preflight success does NOT establish visual parity, remote correctness on a physical TV, PCM sound quality, or Android playback health. Owner must test actual TV APK. Check workflow run and artifact before stating build passed.

## Build & regression strategy
- `.github/workflows/android-debug.yml` triggers on edits to `docs/ci/trigger-netflix-detail-once.txt`; caches for Gradle, native Android libraries, FFmpeg and P2P must be kept.
- Three shorter ARM64 preflights #20 (44923318587), #21 (44923663774), #22 (44923910606) completed successfully before the last two-line tab-navigation correction.
- Build #129 was deliberately cancelled by #130 due to workflow concurrency (it was built from an older snapshot). Only run #130 is relevant for acceptance.
- When fixing any future bugs, keep branch, preflight first, then trigger one integration APK build once; do not trigger expensive builds for documentation-only edits.
