# Nuvio Legacy — Build #131 panel refinements handoff

Date: 2026-11-22
Repository: `nerdfxzxl/nuvio-native-legacy`
Working branch: `feature/official-stream-selector` (do not merge into master).
Reference: official NuvioTV `dev` `ParentalGuideOverlay.kt` for animation timings.

## Source-level fixes implemented

### Subtitles
- Source pills: source-focused OK handling runs **before** Legacy's hold-OK dispatcher, resolving the pill activation bug. Each pill has a pointer focus and click callback.
- Source candidate rows are filtered to a **known English language** with `ling_casa(...,"en")`; unspecified languages are not silently treated as English. Existing addon data outside the UI is not deleted.
- Provider labels prefer addon manifest/display name; common AIOStreams and SubDL installs get concise proper names instead of URL-like identifiers. Unknown URL-like names display `Addon N` rather than exposing URL configuration paths.
- Horizontal pill strip has text-sized pills, clipping, D-pad focus tracking and frame-interpolated scrolling. Focusing never selects; OK or click changes selected source without changing the active subtitle.
- Removed legacy hold-for-options affordance from displayed rows. No source-mode hold submenu, secondary-subtitle action or language drilldown.
- Back exits Subtitles, Style or Sync pages; when editing a slider or font popup, the first Back cancels editing.
- Existing selected subtitle is focused when the list opens or a tab is activated, provided it belongs to the currently visible English source.

### Native subtitle style
- Vertical Offset is saved as half-percent increments (`-40..40` => -20.0%..+20.0%); applied to Legacy's primary subtitle anchor. Positive moves subtitles upward.
- Font Size displays relative to the established 120-unit native baseline: 120 native = 100% shown. Changes in 10%-relative steps are saved exactly, including after switching fonts.
- HDR Brightness 25..100% is applied to app-drawn subtitle text independently from normal HUD dimming when rendering to an HDR output.
- Shadow Size and Shadow Opacity now persist and feed the direct-text and libass-style rendering paths.
- Existing authored ASS override rules remain: a user-touched border/position may override the authored style. Native firmware subtitle tracks can have limitations not shared by Legacy's overlay.
- The controls are visible in the native Style page; OK enters +/- editing, Up/Down directly moves to another row without Back.

### Audio
- The previous Tracks / Adjustment UI is preserved.
- Show All/Show Less is now text-only, without a separate rounded rectangle.

### Player timeline
- Elapsed/total time only moves **above** the seekbar for seek-only/idle OSD. Full player controls, including paused state, keep time below it.
- Progress bar is 8px unfocused / 10px focused; removed thumb/dot and chapter ticks entirely.
- The progress bar retains the last **observed** buffered endpoint for seek-only OSD and clears it when a playback source changes. Missing backend buffer metrics are never fabricated.

### Parental guide
- Removed the Dynamic Clock expansion; this is now a native top-left overlay.
- Official-style timing: 300ms container fade, 400ms accent-line growth, 80ms gap + 200ms fade per warning, five-second hold, reversed warning dismissal (60ms/150ms), line shrink and final fade.
- Category and severity labels use English, with visual warning severity contrast.

## Quality gates
- `tools/android/preflight-c.sh` now syntax-checks parental, ASS, plain-subtitle and Android bridge units, in addition to previously tested player, audio, selector and renderer files.
- `tests/player_panel_131_contract.py` statically verifies prior regressions in source-level routing and appearance decisions. This is **not** a pixel-accurate or TV D-pad acceptance test.
- Native preflight #23 (run 44984697499): passed, before new style changes.
- Native preflight #24 (run 44984914750): passed, including new style fields and renderer units, before final regression tests.
- A full APK workflow and TV verification still need to be reported separately.

## Reality / testing caveats
- Native build success never proves remote/visual parity. Install on actual TV and test English-only source pills with enabled addons, scrolling, buffer display after seek, Subtitle Style on true HDR content, and parental notification chronology.
- Native firmware-rendered subtitles (especially webOS TV native subtitle plane or embedded image tracks) do not expose every fine-grained app-drawn style option. Never claim those pipelines support precise offset or custom shadow.
- Original NuvioTV's Compose animation code provides timings/choreography, but reading code is not the same as watching its animation on a TV.
- No edits to the login secrets, FFmpeg/PCM path, P2P dependencies, caches, build signing or master branch.
