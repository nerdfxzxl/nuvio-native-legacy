# Nuvio Legacy — Player customization / Bass EQ Build #127 handoff

Date: 2026-11-21 (UTC)
Repository: nerdfxzxl/nuvio-native-legacy
Working branch: feature/official-stream-selector
Verified APK source SHA: `3febbdf660223c8248239ed6212bbc9eb477b0e4`
Android debug workflow: https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44882012059
Artifact ID: `14699000447` (`nuvio-native-legacy-android-debug`, 79,495,234-byte ZIP)
APK inside artifact: `Nuvio-2.0.8-android-debug.apk` (~82 MB)
Artifact expires: 2026-12-05T07:56:44Z.

## CI validation
- Build #127: EARLY ARM64 preflight SUCCESS, including player, stats HUD, audio, subtitles, artwork, audio language selection tests and Android graphics.
- Build #127: Full Android APK job SUCCESS. `BUILD SUCCESSFUL in 2m 25s`. Artifact uploaded.
- Gradle wrapper/dependencies/transforms cache hits, native Android dependencies cache hit, P2P engine cache hit. Preserve current cache keys/strategies for future work.
- Build #125 failed on five `gfx_cor` invocations in `estatvivoui.c` missing the radius argument; fixed in #126.
- Build #126 expanded the native preflight and caught `audioLinhaDaFaixa` used before declaration in `faixas.c`; fixed in #127. Expanded preflight stays enabled.

## Implemented since build #124
1. Player transport action order: Play/Pause, Next Episode (series), Subtitles, Audio, Episodes, Zoom, Playback Info.
2. Playback Info: compact live metrics HUD and detailed, sectioned panel (Playback, Source, Video, Audio, Network, System) with D-pad scroll and an HUD toggle. All numeric fields derive from actual media/system data when available; unavailable fields show a placeholder.
3. OK Playback Info toggles overlay. Hold OK expands the compact mode to the detailed mode. Back does not dismiss Playback Info: the first press hides only the usual controls, the next exits playback. Info visibility/mode remembered for next session.
4. Press Right on Playback Info to focus the elapsed/total time; OK toggles elapsed/remaining time display.
5. Smooth, bounded underdamped focus spring for the liquid-glass action buttons; reduced-motion policy preserved.
6. Zoom cycles seven modes, skipping extra Ultra mode.
7. Audio Adjustments: separate panel with Amplification 0..+10 dB, Bass -12..+6 dB (110Hz low-shelf PCM filter), Center Channel Mix -10..+30 dB, four rectangular Night Mode buttons Off/Light/Medium/Strong, optional per-profile persistence, per-control reset icons. The existing 0..200% volume control remains in the Audio Tracks screen.
8. Android Media3 PCM audio processor and JNI bridge apply adjustments live; passthrough/bitstream processing remains untouched and is explicitly marked unavailable instead of silently forcing PCM.
9. Audio language grouping: Original, English, Malayalam, Unknown; extra languages inline behind Show All / Show Less. Within each group tracks are ordered highest-quality first. Track selection remains open. Auto-selection prefers known original, English, Malayalam, then stream default, preserving explicit manual selection.
10. Subtitle synchronization uses 50ms D-pad steps (200ms repeat), 3-decimal seconds display and OK reset in the existing subtitle menu.
11. Early CI gate now compiles `estatvivoui.c`, `estatvivo.c`, `faixas.c`, `legendasui.c`, `audioescolha.c`, `cacheboost.c` and runs `tests/audioescolha.sh`.

## Important acceptance limitations / next work
- CI success confirms compilation and an APK artifact, **not** that all interactions, visual alignment, subtitles, audio output or performance have been accepted on a physical Android TV. Perform TV test before claiming final UI approval.
- The original subtitle workflow has Sync/Style options but is NOT YET the fully requested three-tab Subtitles/Style/Sync redesign; subtitle sync delay memory is not yet keyed per selected subtitle. These are open refinement items.
- The rich detailed video telemetry values for frame rate, decoder, output sample rate and similar fields may remain unavailable because backend APIs don't report them; never fabricate metadata.
- The Bass, Center Mix and Night Mode effects use PCM; bypassed during device-level bitstream passthrough. Device testing is needed for audible quality and channel order.
- The native legacy player architecture, playback source selection, cached dependencies and workflow upload path are preserved. No separate Work-mode context is needed to continue.
- Keep this implementation on the feature branch until the owner tests the APK; do not merge into master without instruction.
