# Nuvio Legacy — Approved Playback Master Specification (Q01–Q50)

**Approved 2026-11-22.** **Planning/documentation only.** No feature implementation or APK workflow authorized by this document. Existing architecture is C/SDL with Android Media3 bridge. Official reference: [NuvioTV dev](https://github.com/NuvioMedia/NuvioTV/tree/dev). This document is the canonical in-repository decision register; [work packages](PLAYBACK_50_WORK_PACKAGES.md) and [progress tracker](PLAYBACK_50_IMPLEMENTATION_PROGRESS.md) govern execution.

## Mandatory precedence and invariants

1. The **latest explicit owner correction** wins over earlier options and source behavior. Do not reinterpret a signed-off choice.
2. Unwatched **Play → Dynamic Clock selector → manual stream selection → cinematic startup → actual first video frame → ~200 ms synchronized fade → fullscreen**. Saved **Resume** may reuse exactly the prior selected source. Never auto-select another addon/release or silently switch sources.
3. Preserve all unbuilt refinements documented in [PLAYER_PENDING_REFINEMENTS_AFTER_134.md](PLAYER_PENDING_REFINEMENTS_AFTER_134.md). Last device-tested artifact is **#134** per handoff; all these changes must be included and regression-tested in the *same final integrated APK* as Q1–Q50.
4. Keep original-language highest-quality audio and source, respect ARC limitations, don't silently lower track/source/resolution/HDR. Real-world unsupported formats require clear errors rather than false claims.
5. No fabricated progress %, bitrate, download speed, network stats or unsupported format capability. No unnecessary UI animation of static title artwork.
6. No feature coding or build until explicitly authorized. After implementation authorization, targeted checks may run; **APK workflow still requires separate explicit authorization**. Do not modify build-trigger files as a side effect of planning.
7. Retain Legacy native architecture, Dynamic Clock, bottom centered responsive control pill, official player icons, subtitle tabs/underline focus and D-pad rules, detailed audio track panel, individual audio reset controls, next-episode prefetch, warm cache, existing navigation and artwork behavior.

## Signed-off decision register

| ID | Decision | Acceptance requirement |
|---|---|---|
| Q01 | Cinematic look and universal loading arc | Faithful official-style backdrop, static title logo (never animate logo), shared native animated arc replacing generic spinners throughout app. |
| Q02 | Event-driven statuses | Real Preparing/Detecting/Fetching subtitles/Building/Starting/Buffering and P2P states; remove generic “Just a moment more”; real speed. |
| Q03 | ElfHosted IP recovery | Retry/renew **same selected source URL identity**, keep position/tracks, handle signed-link issues; never substitute release; 200 OK may contain error video. |
| Q04 | Intelligent stall detection | Distinguish slow progress from genuine stall; bounded recovery, protect large REMUX. |
| Q05 | First-frame handoff | Wait for real decoded/rendered first frame, not merely READY; ~200 ms overlay fade. |
| Q06 | Rebuffer | Hold last frame with small arc, not cinematic; P2P genuine readiness data. |
| Q07 | Reduced motion | Remove decorative motion, retain functional arc, statuses and real progress. |
| Q08 | Back/cancel | Cancel in-flight startup; return to appropriate detail/selector origin, suppress stale callbacks. |
| Q09 | Manual source replacement | Stop old stream immediately, cinematic new chosen source, no simultaneous audio. |
| Q10 | Optional loading details | Default minimal; OK reveals genuine bytes, buffer, peers, diagnostics where available. |
| Q11 | Failed recovery | Refresh selector, focus first available result, **do not auto-play**. |
| Q12 | Recovery vs rebuffer | Ordinary rebuffer arc only; recovery arc plus small genuine Reconnecting/Restoring text. |
| Q13 | Recovery styling | Small white text centered under arc; no background pill. |
| Q14 | Responsive controls | OK/Back/Change Stream remain operable; cancel outdated recovery. |
| Q15 | Cinematic timing | Enter immediately **after specific source accepted**; Play itself first opens selector. |
| Q16 | Scope | Full cinematic only movie/episode; trailers and Live TV keep appropriate lightweight behavior; shared arc universal. |
| Q17 | Artwork | Reuse known backdrop/logo/title immediately; background asset may arrive asynchronously; static logo never animates. |
| Q18 | HTTP vs P2P | HTTP: status/arc/measured speed, **no fake readiness bar**; P2P: actual bar, peers, buffer, clamp unready progress ~95%. |
| Q19 | Explicit stream switching | Stop old playback, retain compatible position/tracks/zoom, start user-selected replacement only. |
| Q20 | Next episode | User-authorized next episode directly starts cinematic; retain existing preloading. |
| Q21 | Clean fullscreen | Hide startup overlay and controls after first-frame handoff. |
| Q22 | Recovery resumes play | After automatic same-source recovery, resume playing even if previously paused; new user pause/Back during recovery cancels restart. **Not background resume**. |
| Q23 | Missing artwork | AMOLED black + static title + arc; no stretched/fake artwork. |
| Q24 | Speed smoothing | Real downloaded-byte measurements, ~1s refresh and 2–3s smoothing; unknown is not zero. |
| Q25 | Speed units | B/s, KB/s, MB/s (MB/s one decimal when appropriate); not confused with media Mbps. |
| Q26 | Delayed manual escape | After ~15s of loading reveal Change Stream without automatic focus. |
| Q27 | Change Stream focus | Down focuses escape; OK otherwise shows loading details. |
| Q28 | Seeking visuals | Retain last frame, arc only if needed; no black/cinematic reset. |
| Q29 | A/V first frame | Synchronized reveal, no audio ahead of video; handle audio-only/error without infinite wait. |
| Q30 | No streams | Stay selector with No Streams Found and Reload, never cinematic. |
| Q31 | Progressive results | Show addon results incrementally and retain focus identity. |
| Q32 | Seamless reload | Old cards visible until new results; honest loading, no stale-card playback. |
| Q33 | Selector state | Preserve addon filter, search, focus, scroll through reload where possible. |
| Q34 | Selector transition | ~200ms selector→cinematic crossfade; initialize player concurrently, distinct from first-frame fade. |
| Q35 | URL freshness | Selective validity checks; renew expired **same-source** URL only. |
| Q36 | Audio fidelity | Original-language highest fidelity; hardware/passthrough or compatible PCM; on impossible playback Audio Playback Error with Retry/Change Stream/Back. |
| Q37 | Dolby Vision | DV when supported; same-release HDR10 base layer only when truly available; otherwise explicit error, no quality drop. |
| Q38 | Frame-rate matching | Optional AFR **OFF by default** for Xiaomi 60Hz MEMC; supported modes only. |
| Q39 | Decoder fallback | Hardware first, quality-preserving compatible software fallback; Video Playback Error with choices; no new MPV engine. |
| Q40 | Dual RAM buffer profiles | Independent full NuvioTV-style All Streams default and Below **30 Mbps** profile. Override only when **reliable average media bitrate** strictly below editable threshold; unknown→All; never classify by live speed/file size; no thrashing/restart. |
| Q41 | Parallel HTTP | Retain ParaleloDataSource; global enable/connection count/chunk size, memory bounds and single-connection fallback for unsupported ranges. |
| Q42 | Disk VOD cache | Retain CacheMidia; global enable/auto/manual size, safety/storage controls, verify byte identity across rotating signed URLs and seek/resume. |
| Q43 | Fast seek | Coalesce D-pad seek bursts, use valid cached/buffered ranges, settle accurately, preserve A/V and tracks. |
| Q44 | Background lifecycle | Pause/save on leaving app; safe warm decoder or optional release; restore **paused**, unlike Q22. |
| Q45 | Watched at credits | Validated IntroDB/MKV chapter credits marker matched to release, otherwise configurable reliable % fallback; mark watched without stopping, protect post-credit scenes/manual overrides. |
| Q46 | Diagnostics | Optional real measured runtime stats + last-session report; label unknown; redact URLs/tokens/headers. |
| Q47 | Video surfaces | Stable surface through pause/buffer/aspect; recreate only as needed for HDR/DV/device/render failures; prevent flashes/loops. |
| Q48 | Cleanup | Preserve async release/warm cache, cancel jobs, invalidate old callbacks, clean decoder/audio/surface appropriately, save progress. |
| Q49 | Audio-route changes | Debounced HDMI ARC/BT/speaker recovery, in-place sink where possible, preserve track/effects/passthrough/PCM fidelity, error on failure; keep Legacy HDMI volume audio-focus fix. |
| Q50 | Native post-play | **Option A:** refine existing posplay.c recommendations, credits and next-episode cards; preserve preload/countdown, post-credit scenes, dismissal and unobtrusive overlay. |

## Additional protected design agreements (not to re-decide)

- Centered **large responsive glass pill** contains all bottom player action buttons, official NuvioTV icons for subtitle/audio/zoom, correct exclusive D-pad focus, anchored icon row and animated compact/expanded shell.
- Subtitle pages Subtitles/Style/Sync use animated underline instead of rounded focus background. On OK of unselected tab, activate it and immediately move focus to its page content; source/provider D-pad navigation and selected-item focus must remain correct.
- Audio Tracks show detailed information; selection leaves panel open. Audio adjustments include dB amplification, bass, night mode and center-channel mix, individual reset icons and optional persistence; no Audio Delay or Playback Speed row in that panel. Original language and fidelity first. ARC ≠ eARC.
- Preserve stream names/emojis from addons, original source identities, landscape/portrait posters, episode navigation, Dynamic Clock, watch progress, custom TV performance and warm-cache behavior.
- Preserve all eight groups of **unbuilt post-#134 fixes**: parental guide, mobile landscape hero, one-Back panel exit, subtitle Format A full filename/wrapping/English badges, 2.5% font steps, held-OK acceleration, absolute buffering rail, responsive control pill/focus. The existing handoff file is the detailed authority for those fixes.

## Critical conflicts and engineering rules

- Q15 versus manual-first Play: **source selection precedes cinematic**; direct same-source Resume can skip selector.
- Q34 selector→cinematic transition and Q05 first-frame transition are **two separate ~200ms transitions**.
- Q22 auto-recovery resumes playing versus Q44 background return stays paused; distinguish triggers, user actions during recovery take precedence.
- Q45 watched marking and Q50 recommendation timing are separate events; never cut off post-credits.
- Q40 media Mbps classification versus Q24/Q25 network MB/s telemetry are different measurements.
- Q36/Q49 fidelity versus ARC physical limits: do not promise ARC carries TrueHD/DTS-HD MA; negotiate supported passthrough or faithful compatible PCM; explicit errors when no quality-preserving route exists.
- Q35 URL renewal and Q03 IP consistency must preserve immutable source identity, prevent unintended addon/release switches.
- Q41 parallel ranges, Q42 cache and Q43 seeks must coordinate content identity and byte validity.
- Q47 surface stability during playback versus Q48 release on exit are lifecycle-specific.
- Official Kotlin/Compose visuals must be **adapted** to Legacy C/SDL; no blind copy/paste.
- Q18 P2P readiness bar must never be imitated with fake HTTP progress.
- Old unbuilt refinements and all 50 decisions share one final integrated test/build gate.

## Implementation authorization and definition of done

Execution boundaries and dependencies: [PLAYBACK_50_WORK_PACKAGES.md](PLAYBACK_50_WORK_PACKAGES.md). Exact checkpoints: [PLAYBACK_50_IMPLEMENTATION_PROGRESS.md](PLAYBACK_50_IMPLEMENTATION_PROGRESS.md). Each package must be scoped, committed, tested to the extent possible without APK, and recorded before a new chat starts. A task is not “done” merely because code was committed; distinguish **coded**, **static-tested**, **compiled**, **APK-built**, **TV-verified**. Never claim runtime parity before testing on device.

Final acceptance includes: no source substitution, accurate status/speed/P2P progress, first-frame A/V sync, HDR/DV compatibility, ARC output, stable buffer selection, correct seek/cache behavior, safe cancellation/background/resume, credits/post-credits and episode preload, D-pad focus, parental overlay, subtitle filename wrapping, responsive pill, and portrait/landscape hero. Final cached APK workflow only on explicit owner instruction.