# Nuvio Legacy — Q01–Q50 Work Packages and Execution Protocol

**Status: planning only, no implementation or build authorized.** Authoritative decisions: [PLAYBACK_50_MASTER_SPEC.md](PLAYBACK_50_MASTER_SPEC.md). Existing unbuilt work: [PLAYER_PENDING_REFINEMENTS_AFTER_134.md](PLAYER_PENDING_REFINEMENTS_AFTER_134.md). Live status: [PLAYBACK_50_IMPLEMENTATION_PROGRESS.md](PLAYBACK_50_IMPLEMENTATION_PROGRESS.md).

## Operating model: Option A (GitHub checkpoints + fresh chats)

- Use **one bounded package per chat**. The assigned chat should execute all steps within its package without routine “continue” prompts, but should **stop cleanly at its package boundary** after tests, commits and tracker update. Do not start the next package in that session.
- Packages can be split into subpackages if scope exceeds the execution budget; record split before editing and keep ownership clear. Never run an indefinite single-task loop or leave uncommitted work as the only copy.
- Start every session by reading the three linked docs **and** the post-#134 handoff, checking latest branch HEAD and working tree/remote status. Read actual source before modifying; do not assume old line numbers or previous session results.
- One writer per branch at a time; never have two chats simultaneously changing the same branch. Before each write verify current head and avoid overwriting newer commits.
- Implement and test only after owner separately says to begin implementation. Source-level tests and targeted native compilation are allowed *after* implementation authorization; **no APK/GitHub Actions run** without explicit build authorization.
- Commit cohesive work frequently. For each requirement record code paths, commit SHAs, checks executed and results, risks, incomplete tests and the exact next action. Never mark TV verified without actual device feedback.
- If blocked or nearing tool/context limits, checkpoint completed work and write a precise handoff; do not falsely claim package completion. Do not initiate unrelated changes.
- Keep the existing feature branch and native architecture; no rebase/reset, force-push, merging into master, large refactor, alternate player engine or workflow trigger without explicit authorization.

## Packages and gates

### P0 — Baseline preservation and reproducibility
**Q coverage:** cross-cutting, no feature coding.
Verify current HEAD, read unbuilt #134 handoff and tests, inventory actual modules and reference implementations, establish regression baseline and test command catalog. Record known build #134 limitations and protected files/behaviors. **Gate:** baseline inventory + explicit scope map; do not build.

### P1 — Playback state, source identity, cancellation and lifecycle skeleton
**Primary:** Q03–04, Q08–09, Q11, Q14, Q19, Q22, Q35, Q48.
Introduce safe session generations/source identity and stale-callback cancellation using existing player architecture; distinguish background versus recovery events; preserve same-source URL renewal. **Gate:** targeted state-machine/cancellation checks and no unintended source selection.

### P2 — Cinematic loading, spinner and telemetry
**Primary:** Q01–02, Q05–07, Q10, Q12–13, Q15–18, Q21, Q23–29, Q34.
Shared native NuvioTV-style arc throughout app; static logo, real statuses, honest HTTP/P2P metrics, reduced motion, delayed Change Stream, both distinct ~200ms fades, first-frame A/V sync. **Gate:** no fake progress/speed, correct startup/rebuffer/recovery distinction, no blocking input.

### P3 — Dynamic Clock and stream-selection robustness
**Primary:** Q08–09, Q11, Q19–20, Q26–27, Q30–35.
Preserve existing selector while adding progressive addon results, identity-based focus/reload, no-results, no stale cards, URL freshness and manual escape; integrate P1/P2 safely. **Gate:** manual source ownership and selector focus/navigation regressions pass.

### P4 — RAM buffer, HTTP range networking, disk cache and seeks
**Primary:** Q18, Q24–25, Q28, Q40–43.
Two independently configurable NuvioTV-style RAM profiles (reliable media Mbps cutoff only), preserve ParaleloDataSource, safe VOD cache identity, seek coalescing and absolute buffer truth. **Gate:** unknown bitrate→All Streams, no profile thrash, range fallback and cache validation, no false buffer rail.

### P5 — Audio/video fidelity, HDR, AFR, surfaces and output routes
**Primary:** Q29, Q36–39, Q47, Q49.
Original-language quality first, decoder fallback without silent downgrade, DV/HDR10 base-layer checks, AFR off by default, device-safe surfaces, HDMI ARC route recovery, retain soundbar volume/audio-focus workaround. **Gate:** capability-aware outcomes and explicit errors; ARC lossless limitations documented.

### P6 — Background lifecycle, watched status, post-play
**Primary:** Q20, Q22, Q44–45, Q48, Q50.
Pause/restore semantics, verified credits detection without cutting post-credits, retain Legacy posplay native overlay and next-episode preload, safe cleanup. **Gate:** Q22 recovery autoplay versus Q44 paused background return, no accidental credits-triggered exit.

### P7 — Full integration, existing unbuilt refinements and focused regression
**Primary:** Q01–Q50 plus every post-#134 handoff item.
Check compilation/source contracts, focus and panel Back, subtitle Format A, 2.5% font increments, held-OK repeat, parental overlay, phone landscape hero, pill focus/animation, absolute buffer rail, source switching and session safety. Fix only evidenced integration defects; checkpoint test results. **Gate:** no known blocking regressions; identify TV-only checks honestly.

### P8 — Final cached APK workflow and real-device validation
**Status: BLOCKED UNTIL SEPARATE EXPLICIT OWNER BUILD AUTHORIZATION.**
Verify P0–P7 completed or disclosed blockers, latest branch HEAD and build cache, run one controlled Android debug APK workflow containing **both** Q01–Q50 implementation **and** all unbuilt post-#134 refinements. Diagnose/retry only failed components when appropriate; validate on Xiaomi TV / ARC soundbar and relevant mobile landscape screens. Document build run ID, artifact, actual device findings and any follow-up commits. Never claim TV-verified from CI success alone.

## Package exit checklist (mandatory)

1. Exact HEAD before/after and changed files recorded.
2. Requirement IDs addressed; any partial or deferred scope explicitly listed.
3. Focused tests executed and result/log reference recorded (or explicit “not run”).
4. Regressions in protected features assessed, including post-#134 unbuilt changes.
5. Commits pushed to **feature/official-stream-selector** only; no uncommitted-only changes.
6. Progress tracker updated with next precise task and blockers.
7. User gets a concise status; next fresh chat starts by reading repository docs.

## First implementation prompt (use only after owner authorizes feature coding)

Continue Nuvio Legacy from GitHub branch `feature/official-stream-selector`. Read `docs/PLAYBACK_50_MASTER_SPEC.md`, `docs/PLAYBACK_50_WORK_PACKAGES.md`, `docs/PLAYBACK_50_IMPLEMENTATION_PROGRESS.md` and `docs/PLAYER_PENDING_REFINEMENTS_AFTER_134.md`. Verify HEAD and existing code. Execute only the next authorized incomplete package, perform focused checks, commit and update tracker. Preserve all post-#134 unbuilt changes. No GitHub Actions/APK build until separate explicit permission. Do not ask for routine confirmations; checkpoint on blockers or limits.
