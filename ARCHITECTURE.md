# Architecture and Source-Mapping Handoff

Last reviewed: 2026-11-15. Evidence levels: VERIFIED = read directly from current branch; REPORTED = user's testing/history; TODO = must inspect code.

## Repository and targets
- Repository: https://github.com/nerdfxzxl/nuvio-native-legacy
- Working branch: `feature/official-stream-selector`; do not modify `master` without approval.
- VERIFIED from README: Legacy is primarily written in C, uses SDL2 and GLES2 for native TV UI. It targets webOS/Tizen and has Android TV/mobile builds. Do not mistake it for the Kotlin/Compose official NuvioTV implementation.
- VERIFIED from `docs/android/INTERFACE-MODES.md`: TV uses a 1920x1080 logical canvas; mobile adjusts canvas to drawable aspect ratio. Android package ID `space.nuvio.nativelegacy`. Debug signature cannot upgrade a release-signed installation.
- TV reference project: https://github.com/NuvioMedia/NuvioTV ; examine **TV** selector implementation and license.
- Search reference: https://github.com/luqmanfadlli/NuvioMobile-Enhanced ; examine behavior, not assume mobile UI can be directly reused.
- Native Legacy upstream: https://github.com/iqui27/nuvio-native-legacy

## Verified Android debug build workflow
File: `.github/workflows/android-debug.yml`, read on working branch.
- Name: `Android APK (debug)`; manual dispatch and push on `feature/official-stream-selector`.
- Runner ubuntu-24.04, JDK 17, Android SDK platform 35/build-tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1, Rust Android targets.
- Builds native curl/JPEG/WebP libraries with `tools/android/deps.sh` and P2P engine with `tools/p2p-motor/build-android.sh`.
- Requires repository Actions secrets named `NUVIO_SUPABASE_URL`, `NUVIO_SUPABASE_ANON_KEY`, `TV_LOGIN_WEB_BASE_URL`; checks nonempty values and writes restricted `local.properties` during build.
- NEVER print, commit, or copy actual secret values to docs, issues, or logs.
- Confirm remaining workflow steps, output artifact name, and current run status before triggering builds.
- Do not run Samsung TPK/webOS jobs for Android-only work.
- Verify any APK installation/signature conflict before asking user to uninstall; avoid unnecessary loss of app data.

## Source map — investigation required
The following are NOT verified function/file mappings. Do not invent names.
| Concern | Next inspection task | Evidence status |
| --- | --- | --- |
| Dynamic clock expansion | Find native render/state transitions for clock and detail page | TODO |
| Old stream selection screen | Find entry points, list renderer, card layout and navigation | TODO |
| Play/Resume short/long actions | Trace input event routing and resume-source behavior | TODO |
| Search text input | Trace Android IME bridge and focus/collapse state | TODO |
| Addon filter strip | Trace data model, clipping/scissor and D-pad focus | TODO |
| Stream filtering | Identify data ownership, metadata fields and list rebuild behavior | TODO |
| Back behavior | Trace navigation stack and error overlay transitions | TODO |
| Performance | Profile text measurement, render loops, allocations, filtering, scrolling | TODO |
| Original selector toggle | Locate persisted appearance/feature settings | TODO |
| Official TV selector | Map NuvioTV Compose/components to behavior and visual tokens | TODO |

## Porting decision
Native C/SDL2/GLES2 and Android Kotlin/Compose cannot share UI code by direct copy/paste. Reuse algorithms/data concepts where licenses permit; translate UI rendering, focus and animation behavior to the native framework. Before implementation, inspect source licenses and create a side-by-side specification of card geometry, spacing, colors, badges, text, focus, virtualization and scrolling.

## Validation requirements
1. Inspect exact working branch HEAD and changed files before edits.
2. Establish baseline build with workflow; record run ID, URL, conclusion, commit SHA and artifact.
3. Separate compile errors from runtime crashes and visual mismatches.
4. Test remote focus with three-plus addons, hundreds of streams, live typing, keyboard opening, reload, back navigation, play/resume.
5. Use actual TV screenshots or recordings for visual comparisons. User reports are evidence of symptoms, not proof of cause.
6. Keep reversible commits and document each test. Never say 'fixed' merely because a commit was pushed.

## Pending inspection
The code search tool returned no matches for broad terms 'stream selector' and 'dynamic clock' on the default branch. This is NOT proof the functionality is absent: searches may miss branch-only changes or localized identifiers. Inspect the working branch's source tree directly with branch-aware file retrieval or repository checkout before mapping functions.
