# P8 Phase B — combined implementation and acceptance checkpoint (2026-11-26)

**Disposition: SOURCE ACCEPTED — final exact-source CI and log audit passed; physical acceptance pending.** Owner authorized all Phase B work as one scope. No additional subphases. Phase A source acceptance preserved at 102f7087373496e0bcd301c7aa2b1e4da2416ac3; P8 overall not yet accepted.

## Source revisions
- fea63e31803d1ffae5d86434c2835db757adbd94: pill, icon, focus, subtitle metadata/default/lift, panel width and smooth scroll.
- aab806aa437609f18311e0b4987906f4d606333d: Legacy/Xperience/imported badge selection, five independently adjustable playback badge categories, actual vs source metadata separation.
- dabc8fc72670052603d76098248c0ce4ee4938a8: combined regression submission and additional source contracts.
- **85f5723cacce1f4c2748446e02cba14c601b83a3**: latest intended tested input, correct legacy-mode fixture expectation, explicit fallback fixture cases, clipped outer-ring visibility and pointer ownership. This checkpoint changes only documentation after that revision.

## Requirement matrix
| Requirement | Implementation / boundary |
|---|---|
| Action pill | Occupied width plus padding, no mirrored unused expansion; focus handoff capped by viewport; anchored row origin, existing labels and pointer targets. |
| Audio icon | Bolder white speaker cabinet SVG and corresponding 128x128 RGBA PNG; verify rendered visual weight on devices. |
| Focus | 3px white focused outline and outer white active+focused track/provider ring, Show All/Less visible focused state. |
| Subtitle names | Existing readable release metadata retained; opaque primary metadata falls back to readable URL basename if supplied; English (n) in English source mode including singleton; unnamed embedded English (Embedded); meaningful embedded track labels retained. No invented release filenames. |
| Font and lift | Roboto new/default/reset style, preserve existing user-selected stored font. Subtitle lift conditioned on bottom controls, not side sheets; existing next-episode obstruction handling preserved. |
| Audio/subtitle panels | Shared 960-unit width capped to available virtual viewport, pixel offset spring for row motion, touch drag preserved, provider strip keeps horizontal focus scrolling. |
| Badge packs | Explicit Legacy and Xperience (built-in), imported URL options, separate player pack choice including follow stream; persisted profile pack tokens and existing visibility toggle. |
| Individual controls | Separate resolution, HDR/Dolby Vision, Atmos, audio codec and audio channel switches for playback badge metadata; all default on. This is five categories, not a per-rule editor for every imported custom badge. |
| Playback Info | Measured resolution/HDR distinct from explicitly labeled source codec/HDR/resolution/container metadata; selected audio track codec/channels/rate shown separately from output route. Unknown actual codec/FPS/decoder/format not fabricated; further live telemetry remains limited to what existing interfaces supply. |
| Integration | Test-only CI submitted; previous source gate and physical limits remain. No build/release/default-branch changes. |

