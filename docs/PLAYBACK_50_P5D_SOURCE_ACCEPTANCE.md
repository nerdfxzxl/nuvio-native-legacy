# P5-D / Q49 dedicated source-acceptance checkpoint — 2026-11-24

**Verdict: IMPLEMENTED IN SOURCE / ACCEPTANCE PENDING.** Not a claim of full verification.
Starting branch HEAD: `0367b20953864167e2f302f3afcfbbf6d4317750`.
Latest production and test input: `5d1a10d055c7caadab05196286c526f1375798fb`.
This report and tracker are documentation-only descendants of that input; final branch HEAD can be obtained from the feature ref.

## Production changes

- `6c4476dec0c5003b177c0e6c42437e87823676a6`: actual Media3 AudioTrack provider/route observer, route fingerprint and debounce policy, route-aware P5-A capability probe.
- `201be95d83376a94f07ad5cda0988c697e44582a`: visible-session ownership in NvPlayer, audio-only track disable/enable using the pinned Media3 track-selection and playback-message APIs, one same-player prepare for a correlated fatal audio error, native generation-owned route/error/watchdog bridge, lifecycle cancellation.
- `a190d6199b0ca6d84ef5656d1db22348148995f1`: independent faithful PCM proof, sink validation, integration contracts, dedicated test-only CI.
- `5d1a10d055c7caadab05196286c526f1375798fb`: actual output creation serial/timestamp confirmation, direct-profile fingerprint, one additional bounded reselection only for a newly stable route requiring a different mode, paused deferred validation, route-only capability updates without native track republishing, tunneling-specific first-frame rearm.

Affected production files: AudioRouteObserverP5D.kt, AudioRouteP5D.kt, AudioOutputProbeP5A.kt, NvPlayer.kt, LiveWindowRecovery.kt, NuvioActivity.kt, src/video_android.c, src/video.h, src/app.c. Test files: tests/p5/AudioRouteP5DTest.kt, tests/p5_audio_route.sh, tests/p5d_route_source_contract.py, tests/cacheboost_android.py. New workflow only: .github/workflows/p5d-route-test-only.yml. The baseline-to-input GitHub compare confirms four forward commits, fourteen changed files and no APK workflow/trigger changes.

## Requirement-level checkpoint

| Q49 scope | Source implementation | Verification / verdict |
|---|---|---|
| Actual route and capability detection | AudioTrack.routedDevice, active track routing listener; device inventory callbacks are only a wake-up signal. API33+ single media-attributes route used when actual output unavailable; unknown remains unproven. Encodings/rates/channels/masks and direct profiles fingerprinted. | Debounce/duplicate/rapid-route/capability tests pass. Actual Android API compile and device switching pending. |
| In-place recovery | Retain compatible existing output; otherwise same audio group/index override, renderer disable and playback-thread message acknowledgement, then enable. No source URL, media item, seek, stop, pause snapshot, speed or user-setting rewrite in normal route path. At most two reselections for a stable route change, one correlated fatal same-player prepare. | Policy tests pass; compilation and integration checks pending. Actual Media3 renderer transition, buffering and state preservation require device proof. |
| P5-A fidelity | Separate PCM fidelity from encoded capability, reject unknown/unsupported HD delivery and ARC multichannel PCM, require actual output config and matching input/creation evidence. Keep selected track; refresh native capability metadata only. Explicit error if selected track cannot be delivered faithfully. | Shared P5-A + route policy tests pass; Android/Media3 configuration integration pending. |
| A/V, HDR/DV, AFR and surface | Audio mute composed with accepted handoff/AFR gates; no ordinary route surface replacement; actual negotiated tunneling or fatal prepare rearms first-frame gate. Existing video failure and recovery owners take priority. | Source inspection only; prior accepted B/C evidence retained; new combined compile/fixture/regression gate pending. Physical fidelity/sync not verified. |
| Lifecycle and bounded failure | Visible player/resource, native generation, route transaction ticket/phase, output serial/timestamp, 750 ms quiet period, 5 s flap limit, 8 s transaction/no-output deadline, native 15 s owner; cancel on release/background/new user audio selection or competing recovery. Paused state is not resumed. Existing Audio Playback Error event/UI reused. | Executed ticket/timeout/cancellation/reselection policy tests pass; native C/JNI and integration checks pending. |
| HDMI soundbar fix | Existing setAudioAttributes(..., false) preserved, no volume-driven route event or automatic audio focus handling. | Earlier revision CI native and audio-focus step passed; exact latest rerun pending. |

## Executed evidence

Local Kotlin/JVM compile and execution on exact Git blob-matched production policy + test sources:
- On a190d619: 40 assertions passed.
- On 5d1a10d0: **46 assertions passed**. Blob hashes: AudioRouteP5D.kt 95d33b5736bed38de2dc815cca9770c30a8f54b9; AudioFidelityP5A.kt 670a027e006ca4af03e49628c9d9634b8b06f05e; AudioRouteP5DTest.kt a49db8cc80b645383154c806669734d522193b13.
- Local JVM tests exercise production policy, not Android hardware or a full player runtime.
- New source integration contracts check actual production call paths; these are source-level assertions, not executed Android integration.
- [First P5-D CI](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45343641760), exact a190d6199b0ca6d84ef5656d1db22348148995f1, job 138272894388: setup/checkout and native regression + HDMI focus step completed successfully; NDK step in progress at checkpoint. Superseded for final acceptance by the next revision.
- [Latest exact P5-D CI](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45344027529), exact 5d1a10d055c7caadab05196286c526f1375798fb: queued behind the preceding run at checkpoint. This is NOT a successful CI result. No completed job log for the latest input is available yet.

## Remaining gates / blockers

No overall P5 acceptance until completed logs on the latest exact source/test SHA establish API35/Media3 1.8.0 Kotlin compilation, NDK C/JNI syntax, PCM and HEVC fixture replays, existing P5-A/B/C regressions, Q49 route policy and integration contracts, and relevant post-#134 preservation checks. Fix only demonstrated failures; no broad repeated audits. The full application link, actual routing on Xiaomi TV, ARC/eARC/Bluetooth/speaker handshakes, actual output fidelity, synchronized recovery, retained-frame behavior and device lifecycle remain P7/P8. No APK built or published. Q18 remains deferred. P6 NOT AUTHORIZED.
