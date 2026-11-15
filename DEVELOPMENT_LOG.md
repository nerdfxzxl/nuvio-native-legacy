# Development Log and AI Handoff

Last updated: 2026-11-15. Historical notes are USER-REPORTED unless marked VERIFIED. This document is a continuity record, not a claim of successful implementation.

## Current state
- Working repository: `nerdfxzxl/nuvio-native-legacy`
- Branch: `feature/official-stream-selector`
- Current task: recover and accurately implement official NuvioTV-style stream panel inside Legacy's expanding dynamic clock, with native TV remote behavior and fast scrolling.
- VERIFIED: `NUVIO_PROJECT.md` was created in commit `5a44c098fdfd2a43fc5df2c8598db45f19c47d9e` on 2026-11-15.
- Verified in repository: Android debug workflow exists at `.github/workflows/android-debug.yml`; its push trigger includes this feature branch.
- No successful current selector build, screenshot comparison or runtime verification is established in this handoff.

## Historical development timeline (user reports)
1. Initially attempted changes in personal official-TV fork `nerdfxzxl/NuvioTV`, aiming to adapt Legacy-like dynamic home/hero/collection UI. Experienced lag, missing hero, trailer black screen/audio-only, collections black screen, and catalog settings inconsistencies. These are a separate workstream, NOT confirmed bugs of this Legacy selector branch.
2. Login failures in TV/mobile forks were resolved by supplying necessary GitHub Actions secrets. Do not assume secrets exist on every repository; verify names and nonempty status without exposing values.
3. Switched attention to personal Legacy fork to replace the stream-selection experience, with official NuvioTV as the visual reference and mobile-enhanced fork as search behavior reference.
4. Early Legacy builds had missing settings toggle/banner and stream selection could exit unexpectedly.
5. Later builds showed panel and search but remained visually inaccurate and laggy with many streams.
6. Latest reported issues: search shows prefilled 'Buscar fontes', collapses even with typed text, reload unreachable, Left from All dead-ends, addon strip only displays two of three names instead of scrolling/partial next item, addon buttons disappear visually on focus transfer, stream cards unlike official TV app, and heavy-stream lag.
7. Back navigation previously showed 'could not open the source' and required repeated presses. Recheck whether this still reproduces on latest HEAD.
8. User reported a recent build failure. Exact workflow run ID, commit, log and failure cause have NOT been verified; do not guess why it failed.

## Locked user decisions
- Match **official NuvioTV TV** stream panel, not Nuvio Mobile, and do not use addon logos.
- New selector expands within dynamic clock on detail page; avoid the separate poster/backdrop selector page.
- Play on unwatched titles opens sources; Resume short press plays last used source; long press on either opens sources.
- Android system keyboard only after OK on expanded search, never just on focus; live filtering as typed.
- Search field outside panel, top-aligned, expands left on focus; collapse on blur should preserve query.
- Show horizontal addon names with clipped/partially visible next item, smooth focus scrolling, fixed All.
- Stream card badges scroll/animate only when focused, if needed.
- Prefer smooth scrolling and native responsiveness over unnecessary visual effects.
- Original/new selector toggle is desired; verify existing settings state and implementation feasibility.
- User prefers one coherent set of changes before an APK build when practical, but quality gates and early compile checks should prevent large untested failures.

## Still unverified / do not assume
- Exact branch HEAD after documentation commits.
- Existing selector code paths and current feature completeness.
- Cause of latest Android debug workflow failure.
- Whether old toggle exists, is wired, or is absent.
- Whether original NuvioTV selector code can legally/technically be reused.
- Device runtime frame rate or scrolling performance.
- Exact UI dimensions, badges, fonts, and screenshots from official reference.

## Next actions for any AI agent
1. Read `NUVIO_PROJECT.md`, `ARCHITECTURE.md`, this file.
2. Fetch current branch HEAD, commit history, source tree and latest Android debug workflow logs.
3. Inspect Legacy native selector/dynamic-clock/search/remote-navigation code and official NuvioTV TV selector source. Populate verified file paths and functions in `ARCHITECTURE.md`.
4. Identify root causes of latest failed build and current UI defects before editing.
5. Propose the smallest reliable implementation sequence; don't redo previously working features unnecessarily.
6. Implement, test, commit, record run ID and results here. Request TV screenshots only when visual comparison is needed.
7. Keep master untouched; never store login secret values in repository.

## Evidence log
| Date | Action | Evidence | Result |
| --- | --- | --- | --- |
| 2026-11-15 | Initial specification | Commit `5a44c098fdfd2a43fc5df2c8598db45f19c47d9e` | VERIFIED docs only |
| 2026-11-15 | Inspected README, Android interface modes, Android debug workflow | Branch-aware GitHub file reads | VERIFIED architecture/build facts above |
| 2026-11-15 | Historical bug capture | Prior user reports | REPORTED, not retested |

## Communication rules
- Be direct, specific, and transparent about what's verified vs pending.
- No claims of 'exact match', 'zero lag', or 'build fixed' without device/build evidence.
- Explain each change in plain language and report commit SHA, workflow outcome and what user should test.
- Avoid repeating settled questions; ask only when decisions conflict or implementation requires clarification.
