# Playback recovery after APK #137

APK #137 failed the owner's same-stream comparison on the Xiaomi S Mini LED and JBL SB180 over standard HDMI ARC. Starting feature branch and installed build: `51791e0e92f0c67c09722e8316f3f1d2aae904a2`. Official NuvioMedia/NuvioTV dev reference: `3c14893c10c9bc067471a0b9b175b050b277f169`. Source and physical acceptance are open.

## Findings from the shipping paths

- Official PlayerRuntimeControllerInitialization constructs a forked Media3 1.8 engine, a hardware-first video renderer and decoder fallback. It has container-level DV handling and explicit Xiaomi profile policy. Legacy uses stock Media3 1.8, a per-codec validation/HEVC adapter, Jellyfin FFmpeg 1.8.0+1 and separate SDL and video planes. Identical dependency version labels hide meaningful differences.
- Official FFmpeg uses source downmix side data, center coefficient times 10^(dB/20), configurable normalization, LFE off by default. Gain (0..10 dB) precedes Night Mode on final PCM. Official Night Mode uses one linked detector, upward compression, smooth attack/release and a peak limiter.
- Legacy GanhoAudioProcessor operates before StereoDownmixP8, clips center before mixing, treats quad surround as center and applies memoryless per-channel peak compression. The fallback mixer unconditionally divides a 5.1 signal by 2.6642. Even isolated front material loses 8.51 dB. These are demonstrated source defects. The audio format and decoder used by the failing physical stream are unknown.
- Legacy's TCL surface restart is gated on TCL + MTK. The mere presence of that code does not establish that it ran on Xiaomi. Default frame-rate strategy is off in both players.
- Legacy bounds parallel HTTP memory, validates ranges and keys cache proofs to the selected representation. Official has stock and tuned load control plus parallel-range paths. Keep the two configured profiles and verify allocation and transport before changing limits.

## Continue from here

Read this checkpoint at current branch HEAD, inspect the diff since the build revision, and use the pinned official reference above. Trace actual policy/route transitions and independent production render callbacks. Correct downmix, processing order and dynamics, measure PCM; address verified codec/surface/transport differences; run production Android link and tests without assembling an APK. Record exact tested commits, measurements, remaining unverified paths and CI links here. Do not touch the APK trigger until separately authorized. Physical acceptance requires the owner's same-stream A/B.

## Integrated PCM and routing changes

Investigation checkpoint f1a6c4cafca94fd3ba9740f4bb70fcd67b959a15. The production sink now mixes conventional surround PCM in float, applies center before folding, omits LFE from the stereo mix, and applies the shelf, gain, linked Night Mode and limiter before a single output quantization. Center leaves quad's rear-left channel alone. API 29–32 can now negotiate direct output using the observed route. The initial PCM shape is checked on ARC, and adjustment-driven audio reselection disables tunneling. Same-item, same-position transactions and existing UI controls remain in place.

Local API 35 / actual stock Media3 1.8.0 compilation and executable PCM16/float replay passed. At 48 kHz, +10 dB gain measured +10.000 dB below overload. The 5.1 front-only fixture retained unity, center measured -10, +6 and +30 dB. Quiet tones at 125 Hz, 1 kHz and 8 kHz measured +5.999, +9.999 and +14.998 dB for the three Night presets. The same tests checked interchannel ratio, near-silence, peak bounds, bass response and live return to unity. Tests in tests/p5/PlaybackSignalTest.kt run via tests/cacheboost_android.py. Local compile uses the harness Activity stub; full application compile and native link are pending. The stock decoder exposes canonical PCM lanes, not FFmpeg frame-specific downmix side data. Native DV compatibility, first-frame metadata gating, HDR snapshots, load control and complete integrated verification remain open.