## Exact-source gates
Latest workflows target 85f5723cacce1f4c2748446e02cba14c601b83a3:
- [Android integration #10](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45744346295), in progress at checkpoint.
- [Linux regressions #15](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45744346376), pending at checkpoint.
- [P6 lifecycle #32](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45744346342), pending at checkpoint.
Previous superseded input dabc8fc: #9 Android, #14 Linux, #31 lifecycle. Do not treat their results as exact final source verification. Added badge pack persistence fixture to focused and Linux regressions; expanded panel/name and occupied geometry contracts. Actual production CI has not yet passed at the intended final revision. Local full checkout/compiler unavailable in previous continuation, and no local full build claimed here.

## Continuation (same Phase B, not another phase)
Inspect exact-revision workflow conclusions and all executed job logs. Batch-fix demonstrated build or test errors and any demonstrated source requirement gaps; keep existing tests meaningful, especially legacy subtitle behavior and preserved Phase A playback. Review per-badge visibility and actual telemetry limits, verify settings persistence and UI ownership, then record full final source disposition only if warranted. Compare HEAD to tested SHA (documentation-only successors allowed). Keep Q18 prior authorized deferral only. Do not claim physical animation, focus, clipping, subtitle baseline, soundbar or decoder compatibility without physical tests.

After source acceptance, obtain separate explicit authorization for the one cached integrated APK as required by P7 acceptance and P8 handover. Test the previously failing streams on Redmi Note 10 Pro and Xiaomi TV/ARC, plus all Phase B visual/remote/touch acceptance cases. No APK assembled or triggered in this continuation.

## Final review follow-up — 2026-11-26

All three earlier workflows (Android #10, Linux #15, P6 #32) **PASS** at 85f5723cacce1f4c2748446e02cba14c601b83a3. All nine executed job logs inspected; no reported ASan/UBSan fatal errors or assertion failures. Optional macOS legacy job skipped, not counted as passed. Current HEAD at that review was documentation-only ahead.

The final source audit found an acceptance blocker despite passing CI: the existing Legacy format row and independent Fusion title row could render simultaneously. Old audio labels could bypass individual badge switches, and JOC or source-track channels could be misrepresented as sink output. The owner's November 25 decisions are one selected Legacy/Xperience system, one compact top-left row, actual playback metadata, source details in Playback Info and independent badge content controls.

Corrected in **528224741a60fd5fba8f55553b26f44be5d80dac**:
- Exactly one OSD badge draw call; no under-title duplicate or legacy format-row bypass.
- Legacy preserves compact individual marks; Xperience/imported rules receive only enabled observed playback properties. No fallback to another badge family for unmatched rules.
- Show in player controls the entire row. Five independent switches gate observed metadata. Retired competing format/style/position/codec settings remain in persisted indices but are hidden. Original stream metadata stays in the detailed info panel. Compact/detailed Playback Info owns the overlapping top-left area while open.
- PCM is labeled PCM, not TrueHD/DTS passthrough. Selected-track channel count and JOC are not represented as confirmed PCM sink channels or Atmos output; output channels omitted when unavailable. Bitstream badges indicate the selected passthrough format, not external receiver/speaker verification. A missing pack rule may omit a badge rather than substitute artwork. Five category switches, not an individual imported-rule editor. Unknown video codec/FPS/decoder and actual PCM output channel/rate remain unreported rather than guessed.
- New production policy helper and test exhaust 3840 combinations of choice, stream pack, output route, switch mask and info ownership (plus source contracts for the single draw call). Test results pending.
- Phase A playback/recovery implementation remains unchanged.

Fresh test-only workflows against correction:
- [Android #11](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45812115962)
- [Linux #16](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45812116146)
- [P6 #33](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45812115979)

**Historical checkpoint: Phase B remained partial until fresh CI and log review; superseded by final signoff below.** Earlier passing CI is not evidence for the corrected production revision. No integrated APK triggered. After the exact-revision gate, compare documentation-only HEAD, inspect logs and finalize bounded source acceptance. Physical verification of actual placement, artwork, D-pad/touch animation, subtitle appearance, output route and TV decoder behavior remains mandatory. Q18 remains the prior owner-approved deferral. Do not restart implementation or introduce another phase; do not publish or merge. Separate explicit cached APK build authorization required after source acceptance.


## Final Phase B source acceptance — 2026-11-26

**PASS — P8 Phase B SOURCE ACCEPTED, bounded source/integration gate.** Exact tested production revision: `528224741a60fd5fba8f55553b26f44be5d80dac`. Android #11 (45812115962), Linux #16 (45812116146), P6 #33 (45812115979) all completed SUCCESS. All nine executed job logs fetched and inspected for test evidence and fatal sanitizer/error annotations; none found. Android focused and ARM64 jobs 139882339190/139882339672 passed, including actual AArch64 link, SDL_main and Gradle compilation. Optional macOS legacy job skipped, not passed. Linux jobs 139882339528, 139882340006, 139882340010, 139882340015, 139882340165, 139882340814 all passed, including 50 sanitizer worker repetitions. P6 job 139882339319 passed actual production Kotlin/Activity/SDL compilation, HEVC adaptation, audio fidelity, Q49 75 assertions, HDR 27/27, AFR 34, surface 55 and frame geometry. Focused and Linux badge tests each passed 3840 policy combinations. Focused source contracts confirm single badge draw site, five independent visibility consumers, selected-pack persistence, source/output separation, 960-unit panels, focus rings, subtitle fallback names, Roboto default, bottom-control lift and occupied pill geometry.

GitHub compare from tested revision to pre-signoff HEAD `bb48ab0d1cdc3f1d1369f4667840b2043995cb93`: one commit ahead, zero behind, exactly two documentation files (this acceptance report and implementation progress). This signoff changes those same documents only. No new production/test/workflow edits or duplicate CI required. Prior source audit and its badge correction are retained; this final continuation verifies the documented correction through exact-source CI and comparison, not a new independent exhaustive source audit. No further demonstrated blocking source defect in this bounded scope. Phase A source acceptance retained. P0–P7 and P8 A/B source gates closed; P8 overall remains PARTIAL pending integrated APK and physical device acceptance. Q18 remains the prior authorized deferral. Unknown actual PCM sink channels/rate and unavailable video telemetry remain omitted, five category switches are not per-imported-rule editing; real rendering, remote/touch behavior, audio/ARC fidelity, decoder compatibility and previously failing streams require device verification. No APK triggered, built, published or merged. Next step: separately authorized cached integrated debug APK, then device testing.
