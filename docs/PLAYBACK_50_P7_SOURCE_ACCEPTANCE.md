# P7 comprehensive integration acceptance — 2026-11-25

**Disposition: source integration review completed; final acceptance conditional on fresh exact-revision verification. NOT a final PASS yet. P8 remains NOT authorized.**

Branch: `feature/official-stream-selector` only. Starting reviewed HEAD `d40b9ec0d5c133e96a6210bee55c395c979161ed`. Q46 corrected production/test input `9d4878898010e405c4b3a1dfc5b74c6aae1d6d25`. This report is a documentation-only successor; verify the actual branch HEAD and compare before closing the gate.

## Scope and method

Re-read approved master register, work packages, P0 baseline, post-#134 handoff, P1–P6 accepted checkpoints and P5/P6 final acceptance reports. Compare P6 acceptance document HEAD `46793eb95d680582a80dcd8250cf96a15b0d4e62` to P7 integration #5 HEAD: 55 commits ahead, zero behind; 55 changed paths returned, including production changes in NvPlayer.kt, PlaybackIntentP6.kt, app.c, video_android.c, descoberta.c/.h, intro.c/.h, parental.c/.h, p7_join_workers.h, player.c, legendasui.c, estatvivoui.c/.h, playback_diag_p7.h. Re-fetched current production paths and audited their ownership, UI, diagnostics and preservation contracts against accepted implementation evidence. This is a bounded **integration/delta audit**, not a new independent execution of every historical focused test or exhaustive line-by-line reimplementation of P0–P6. Unchanged accepted source/test evidence is inherited; actual source excerpts and current full link/host logs were inspected. No physical device claims. No expanded-spec guesswork; the approved master register and explicit owner corrections govern.

## P7-A/B/C actual verification

