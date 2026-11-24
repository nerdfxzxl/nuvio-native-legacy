# P5-D / Q49 dedicated final source acceptance — 2026-11-24

**Verdict: P5-D / Q49 SOURCE ACCEPTED.** Physical-device and full-link validation remain separate.
Starting branch HEAD: `0367b20953864167e2f302f3afcfbbf6d4317750`.
Latest production and test input: `5d1a10d055c7caadab05196286c526f1375798fb`.
This report and tracker are documentation-only descendants of that input; final branch HEAD can be obtained from the feature ref.

## Production changes

- `6c4476dec0c5003b177c0e6c42437e87823676a6`: actual Media3 AudioTrack provider/route observer, route fingerprint and debounce policy, route-aware P5-A capability probe.
- `201be95d83376a94f07ad5cda0988c697e44582a`: visible-session ownership in NvPlayer, audio-only track disable/enable using the pinned Media3 track-selection and playback-message APIs, one same-player prepare for a correlated fatal audio error, native generation-owned route/error/watchdog bridge, lifecycle cancellation.
- `a190d6199b0ca6d84ef5656d1db22348148995f1`: independent faithful PCM proof, sink validation, integration contracts, dedicated test-only CI.
- `5d1a10d055c7caadab05196286c526f1375798fb`: actual output creation serial/timestamp confirmation, direct-profile fingerprint, one additional bounded reselection only for a newly stable route requiring a different mode, paused deferred validation, route-only capability updates without native track republishing, tunneling-specific first-frame rearm.

Affected production files: AudioRouteObserverP5D.kt, AudioRouteP5D.kt, AudioOutputProbeP5A.kt, NvPlayer.kt, LiveWindowRecovery.kt, NuvioActivity.kt, src/video_android.c, src/video.h, src/app.c. Test files: tests/p5/AudioRouteP5DTest.kt, tests/p5_audio_route.sh, tests/p5d_route_source_contract.py, tests/cacheboost_android.py. New workflow only: .github/workflows/p5d-route-test-only.yml. The baseline-to-input GitHub compare confirms four forward commits, fourteen changed files and no APK workflow/trigger changes.

## Requirement-level source acceptance

| Q49 scope | Source implementation | Verification / verdict |
|---|---|---|
| Actual route and capability detection | AudioTrack.routedDevice, active track routing listener; device inventory callbacks are only a wake-up signal. API33+ single media-attributes route used when actual output unavailable; unknown remains unproven. Encodings/rates/channels/masks and direct profiles fingerprinted. | PASS: debounce/duplicate/rapid-route/capability policies and real Android API compile. Physical switching pending. |
| In-place recovery | Retain compatible existing output; otherwise same audio group/index override, renderer disable and playback-thread message acknowledgement, then enable. No source URL, media item, seek, stop, pause snapshot, speed or user-setting rewrite in normal route path. At most two reselections for a stable route change, one correlated fatal same-player prepare. | PASS: production policy, API35/Media3 compile and source integration contracts. Actual renderer transition, buffering and state preservation require device proof. |
| P5-A fidelity | Separate PCM fidelity from encoded capability, reject unknown/unsupported HD delivery and ARC multichannel PCM, require actual output config and matching input/creation evidence. Keep selected track; refresh native capability metadata only. Explicit error if selected track cannot be delivered faithfully. | PASS: shared P5-A and Q49 policy, actual PCM replay and Media3 compile. Physical output fidelity remains unverified. |
| A/V, HDR/DV, AFR and surface | Audio mute composed with accepted handoff/AFR gates; no ordinary route surface replacement; actual negotiated tunneling or fatal prepare rearms first-frame gate. Existing video failure and recovery owners take priority. | PASS: prior B/C evidence retained and combined compile/fixture/regression gates passed. Physical fidelity/sync not verified. |
| Lifecycle and bounded failure | Visible player/resource, native generation, route transaction ticket/phase, output serial/timestamp, 750 ms quiet period, 5 s flap limit, 8 s transaction/no-output deadline, native 15 s owner; cancel on release/background/new user audio selection or competing recovery. Paused state is not resumed. Existing Audio Playback Error event/UI reused. | PASS: ticket/timeout/cancellation/reselection policies, native C/JNI syntax and source integration contracts. Physical lifecycle pending. |
| HDMI soundbar fix | Existing setAudioAttributes(..., false) preserved, no volume-driven route event or automatic audio focus handling. | PASS: exact latest CI confirms both audio-focus calls remain false. |

## Executed evidence

Local Kotlin/JVM compile and execution on exact Git blob-matched production policy + test sources:
- On a190d619: 40 assertions passed.
- On 5d1a10d0: **46 assertions passed**. Blob hashes: AudioRouteP5D.kt 95d33b5736bed38de2dc815cca9770c30a8f54b9; AudioFidelityP5A.kt 670a027e006ca4af03e49628c9d9634b8b06f05e; AudioRouteP5DTest.kt a49db8cc80b645383154c806669734d522193b13.
- Local JVM tests exercise production policy, not Android hardware or a full player runtime.
- New source integration contracts check actual production call paths; these are source-level assertions, not executed Android integration.
- [First P5-D CI](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45343641760), exact a190d6199b0ca6d84ef5656d1db22348148995f1, job 138272894388: setup/checkout and native regression + HDMI focus step completed successfully; NDK step in progress at checkpoint. Superseded for final acceptance by the next revision.

## Final CI evidence

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

## Bounded source review and disposition

Reviewed the Q49 changes to actual-route observation, playback-thread audio-only selection, input/output configuration validation, native generation ownership, and the interaction with the already accepted video/AFR/surface gates. The previous source review and four implementation commits addressed the identified gaps. The final exact-source tests and compilation reveal no further demonstrated source blocker; no repeated broad audit or unchanged CI rerun is warranted. The normal Q49 path does not replace the source or video surface or write a saved playWhenReady value; tunneling or a fatal same-player prepare rearms the existing first-frame gate. Capability-only native updates preserve track ordinals and do not invoke full track-list selection. These are source findings, not a claim of uninterrupted output on actual TV hardware.

**P5-D SOURCE ACCEPTED.** Q49's source gate is closed. P7/P8 retain full application linking, actual Xiaomi TV routing, ARC/eARC/Bluetooth/speaker handshakes, physical fidelity and synchronization, retained frames, remote/lifecycle and integrated APK validation. Q18 remains owner-deferred. See [final P5 acceptance](PLAYBACK_50_P5_FINAL_SOURCE_ACCEPTANCE.md) for the next authorized package.
