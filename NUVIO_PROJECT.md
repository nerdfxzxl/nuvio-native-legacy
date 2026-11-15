# Nuvio Personalization Project — Living Specification

Status: planning and recovery; no feature is considered complete without build and device verification.
Primary target: Android TV / Google TV build of Nuvio Native Legacy.
Working branch: `feature/official-stream-selector`. Keep `master` untouched until changes are verified.

## Source projects
- Personal fork: https://github.com/nerdfxzxl/nuvio-native-legacy
- Native Legacy upstream: https://github.com/iqui27/nuvio-native-legacy
- Official NuvioTV reference: https://github.com/NuvioMedia/NuvioTV (TV UI, **not** Nuvio Mobile)
- Search-bar behavior reference: https://github.com/luqmanfadlli/NuvioMobile-Enhanced
- Separate personal TV fork: https://github.com/nerdfxzxl/NuvioTV

## Current priority: stream selector
Rework the Legacy Android TV stream selection experience to closely match the official NuvioTV stream panel visually and in remote-control behavior, while retaining Legacy's performance. Inspect actual source and licenses before reusing code; do not assume Jetpack Compose UI can be pasted into native C/SDL2/GLES2.

### Intended flow
- Unwatched title: short Play opens stream selector in expanded dynamic clock.
- Resume title: short Resume plays the previously selected source.
- Long Play or Resume: opens stream selector in expanded dynamic clock.
- Back: collapses selector to detail page in one predictable navigation action; never opens an error screen or stranded poster.
- Preserve a configurable way to switch between original Legacy selector and new selector, if technically practical.

### Layout and presentation
- Selector is integrated into the dynamic-clock expansion, not Legacy's separate full-backdrop stream page.
- Panel fills available vertical space, rather than a short inset panel.
- Stream cards should match official NuvioTV design, content hierarchy, typography, and focus states as closely as feasible; inspect reference implementation before coding.
- Do not show addon logos. Show addon names in filters.
- Stream/file description should preserve meaningful source metadata.
- Badges that overflow may animate **only on the focused card**; do not animate every card.

### Search
- Search icon/control sits **outside** the stream panel, aligned with its top, to the left.
- On focus, search field expands toward the left with smooth animation and the lens icon moves to the field's left side.
- On blur, collapse without losing the typed query; refocusing restores the query.
- The Android system keyboard opens **only on explicit OK/Select on the expanded search field**, not merely on focus.
- Filter streams immediately as characters are typed; no keyboard confirmation required.
- Search across stream names/descriptions/metadata, including examples like 2160p, Malayalam, DV, IMAX and release-group keywords.
- No preset placeholder text inserted as a search value; use an appropriate English hint if desired.

### Remote focus and addon filters
- Predictable D-pad navigation between search, Reload, All, addon filters and stream cards.
- Reload must be reachable by remote focus.
- Addon filters form a horizontally scrolling strip: show partially visible next item where appropriate, and slide the strip under/alongside the fixed All control as focus moves.
- All addon filters remain visible/rendered when navigating into the strip; never disappear unexpectedly.
- Pressing Left from All must follow an explicitly designed focus path rather than dead-ending.

### Performance
- Remain responsive with many addons and hundreds of streams.
- Avoid expensive per-frame layout, text measurement, allocations, image decoding, or full-list rebuilding where possible.
- Benchmark and inspect Legacy and official NuvioTV implementation before choosing caching, virtualization, and rendering strategies.
- No addon logos or unnecessary thumbnails.
- Focus transitions should feel immediate and scrolling should remain smooth.

## Known reported bugs (unverified on current HEAD)
1. Search control displays unwanted prefilled `Buscar fontes`.
2. Search collapses while text exists; query behavior needs correction.
3. Reload is unreachable by D-pad.
4. Left from All does nothing.
5. With three addons only two are visible; desired partially visible next item and scrolling missing.
6. Addon buttons visually disappear on moving focus from All.
7. Stream cards do not match official NuvioTV.
8. Many streams cause TV lag.
9. Back navigation previously opened `could not open the source` and required multiple presses.
10. Earlier builds had stream selector exiting/returning unexpectedly.
These are reports from prior device testing; verify each against the latest branch before claiming fixed.

## Implementation protocol
1. Inspect branch HEAD, relevant C UI/navigation/rendering files, Android glue, current selector, and official NuvioTV TV implementation.
2. Document exact component/file mapping and feasibility of reuse vs port.
3. Establish a reproducible baseline build/test; record failures and exact error logs.
4. Implement one coherent slice at a time; run static/build tests before committing.
5. Use GitHub Actions Android debug APK workflow for device testing; do not run Tizen builds for Android-only changes.
6. Ask for screenshots only when visual evidence is necessary; prefer logs for build/crash issues.
7. Never claim a bug fixed without test evidence. Keep a rollback commit/branch.
8. Record commit SHA, build run, device results, regressions, and next steps in this file or a linked issue.

## Chat continuity
At the start of each new chat: read this specification, inspect the current working branch and recent commits, and review latest build/test evidence. Do not assume previous chat changes were committed. Do not restart from scratch or re-ask settled requirements. Clarify only genuine contradictions.

## Later work (not current implementation scope)
Simplify settings, preserve desired customizations, improve home/hero/collections and episode selection UI. Define separate specifications before modifying those areas.

## Project journal
- 2026-11-15: Initial living specification created. Requirements and bug list reflect user reports, not verified implementation status. No app source code modified as part of this documentation change.
