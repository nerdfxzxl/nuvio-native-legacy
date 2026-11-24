# Final overall P5 source acceptance — PASSED

Date: 2026-11-24. Branch: feature/official-stream-selector.
Starting verified HEAD: 0367b20953864167e2f302f3afcfbbf6d4317750.
Exact verified implementation/test HEAD: 5d1a10d055c7caadab05196286c526f1375798fb.
Pre-acceptance branch HEAD: 35065510836b8f12edd07433af27038489f43200, documentation-only descendant.
This acceptance commit changes documentation only; the feature ref records the final commit.
See [dedicated Q49 final report](PLAYBACK_50_P5D_SOURCE_ACCEPTANCE.md).

**P5 SOURCE ACCEPTED. P6 AUTHORIZED FOR SOURCE DEVELOPMENT ONLY; NOT IMPLEMENTED HERE.**

| Requirement | Prior source verdict | Final combined source verdict |
|---|---|---|
| Q29 first-frame A/V sync | Accepted P5-C | PASS — existing handoff retained; Q49 mute composed with gate; surface/startup seek and cinematic tests passed |
| Q36 original audio/fidelity | Accepted P5-A | PASS — route-aware capability, actual output validation, original selection/error policy and PCM replay passed |
| Q37 Dolby Vision / proven HDR10 fallback | Accepted P5-B | PASS — prior codec policy preserved, actual HEVC/Format and HDR regressions passed |
| Q38 AFR OFF by default | Accepted P5-C | PASS — existing display ownership respected; AFR policy passed |
| Q39 decoder fallback/errors | Accepted P5-B | PASS — separate bounded Q49 ownership and existing native/codec error tests passed |
| Q47 stable surfaces | Accepted P5-C | PASS — ordinary Q49 path retains the surface; tunneling/fatal path rearms accepted gate; frame/surface regression tests passed |
| Q49 audio-route recovery | Dedicated P5-D review | PASS — 46 executed policy assertions, source integration contracts, real Android compile and native syntax preflight |

Prior accepted evidence: [P5-A run](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45225191077) and [P5-B/C run #8](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45337986924) (input bd31048dc133e886122f45badead8addef51741e), plus [committed B/C acceptance](PLAYBACK_50_P5BC_SOURCE_ACCEPTANCE.md). No broad repeat of already accepted packages.

## Final verification

[P5-D CI #2 / #45344027529](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45344027529), attempt 1, job 138278298592: COMPLETED SUCCESS on exact input 5d1a10d055c7caadab05196286c526f1375798fb. The actual completed job log was inspected, not just the green result. Checkout verified the tested SHA; GitHub comparison against the pre-acceptance HEAD 35065510836b8f12edd07433af27038489f43200 confirmed exactly three documentation-only changes.

Actual logged results:
- Original-language native selection: audioescolha: ok; Audio Playback Error native policy: 10 assertions PASS; video error native policy: 47 assertions PASS.
- HDMI soundbar audio-focus regression: both setAudioAttributes calls retain handleAudioFocus=false.
- P2 arc/download/first-frame timing PASS; cinematic handoff 41 checks PASS; P2-B frame-hold 17/17 PASS.
- Real Android NDK C/JNI syntax preflight PASS, including relevant Home, player HUD, audio, subtitle and post-#134 preservation guards.
- Real API35 / Media3 1.8.0 production Kotlin compilation: OK. Actual PCM/cache replay PASS (including 16-bit, float and bitstream guard).
- Actual HEVC CSD/IRAP, codec proof, runtime and reuse fixture replay: 47 assertions PASS.
- P5-A fidelity 17 assertions PASS; P5-D route policy 46 assertions PASS; P5-D source integration contracts PASS (not device execution).
- P5-B HDR/decoder policy 27/27 PASS; P5-C AFR 34 assertions PASS; P5-C surface/startup seek 55 assertions PASS.

No APK assembly, signing, publication, release or default-branch merge. The targeted compile is not full application linking and the executable policy and fixture tests plus source contracts are not physical Android routing tests.

## Final disposition

The bounded source review covers Q49's new interactions with previously accepted gates and identifies no further demonstrated source blocker. P0–P4 and P5-A/B/C accepted checkpoints remain intact. Q18 embedded P2P readiness remains owner-deferred. P6 is the next authorized source package, not part of this session. No physical ARC/eARC/Bluetooth/speaker switching, hardware fidelity, actual A/V synchronization, full link, real TV UI or integrated APK behavior is claimed tested; those remain P7/P8 gates. No repeat CI is needed for this documentation-only acceptance commit.
