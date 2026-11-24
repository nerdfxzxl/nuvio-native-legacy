// Q44: an app-private, one-shot checkpoint; no raw source URL or headers.
#ifndef NV_RESTORE_P6_H
#define NV_RESTORE_P6_H
#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <ctype.h>
#include <stdint.h>
#include <limits.h>
#include "stream_id_p6.h"
#define P6_RESTORE_TEXT_CAP 768
#if NV_STREAM_ID_CAP != 128
#error Update restore target scanf width with the stream ID capacity
#endif
#define P6_RESTORE_FILE "p6-paused-session.txt"
typedef struct {
  char task[37], account[96], title[64], type[8], target[NV_STREAM_ID_CAP];
  char fingerprint[65], addon[40], hash[41];
  int profile, season, episode, file;
  long long posMs, durMs;
  unsigned long long bytes, audioTrack, subtitleTrack;
  int subtitleMode; // 0 unknown, 1 off, 2 embedded, 3 external
  char external[24];
} P6RestoreTicket;
static inline int p6_restore_hex(const char *s, unsigned n) {
  if (!s || strlen(s) != n) return 0;
  for (unsigned i = 0; i < n; i++) if (!isxdigit((unsigned char)s[i])) return 0;
  return 1;
}
static inline int p6_restore_token(const char *s) {
  if (!s || strlen(s) != 36) return 0;
  for (int i = 0; i < 36; i++) {
    if (i == 8 || i == 13 || i == 18 || i == 23) { if (s[i] != '-') return 0; }
    else if (!isxdigit((unsigned char)s[i])) return 0;
  }
  return 1;
}
static inline int p6_restore_field(const char *s, size_t cap) {
  if (!s || !*s || strlen(s) >= cap) return 0;
  for (; *s; ++s) if (isspace((unsigned char)*s) || iscntrl((unsigned char)*s)) return 0;
  return 1;
}
static inline int p6_restore_valid(const P6RestoreTicket *r) {
  if (!r || !p6_restore_token(r->task) || !p6_restore_field(r->account, sizeof r->account) ||
      !p6_restore_field(r->title, sizeof r->title) || !p6_restore_field(r->target, sizeof r->target) ||
      !p6_restore_hex(r->fingerprint, 64) || r->profile < 0 ||
      r->posMs < 0 || r->durMs < 120000 || r->durMs > INT_MAX || r->posMs >= r->durMs - 1000) return 0;
  if (r->file >= 0) {
    if (!p6_restore_hex(r->addon, 32) || !p6_restore_hex(r->hash, 40)) return 0;
  } else if (strcmp(r->addon, "-") || strcmp(r->hash, "-") || r->bytes || r->file != -1) return 0;
  if (r->subtitleMode < 0 || r->subtitleMode > 3) return 0;
  if (r->subtitleMode == 2 && !r->subtitleTrack) return 0;
  if (r->subtitleMode == 3) {
    if (r->external[0] != 'a' || !p6_restore_hex(r->external + 1, 16)) return 0;
  } else if (r->external[0] && strcmp(r->external, "-")) return 0;
  if (!strcmp(r->type, "movie")) return r->season == 0 && r->episode == 0 && !strcmp(r->title, r->target);
  return !strcmp(r->type, "series") && r->season >= 0 && r->episode > 0;
}
static inline int p6_restore_encode(const P6RestoreTicket *r, char *buf, size_t cap) {
  if (!p6_restore_valid(r)) return 0;
  int n = snprintf(buf, cap, "p6v2 %s %s %d %s %s %d %d %s %lld %lld %s %s %s %d %llu %016llx %d %016llx %s\n",
      r->task, r->account, r->profile, r->title, r->type, r->season, r->episode,
      r->target, r->posMs, r->durMs, r->fingerprint, r->addon, r->hash, r->file, r->bytes,
      r->audioTrack, r->subtitleMode, r->subtitleTrack, r->external[0] ? r->external : "-");
  return n > 0 && (size_t)n < cap;
}
static inline int p6_restore_decode(const char *s, P6RestoreTicket *r) {
  int n = 0;
  if (!s || !r || strlen(s) >= P6_RESTORE_TEXT_CAP) return 0;
  memset(r, 0, sizeof *r);
  // Numeric bounds are checked below; width also bounds conversion input.
  return sscanf(s, "p6v2 %36s %95s %9d %63s %7s %9d %9d %127s %10lld %10lld %64s %39s %40s %9d %19llu %16llx %1d %16llx %23s%n",
      r->task, r->account, &r->profile, r->title, r->type, &r->season,
      &r->episode, r->target, &r->posMs, &r->durMs, r->fingerprint, r->addon,
      r->hash, &r->file, &r->bytes, &r->audioTrack, &r->subtitleMode,
      &r->subtitleTrack, r->external, &n) == 19 && n > 0 &&
      !strcmp(s + n, "\n") && p6_restore_valid(r);
}
static inline int p6_restore_owner(const P6RestoreTicket *r, const char *task,
                                   const char *account, int profile) {
  return p6_restore_valid(r) && task && account && !strcmp(r->task, task) &&
      !strcmp(r->account, account) && r->profile == profile;
}
// Reject an incompatible timeline before restoring transport intent.
static inline int p6_restore_duration(long long expected, long long actual) {
  if (expected < 120000 || actual < 120000) return 0;
  long long tolerance = expected / 50;
  if (tolerance < 5000) tolerance = 5000;
  return actual >= expected - tolerance && actual <= expected + tolerance;
}
// Only the exact digest or a known torrent file in the same addon is proof.
// A filename, group, resolution or approximate size is never proof.
static inline int p6_restore_source(const P6RestoreTicket *r, const char *digest,
                                     const char *addon, const char *hash, int file,
                                     unsigned long long bytes) {
  if (!p6_restore_valid(r)) return 0;
  if (digest && p6_restore_hex(digest, 64) && !strcmp(r->fingerprint, digest)) return 1;
  return r->file >= 0 && file == r->file && addon && hash &&
      !strcmp(r->addon, addon) && !strcasecmp(r->hash, hash) &&
      (!r->bytes || !bytes || r->bytes == bytes);
}
#endif
