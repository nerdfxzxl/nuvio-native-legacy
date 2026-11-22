# Nuvio Legacy — Build #134 Handoff (22 November 2026)

## Source and artifact
- Branch: `feature/official-stream-selector`. Do not merge into master without approval.
- Workflow: https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/44987999328
- APK build source: `bad57b2f9c9fecdfd8399372d576834e26406023`
- Artifact: `nuvio-native-legacy-android-debug`, ID `14740439614`
- Download: https://api.github.com/repos/nerdfxzxl/nuvio-native-legacy/actions/artifacts/14740439614/zip
- ZIP size: 79,479,873 bytes. Expires 2026-12-06 05:13:04 UTC.
- This handoff file was committed after triggering the APK; code/artifact correspond to the source SHA above.

## Changes
1. Player transport: `src/player.c`
   - Removed the conditional layout that rendered independent, widely spaced circular buttons when `telefoneui_ativo()`. TV and Android touch now draw the SAME shared centered liquid-glass capsule and each button retains a touch target.
   - The capsule itself stays a fixed, centered width based on max possible label width instead of recentering on every focus animation frame.
   - Button icon is anchored at `r.x + (68 - 30)/2` (constant within each button whether focused or not); label fades and slides into the expanding region on the right, clipped to the pill.
   - Underlying focus spring, D-pad ordering, player controls and icon sources are preserved.
   - Removed touch-only two-row transport layout and touch-only compact mode. On genuinely narrow widths, icons may remain compact, but still share ONE capsule. TV OSD vertical alignment is now shared by mobile.
2. Home hero bottom positioning: `src/home.c`
   - Main Modern hero-focused copy baseline increased from 22 to 82 units; Dynamic hero baseline uses 70 instead of 22 units.
   - The synopsis and View Details action move down as one measured group, retaining internal gaps. Rows/other surfaces retain their prior layout logic.
3. No black hero gradient: `src/gfx.c`, `src/gfx.h`, `src/home.c`
   - New render-scoped `gfx_hero_sem_degrade_atual` flag is true only while drawing the main Home hero and always reset afterward.
   - Existing shared hero shaders (band/full/vitrine/vitrine-din and optimized fused crossfades) use a sentinel uniform to bypass dark veils for Home only. Artwork crop and crossfade plumbing are retained. The trailer overlay's matching dark ramp is also suppressed.
   - Detail-screen gradients, cards and other rendering modes remain unchanged.

## Targeted test strategy requested by owner
- Build #133 passed broad Android graphics, artwork and native checks earlier today.
- Build #134 used a one-shot `[ui-fast]` commit marker on the existing Android debug workflow.
- This fast mode keeps Android ARM64 C syntax compilation of exactly the changed native code (`player.c`, `home.c`, `gfx.c`), shell syntax checks and `tests/player_home_polish_134.py` regression assertions.
- Unchanged artwork conversion, Robolectric graphics, and unrelated stream/audio/detail unit-test suites were skipped for this tagged build, not deleted. Regular builds still run the full preflight.
- The real full Gradle/native APK build still ran with the existing login configuration and cache and uploaded the debug APK.
- Targeted preflight completed successfully; real-device pixel-perfect tests were not performed.

## TV/mobile acceptance checklist
- On phone in landscape, the player transport must appear inside one centered large rounded capsule (no full-width isolated circles).
- On TV, the existing bar/seek/focus behavior must remain functional. Navigate L-to-R and R-to-L; the icon should no longer shift *within* its expanding button; the label should slide in on its right. Neighbor movement caused by animated widths may still occur.
- All buttons must respond to remote OK and mobile touch and present the correct icon and action label.
- On the Home hero focused view, check new synopsis and View Details placement near the actual screen bottom. Verify no overlap with the first poster row, particularly in Dynamic layout.
- Check black-gradient removal for still backdrop and autoplay trailer and confirm the detail page retains its own shading.
- Check a real device for GLES shader compilation. The native C preflight validates shader source strings, not runtime GLSL compilation on every GPU.

## Boundaries and preservation
- Nuvio's existing subtitle/audio refinements and full workflow #133 remain intact; no stream selector, playback engine, audio processor, secrets, or P2P changes in this batch.
- The artifact proves APK compilation and upload, not measured visual behavior on physical Android TV and mobile.
