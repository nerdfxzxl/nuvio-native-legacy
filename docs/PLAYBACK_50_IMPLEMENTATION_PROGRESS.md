# Nuvio Legacy — Q01–Q50 Implementation Progress

**Last updated:** 2026-11-22
**Status:** P0 BASELINE AUDIT COMPLETE; P1 FEATURE IMPLEMENTATION NOT STARTED; APK BUILD NOT AUTHORIZED.
**Branch:** `feature/official-stream-selector` (not `master`).
**Prior feature baseline:** `6171645e05d2137d2a1e9ed1b113687246acd7ca` — docs: lock subtitle filename Format A and unbuilt wrapping/parser test handoff.
**Latest planning commits:** master specification `7d3c81d81aee309843dd2f0b767c6ef3c2d59723`; work packages `b059a7a9a42ae2b91d87bff0a03bfeb314d0f647`; this progress tracker is an additional documentation-only commit. **Recheck actual HEAD at the beginning of every session.**

## Authoritative references

- [Approved Q01–Q50 master specification](PLAYBACK_50_MASTER_SPEC.md)
- [Bounded package plan and session protocol](PLAYBACK_50_WORK_PACKAGES.md)
- [Existing post-#134 unbuilt refinements](PLAYER_PENDING_REFINEMENTS_AFTER_134.md)
- [P0 verified source inventory, preservation audit and known risks](PLAYBACK_50_P0_BASELINE.md)
- [Official NuvioTV dev reference](https://github.com/NuvioMedia/NuvioTV/tree/dev)

## Existing work to preserve (not part of Q1–Q50 implementation progress)

**Latest built and device-tested artifact per handoff: #134.** After #134, code for eight refinement groups was committed but **not built**. They include official parental guide, landscape mobile hero, one-Back panel exit, English subtitle rows and Format A complete decoded release filenames with UTF-8-safe wrapping, 2.5% font increments, held-OK acceleration, absolute buffering rail and responsive focused player pill. The handoff records source-level checks only; native/Android/TV verification is still outstanding. **All must be in final integrated workflow alongside the 50-question refinements.**

## Work package status

| Package | Scope | Status | Implementation commits | Verification |
|---|---|---|---|---|
| P0 | Baseline, inventory, preservation | COMPLETE — source audit only | [`d0ff5a1`](https://github.com/nerdfxzxl/nuvio-native-legacy/commit/d0ff5a1f44fc42034c867ca149a21e1814d2f77c) (documentation) | GitHub branch diff/source/test scripts inspected; **no tests, compile or APK run** |
| P1 | State/source identity/cancellation | NOT STARTED | — | — |
| P2 | Cinematic/arc/status/speed | NOT STARTED | — | — |
| P3 | Selector/reload/recovery | NOT STARTED | — | — |
| P4 | Buffer/network/cache/seek | NOT STARTED | — | — |
| P5 | Fidelity/HDR/surface/audio route | NOT STARTED | — | — |
| P6 | Background/credits/post-play | NOT STARTED | — | — |
| P7 | Integration/regression, post-#134 | NOT STARTED | — | — |
| P8 | One cached APK + device tests | BLOCKED — NEEDS BUILD AUTHORIZATION | — | — |

**Q01–Q50 implementation count: 0/50 verified implemented under this initiative.** Some behaviors already exist in Legacy; inspect and classify them during P0 rather than duplicating code or claiming they are absent. “Committed” ≠ “compiled” ≠ “APK built” ≠ “device tested”.

## Change log

- **2026-11-22 / planning:** Owner selected Option A: GitHub checkpoints and fresh chats, one bounded work package per chat. Approved all 50 design choices, with Q50 Option A. Requested creation of canonical GitHub spec, tracker, package boundaries and preservation rules. Explicitly prohibited implementation and APK workflow during planning.
- **2026-11-22 / documentation:** Created master decision register, bounded work-package plan and this progress tracker. No feature code, CI trigger or APK run requested.

## Immediate next action

**P0 is complete. Do not re-run P0 in the next chat.** Await owner's request to execute **P1 only** in a fresh chat. Read master spec, package plan, this tracker, P0 baseline and post-#134 handoff; verify HEAD and local working tree where applicable. P1 targets playback source/session identity, stale callback cancellation, same-source renewal and recovery/background precedence. It must not trigger APK/GitHub Actions. Stop at P1 checkpoint and update this tracker. P8 requires separate explicit owner workflow authorization.

**P0 known integration issue:** Android `src/video_android.c:video_buffer_fim()` currently returns nonzero only for live streams, while `src/player.c` uses that API for the VOD buffer rail; `src/video.c` updated after #134 is a non-Android target path. See [P0 baseline](PLAYBACK_50_P0_BASELINE.md) and ensure P4/P7 resolves and tests actual Android VOD buffer endpoint. The early native syntax preflight omits several newly changed files; add coverage before P8. Linux compatibility of subtitle C harness scripts also needs checking. Full detailed Q01–Q50 prose document is not yet archived in GitHub; the in-repo master register is condensed.

- **2026-11-22 / P0 source baseline:** Verified HEAD `5a9975c` and source commit `6171645`, reviewed build #134 source `bad57b2`, compared 26 post-build commits / 19 changed paths, inspected native, Android, test and workflow files; committed [baseline report](PLAYBACK_50_P0_BASELINE.md). Found Android VOD buffered-end gap and fast-preflight coverage issues. **No feature code, tests, native compilation, CI run or APK triggered.**

## Mandatory checkpoint template for future sessions

- Session date and assigned package:
- HEAD before / after:
- Exact requirement IDs addressed:
- Files changed and commit SHAs:
- Checks executed (command, outcome, limits):
- Existing features/regressions checked:
- Not verified / TV-only / known blockers:
- Remaining scope and next precise action:
- Workflow/build triggered? (default NO):
