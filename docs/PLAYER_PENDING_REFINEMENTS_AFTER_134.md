# Nuvio Legacy — unbuilt panel/hero refinements after #134 (2026-11-22)

## Status
- **COMMITTED ONLY.** No Android APK or GitHub Actions workflow triggered for these changes. Latest built and device-tested artifact remains #134.
- Branch `feature/official-stream-selector`; master untouched. This file is intentionally **not** a build trigger. Do not modify `docs/ci/trigger-netflix-detail-once.txt` until the owner requests the next APK.
- Source-level static presence checks passed for all below. Native compilation, Android GLSL/runtime behavior, playback and precise animation parity **NOT YET VERIFIED** for this unbuilt batch.

## Accepted fixes committed
1. **Official-style parental guide** — `src/plrguia.c`, `src/parental.c`
   - Remove the custom dark backdrop, use a taller prominent white vertical line, larger white title + muted severity typography, wider text/line spacing, near top-left, matching screenshot and official `NuvioMedia/NuvioTV` `dev` `ParentalGuideOverlay.kt`.
   - Preserve its stagger-in / reverse-out choreography. Warnings show Severe before Moderate/Mild with same-level order stable, like the reference screenshot.
   - Exact pixel/animation appearance still requires a TV visual check; this is a native reconstruction, not copied Kotlin Compose.
2. **Landscape mobile hero** — `src/home.c`
   - Root cause: `heroCompactoTelefone` compressed all very-wide mobile landscape screens, reduced artwork and row-rest heights. Compact applies to portrait only; landscape uses immersive TV hero geometry and lower catalogue rows.
3. **Back exits complete panel** — `src/faixas.c`
   - One Back from Tracks/Adjustment, Subtitles/Style/Sync, tab bar, +/- editing or font dropdown now closes the entire sheet rather than stepping back through subpages.
4. **Cleaner English subtitle rows** — `src/legendasui.c`, `src/legendasui.h`
   - Source-mode selected subtitle has checkmark, not redundant `Main` pill.
   - Retain single `EN` badge; remove duplicate `English` and repeated provider, subtitle format/release in the secondary row.
   - Remove common filename extension (.srt/.vtt/.ass/.ssa/.zip/etc). Preserve up to 256 bytes of the filename and UTF-8-safe fallback truncation for narrow legacy API callers. Wrap titles for up to 4 lines inside each existing 102px row.
   - **PENDING OWNER CHOICE:** semantic display format; options A full release filename, B cleaned title + quality metadata, C technical badges/tokens. Opaque auto-generated `subtitle%2F...zip` labels especially need user-approved fallback presentation. Do not hardcode a format before owner chooses.
5. **Font size** — `src/faixas.c`
   - Single adjustment is 3 native units = 2.5% rather than 12 units = 10%. Render the exact value with one decimal.
6. **Held +/- acceleration** — `src/faixas.c`
   - SDL held OK key repeat triggers shared progressive acceleration on Audio dB rows, Subtitle Style numeric rows and Sync delay; repeat is throttled to avoid writing every hardware repeat. Reset controls remain single-action.
   - Actual Android remote support for repeat on center/OK must be verified on device.
7. **True absolute buffering** — `src/video.c`, `src/player.c`, `src/plrui.c`
   - Remove seek-position-relative byte predictions from `video_buffer_fim`; use actual absolute buffer end from backend, preserving last measured end for OSD.
   - If playhead seeks past the buffered endpoint, the unbuffered segment of playback rail is visually subdued; no artificial chapter-like mark.
   - Some media backends provide only one buffered range: accurate disjoint range segments beyond the current absolute endpoint require deeper backend work.
8. **Action pill + exclusive focus** — `src/player.c`
   - The old #134 shell used permanent maximum width, never shrinking. New shell animates between compact resting width and expanded width proportional to active focus springs; includes overlap from outgoing/incoming buttons.
   - The icon row's starting X is fixed rather than recomputed from expanded shell width. Labels still push trailing icons and may move neighbors during focus transition; precise perceived parity requires TV trial.
   - Time/seekbar/Info/skip focus now prevents a stale Play/other button highlight. Focus spring no longer overshoots.
9. **Regression contracts** — `tests/player_home_polish_134.py`, `tests/player_panel_131_contract.py`
   - Updated obsolete fixed-shell and Back assertions to new behavior; extended static checks. Tests were **NOT executed by CI** at owner's request not to build or run workflows.

## Next steps before ANY build
- Owner to choose subtitle title/name presentation format (A/B/C or custom).
- Owner may provide additional refinements. Continue committing changes without triggering build until explicitly instructed.
- Before next artifact: run focused source/native compile regression, then a single APK workflow with existing cache, never unneeded duplicate full workflows.
- Acceptance tests: D-pad focus/Back behavior, true hold-OK repeats, full English filename wrap, seek beyond buffer, no double focus, phone hero in landscape, player pill capsule width/anchored icon motion, parental exact TV visual comparison.