- [Android integration #5](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45603811488) at `d40b9ec0d5c133e96a6210bee55c395c979161ed`: required jobs Android 139169317663 and focused 139169317312 completed SUCCESS, actual logs inspected. ARM64 ELF, SDL2 dependency, SDL_main, nativeEvento, Rust nv_mkvcore_achar_tracks, embedded Nuvio Engine/0.1.3; production Kotlin/Java compile and JNI contract incl 33 C-to-Kotlin static method lookups. Workflow asserts no APK generated. Delayed subtitle and telemetry ownership fixtures pass, 8/8 track-command ownership, post-134 static contracts, catalogue and actual posplay tests pass. macOS job skipped by explicit host-run policy after Linux gate passed; not marked successful.
- [Linux #10](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45590217741) at `268b255e2b1c9bd19dba0898cbaa95fa2b868828`: all six groups / 27 suites passed; actual logs inspected, no reported sanitizer diagnostics. Both manual and automatic resume paths and 50/50 worker-shutdown repetitions pass. No production edits between this input and #5. Android-specific behavior is not proven by Linux stub backends. ASan leak detection disabled for targeted crash diagnostic, not leak acceptance.
- Prior accepted focused P4 native VOD, RAM, parallel, cache adapter/Media3 checks; [P5 final](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45344027529) audio, PCM, HDR, codec, AFR, surface and route; [P6 final #27](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45438480856) ownership, lifecycle, credits and watched/Next tests. These are inherited evidence, not all rerun at #5.
- Q46 audit identified actual stale/estimated speed defect; correction described below. Fresh [Android integration #6](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45605515198) and [Linux #11](https://github.com/nerdfxzxl/nuvio-native-legacy/actions/runs/45605515205) target exact `9d4878898010e405c4b3a1dfc5b74c6aae1d6d25`, both in progress when recorded. Linux retains all 27 suites and adds estatvivo (28 total); Android retains existing focused checks and adds estatvivo and playback_diag_p7. No new ordinary APK workflow. Full local checkout/native dependencies unavailable; do not claim new tests pass before logs confirm.

## P7-D — individual requirement integration matrix

Status **Retained** means prior accepted implementation evidence and current integration/delta review found no new demonstrated source blocker; not a fresh per-requirement runtime proof. **Recheck** is a corrected source defect whose new exact CI gate is open. **Deferred** is the sole explicitly owner-deferred feature. All other physical behavior remains for P8.

| Requirement | Source disposition | Current path / evidence and acceptance boundary |
|---|---|---|
| Q01 — cinematic look / universal arc | Retained | player.c desenharCinema uses saved artwork mode, four-stop gradient, fixed logo geometry; plrui.c plrui_arco routes wrappers through nv_arc_quadro. P2-C 872-file native loader audit inherited; visual parity device-only. |
| Q02 — real loading phases | Retained | player.c estadoCargaP2 uses engine events, subtitle fetch, generation-bound native phase; video_download_bps. P2 tests; no generic fake message. Validate actual TorBox phases. |
| Q03 — same-source IP/link recovery | Retained | app.c FJOB_RENOVAR_VOD verifies job/list/profile/target and calls stream_vod_renovada_aplicar before reusing URL/position; fonteid_p1.h release identity; warning-clip failure selects manual sheet. P1/P3 tests; live ElfHosted signed URL/IP changes device/network. |
| Q04 — stall vs slow progress | Retained | video_android.c bombearRetryVideo uses bufavanco_segura and bounded retry; app.c tentarProximaFonteVOD checks real progress. P1/P4/P5/P6 evidence; large REMUX live test. |
| Q05 — true first-frame fade | Retained | NvPlayer onRenderedFirstFrame, native generation gate, player.c nv_p2d_visual_ready, abrecine.c only starts 200ms exit on stable frame; P2-D 41 checks, P5-C surface checks. |
| Q06 — rebuffer last frame | Retained | FrameHoldP2B + NvPlayer copy/result ownership, native buffering UI; SDR bounded retention, HDR/DV cannot guarantee no flash. P2-B 17/17 and P5 tests. |
| Q07 — reduced motion | Retained | plrui_arco reduced-motion arc, abrecine and desenharCinema reduced path. P2 static/host evidence; actual feel device-only. |
| Q08 — Back / cancel | Retained | player.c loading owner, app.c source/job invalidation and profile/episode generation, P3 retained-card gate. #10 player_back and selector regressions; actual remote during open. |
| Q09 — manual replacement | Retained | app.c abrirFonteManualDireta retires generation and pending jobs; NvPlayer release/command generations, prior P1 old source pause before release. #10 manual resume, stale callbacks #5. |
| Q10 — loading details | Retained | player.c linhasAbrindo displays genuine stats when available, loading_nav_p2c and loading_handoff_p2d own OK while veil active. P2-D tests. |
| Q11 — failed recovery picker | Retained | app.c selecionarAposFalhaVOD first available focus + recarregar_externo, Android tentarProximaFonteVOD returns instead of sibling autoplay. #10 manual and reload policies. |
| Q12 — distinct recovery/rebuffer | Retained | player.c status/recovery and small arc paths, video_android.c video_reconectando; P2 accepted. Real buffer/reconnect visual pending. |
| Q13 — recovery styling | Retained | Native player small recovery text/arc path inherited from P2; unchanged styling; actual white text/no plaque visual pending. |
| Q14 — responsive loading controls | Retained | player.c player_carregando holds ownership via nv_p2d_loading_owner; 15s escape + Back/OK; P2-D tests and #10 player_back. |
| Q15 — source accepted before cinema | Retained | app.c manual picker/abrirFonteManualDireta, player loading gate; #10 both manual starts first row and automatic preference routing. Saved exact Resume only per approved configuration. |
| Q16 — VOD-only cinema | Retained | player.c ehCanal exclusion and independent trailer path; #10 trailer/scrobble/EOF regressions; real Live TV retune pending. |
| Q17 — artwork reuse | Retained | player.c cinemaArteObter and artehero_logo_sessao; static title quad and backdrop from selected title, no separate metadata request. |
| Q18 — HTTP / genuine embedded P2P readiness | Deferred only for playable percentage | player.c estadoCargaP2 real engine phases, p2pmotor measured pieces/peers, video_download_bps HTTP; no fabricated bar. Genuine current-playhead denominator/percentage not implemented, explicitly owner-deferred. |
| Q19 — explicit switching with compatible continuity | Retained | P1 compatible source/track/zoom restore; native video_android.c publication and choosing under generation/lock, Kotlin trackCommandOwnedP7. #5 delayed publication and 8 track tests. Real track/position carryover pending. |
| Q20 — authorized Next | Retained | app.c abrirProximoEpisodio/proximoPreparoPasso, next_p6.h, proximopreparo.c exact title/pair/wire ID and generation. P6 23 tests, #10 prefetch/posplay/EOF, #5 actual posplay. |
| Q21 — clean fullscreen | Retained | abrecine calado and stable-frame exit, player.c startup navigation. P2-D acceptance; physical overlay handoff. |
| Q22 — same-source recovery play | Retained | video_android.c video_p6_recovery_play_intent, reconPausado/p6SystemPaused, video_reconexao.h; PlaybackIntentP6 distinguishes user/system pause. P6 33 intent assertions; #5 ownership. |
| Q23 — missing artwork | Retained | desenharCinema black base, artwork only when valid, static text fallback; no fabricated/stretched fallback. Visual device check. |
| Q24 — measured/smoothed speed | Retained | loading_telemetry_p2.h native byte-count time/window and video_download_bps freshness; Kotlin real HTTP/range listeners. P2/P4 accepted; live endpoint check. Q46 display fix reuses this measurement. |
| Q25 — units | Retained | loading_telemetry_p2.h nv_download_texto B/s KB/s MB/s, distinct from media bitrate/diagnostic Mbps. P2/P4 evidence. |
| Q26 — delayed Change Stream | Retained | player.c cinemaEscapeDisponivel/nv_loading_change_ready, loading_handoff retains owner to veil end; P2-D evidence. |
| Q27 — Down/OK escape vs details | Retained | player.c nv_loading_change_confirm and P2-C/D input gates; actual held remote pending. |
| Q28 — seeking visuals | Retained | NvPlayer FrameHoldP2B.beyondKnownBuffer and source-owned result, player.c scrub/rail; P2-B and P4-D tests, no fake HDR conversion. |
| Q29 — synchronized A/V first frame | Retained | NvPlayer volume gate somLiberado + videoOutputFailed + AFR + route mute; rendered frame event and native stable surface gate. P5-C checks, actual ARC audio/visual sync pending. |
| Q30 — no streams | Retained | streams.c No Streams Found, reload selectable after retained phase, app manual flow; #10 addon and reload regressions. |
| Q31 — progressive results | Retained | addons.c progDrenar merges incrementally on UI thread; streams.c target/list scope. #10 pending title/manifest tests. |
| Q32 — seamless reload | Retained | streams.c p3Reload retained-only phase, nv_p3_reload_selectable, generation reject, empty/failure retirement. #10 playback_p3_reload. |
| Q33 — selector memory | Retained | streams.c saved focus/addon/search and p3RestorePending, user action precedence; P3 accepted and #10 player/manual tests. Actual remote scroll parity pending. |
| Q34 — selector-to-cinema fade | Retained | player.c desenharCinema t/0.2f entry independent from abrecine 200ms exit. P2/P3 accepted. |
| Q35 — URL freshness | Retained | fonteid_p1.h exact release comparator, app.c renewal publication and source/header identity; P1/P3 and #10 URL rotation test. No live IP/URL validity proof. |
| Q36 — original audio / fidelity | Retained | video_android.c escolherAudioPreferido/notaFonteAudio and output capability gate, NvPlayer AudioOutputProbeP5A/PCM, explicit audio error policy. P5-A/D actual compile and focused tests; #5 generation contract. Physical ARC not eARC. |
| Q37 — DV / verified HDR10 fallback | Retained | NvPlayer verifiedBase / HdrCodecFallbackP5B guard, actual format/CSD proof. P5-B 47 replay + 27 policy checks; real display pending. |
| Q38 — optional AFR off | Retained | NvPlayer FrameRateP5C preference lease/gate, P5-C setting default and 34 tests inherited; no default change in P7. Xiaomi MEMC/device modes pending. |
| Q39 — decoder fallback/error | Retained | NvPlayer guarded renderer/runtime fallback; native videoerro_p5b owned retry, modal takes precedence over automatic source switch. P5-B/D tests. |
| Q40 — dual RAM profiles | Retained | player.c player_buffer_average exact URL/header match and nv_overall_bps; buffer_profile.h nv_buffer_low strictly below threshold and unknown false; NvPlayer RamBufferProfile. P4-A and production compile; actual buffer behavior pending. |
| Q41 — validated parallel HTTP | Retained | ParaleloDataSource exact Content-Range and range-total checks, bounded queue and serial fallback. P4-B 24 tests + P4-C loopback adapter; real TorBox pending. |
| Q42 — disk VOD cache | Retained | CacheMidia uses CacheProvaP4C and CacheSpanP4C, strict representation proof, integrity and conservative miss; P4-C 10 actual Media3 adapter regressions. Storage/rotating URLs on device pending. |
| Q43 — fast seek | Retained | player.c terminarSalto/seek coalescing and native vodBufferFimMs generation, NvPlayer FrameHold and cache validation. P4-D/P4-C tests, #10 player/seek regressions. |
| Q44 — background pause | Retained | NuvioActivity task token and lifecycle methods; player.c p6 checkpoint generation/profile, app.c exact restoration, PlaybackIntentP6. P6 restore/intent tests and #5 compile; process death/device background pending. |
| Q45 — watched at credits | Retained | capmkv current-file credit bounds, watched_p6.h viewed proof, player.c p6WatchReached/Mark, exact identity/journal fallback. P6 35 tests, #10 watched/trailer/identity suites. Unproven IntroDB hints cannot mark or skip; configured reliable fallback retained. |
| Q46 — truthful diagnostics | Recheck | New numeric report scoped to account/profile and persisted via estatvivoui; audit found stale/estimated live speed and snapshot reread in old stats. Correction uses native fresh rolling measurement, unknown stays unknown, estimates labeled on other backends, historical origin separation. New focused tests and exact link/host gate in progress. |
| Q47 — stable surfaces | Retained | NvPlayer FrameHoldP2B, surface lifecycle and stable-frame gate, P5-C surface 55 assertions. Device HDR transitions may still briefly flash. |
| Q48 — cancellation/cleanup | Retained | PendingAdoptionP6, proximopreparo generation/cancel, app.c player close then discovery/intro/parental final joins. #10 full suites, 50 ASan/UBSan repetitions, #5 production link. No claim all races/leaks eliminated. |
| Q49 — audio-route recovery | Retained | NvPlayer AudioRouteObserverP5D and AudioRouteP5D, composed volume/mute gate, native capability generation and deadline. P5-D 46 tests, HDMI audio-focus guard, #5 ownership; actual ARC/BT/speaker switching pending. |
| Q50 — native post-play | Retained | posplay.c p6_postplay_eof/countdown/aguardaCena; next_p6 ownership and explicit dismissal. P6 17 policy checks, #5 actual module integration and #10 postplay/EOF regressions; physical recommendations/remote pending. |

## P7-E — all eight protected post-#134 refinement groups

| Item | Current source / actual verification | Remaining device gate |
|---|---|---|
| Parental guide | plrguia.c PG_LEFT and white line, alpha stagger/reverse choreography; parental.c stable severity sort, late publication invalidated on final shutdown. #5 panel contract and #10 join/exit tests. | Exact typography, spacing, animation and title transition on TV. |
| Landscape mobile hero | home.c heroCompactoTelefone returns mobile AND homeRetratoTelefone; TV geometry preserved in landscape. #5 player_home_polish_134 static pass. | Portrait and landscape phone actual screenshots and row positioning. |
| Single Back panel close | faixas.c early nonrepeat Back closes sheet, resets tab/edit/dropdown/repeat; #5 panel contract, #10 player Back fixtures. | Each audio/subtitle tab, edit and dropdown actual Android Back. |
| English subtitle Format A | addons.c subtitleFileName/movieReleaseName fallback, legendasui_titulo readable release/opaque ID, txt_bloco_corta_longas up to four lines, EN once and no Main pill; P7 embedded human-label slash preservation. #5 panel contract; #10 actual addon/parser/legendasui suites. | Live addon long UTF-8 filenames and actual line wrapping/truncation. |
| Font 2.5% increments | faixas.c dir*3 and 100/120 display; #5 held/font contract pass. | Physical font calibration and single increment. |
| Held OK acceleration | faixas.c ajusteRepetir 140/95/60ms throttle and 1/2/4 steps, release reset. #5 contract. | Android remote repeat event behavior and reset controls. |
| Absolute buffering | video_android.c vodBufferFimMs, callback seek generation and video_buffer_fim; player.c last endpoint; plrui.c subdued unbuffered band. P4-D actual VOD regressions; #5 rail guards and #10 player tests. P0 Android gap closed. | Real seek beyond buffer, no fictitious rail; disjoint ranges not claimed. |
| Responsive pill / exclusive focus | player.c fixed baseX, baseW+2*expanded+28 shell, focus spring and seek/time exclusion. #5 static contract and #10 navigation. | TV/phone visual animation, remote repeat and touch layout. |

Source audit of all eight groups complete; no additional demonstrated preservation defect identified. The two post-134 static contract files passed in #5; the fresh test-only gate rechecks them. Do not claim pixel parity or hardware input from static tests.

## Q46 correction and exact verification required

Observed source issue in estatvivo.c: old speed persisted on invalid/missing measurements and could be shown as current. estatvivoui.c detailed/HUD speed did not distinguish estimated throughput from real measurement; report sampled a UI-polled bytes counter, so repeat/stale snapshots could be counted as zero. Source change `9d4878898010e405c4b3a1dfc5b74c6aae1d6d25`:
- Android uses existing video_download_bps() (native opening-owned byte-rate smoothing and expiry), converting B/s to decimal kbps only for valid finite/range-bounded values. Unknown does not fall back to file bitrate or position.
- Unknown current sample clears displayed speed but preserves historical values; switching measured/estimated origins clears mixed-history averages. Detailed/HUD labels measured vs estimated, file bitrate explicitly estimate.
- Last-session numeric-only report consumes measured rates only; sampled playback/buffering/recovery semantics and privacy/schema/profile boundaries retained. Report avg/peak is sampled smoothed speed, not raw network capacity. Other backends without direct measurements remain unknown in the numeric report; existing clearly labeled estimates remain in older live statistics.
- Modified tests/estatvivo.c asserts unknown instead of stale value on counter reset, seek and buffering, preserves history and adds direct rate/unknown/no fallback/origin transition/true zero and bounds checks; tests/playback_diag_p7.c exercises unknown/zero, source transition and invalid direct rates. Existing P2 byte-meter coverage retained.
- Fresh workflows #6/#11 include both diagnostics checks; no other production behavior changed in this correction. This is a required verification of a real new source change, not an artificial retry of #10 or #5.

## P7-F — final gate / P8 device checklist

P7 source integration audit covers the signed-off 50 IDs (48 retained, one owner-deferred Q18, one corrected Q46 awaiting exact test/link evidence). P7-E eight groups reviewed. **Final acceptance cannot be issued while #6/#11 remain in progress.** Once both complete, inspect actual logs and confirm the tested SHA equals current production tree (documentation-only successors allowed); close P7 with final report/status if no new blocking evidence. Fix demonstrated failures only. No additional full audit/redundant run required for documentation-only changes.

P8 separate owner authorization required for one cached Android debug APK workflow, then test the same integrated artifact:
1. Manual Play/Resume, exact source selection and replacement, progressive reload/search/addon focus, no-results, Back and 15s Change Stream.
2. TorBox HTTP startup, status/speed truth, buffering/reconnect and slow REMUX, ElfHosted signed URL/IP renewal same-release only, audio/video first frame and distinct fades, missing artwork and reduced motion.
3. Xiaomi S Mini LED: DV/HDR10, actual hardware decoder/software fallback/error behavior, AFR OFF/default and optional supported modes, surface stability, seeks and real buffered rail. No guarantee of HDR/DV retained frame on all devices.
4. HDMI ARC soundbar, original-language compatible fidelity, PCM/passthrough and individual adjustments, route switches speaker/BT/ARC, HDMI audio focus/volume. ARC cannot be treated as eARC or guaranteed lossless TrueHD/DTS-HD MA.
5. Dual RAM profiles with reliable average bitrate around strict 30 Mbps threshold and unknown case, parallel HTTP fallback, disk storage/rotating URL cache identity and seek continuity.
6. Background/Home/Back and process/task recreation paused, same-source recovery intent, live account/profile isolation, progress/journal/credits and post-credit scenes, explicit and auto Next after actual EOF, warm/preload ownership, live TV and trailers.
7. Post-134 eight-item checklist above, full phone landscape/portrait hero and TV/phone responsive pill; D-pad focus, Back, hold OK, subtitle English Format A and typography.
8. Detailed Playback Info actual measured speed and unknown during missing telemetry, scoped last report, redacted report, physical output/thermal/unknown fields; Q18 percentage remains intentionally deferred.

No APK built, signed, published or released; no debug trigger, default branch or merge. No TV/ARC/mobile runtime claim.
