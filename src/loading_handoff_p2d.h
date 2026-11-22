#ifndef NV_LOADING_HANDOFF_P2D_H
#define NV_LOADING_HANDOFF_P2D_H

/* P2-D: Media readiness, hardware-plane readiness and cinematic navigation
 * are DIFFERENT states. An elapsed playback clock is never a rendered frame.
 * Kept independent of SDL/Android so the actual predicates can be host-tested.
 */
static inline int nv_p2d_media_ready(int ready, int rendered_frame, int confirmed_audio_only) {
  return !!ready && (!!rendered_frame || !!confirmed_audio_only);
}

static inline int nv_p2d_visual_ready(int frame_on_current_surface, int surface_stable,
                                      int confirmed_audio_only) {
  return !!confirmed_audio_only || (!!frame_on_current_surface && !!surface_stable);
}

static inline int nv_p2d_loading_owner(int waiting_source, int has_video, int media_ready,
                                       int cinematic_pending, int is_live,
                                       int source_error, int exiting) {
  return !!waiting_source || (!!has_video && !media_ready) ||
         (!is_live && !source_error && !exiting && !!cinematic_pending);
}

#endif
