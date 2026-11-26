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
