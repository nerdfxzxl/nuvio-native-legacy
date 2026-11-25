#ifndef NV_PLAYBACK_DIAG_P7_H
#define NV_PLAYBACK_DIAG_P7_H
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#include <ctype.h>

/* Fixed numeric schema: no stream URL, header, token, title, host, or filename
 * can enter this report. Measurements are sampled, not estimated bandwidth. */
typedef struct {
  unsigned long long samples, playMs, bufferMs, buffers, recoveries, errors;
  unsigned long long speedN, speedSum, peakKbps;
} P7DiagReport;
typedef struct {
  P7DiagReport report;
  unsigned lastMs, source;
  long long bytes;
  int have, played, playing, buffering, recovering, failed;
} P7Diag;

static inline void p7_diag_gap(P7Diag *d) { d->have = 0; }
static inline void p7_diag_sample(P7Diag *d, unsigned now, unsigned source,
                                 int playing, int buffering, int recovering,
                                 int failed, long long bytes) {
  unsigned dt = now - d->lastMs;
  int same = d->have && d->source == source && dt <= 2000u;
  P7DiagReport *r = &d->report;
  playing = !!playing && !buffering && !recovering && !failed;
  buffering = !!buffering;
  recovering = !!recovering;
  failed = !!failed;
  if (same && playing && d->playing) r->playMs += dt;
  if (same && d->played && buffering && d->buffering) r->bufferMs += dt;
  if (d->played && buffering && (!same || !d->buffering)) r->buffers++;
  if (recovering && (!same || !d->recovering)) r->recoveries++;
  if (failed && (!same || !d->failed)) r->errors++;
  if (same && dt >= 250u && playing && d->playing && bytes >= 0 &&
      d->bytes >= 0 && bytes >= d->bytes) {
    long long delta = bytes - d->bytes;
    if (delta <= 1000000000LL) {
      unsigned long long kbps = (unsigned long long)delta * 8u / dt;
      if (kbps <= 4000000u && r->speedSum <= ULLONG_MAX - kbps) {
        r->speedSum += kbps; r->speedN++;
        if (kbps > r->peakKbps) r->peakKbps = kbps;
      }
    }
  }
  r->samples++;
  d->played |= playing;
  d->lastMs = now; d->source = source; d->bytes = bytes;
  d->have = 1; d->playing = playing; d->buffering = buffering;
  d->recovering = recovering; d->failed = failed;
}

static inline int p7_diag_encode(const P7DiagReport *r, char *out, size_t n) {
  int k = snprintf(out,n,"1 %llu %llu %llu %llu %llu %llu %llu %llu %llu\n",
      r->samples,r->playMs,r->bufferMs,r->buffers,r->recoveries,r->errors,
      r->speedN,r->speedSum,r->peakKbps);
  return k > 0 && (size_t)k < n;
}
static inline int p7_diag_decode(const char *s, P7DiagReport *out) {
  unsigned long long a[10];
  if (!s || !out || strlen(s) >= 256) return 0;
  for (size_t i=0;i<10;i++) {
    char *end;
    while (*s == ' ' || *s == '\n') s++;
    if (*s < '0' || *s > '9') return 0;
    errno=0; a[i]=strtoull(s,&end,10);
    if (errno || end == s || (*end && *end != ' ' && *end != '\n')) return 0;
    s=end;
  }
  while (*s == ' ' || *s == '\n') s++;
  if (*s || a[0]!=1 || !a[1] || a[4]>a[1] || a[5]>a[1] || a[6]>a[1] ||
      a[7]>a[1] || a[9]>4000000u || (!a[7] && (a[8] || a[9])) ||
      (a[7] && (a[8]/a[7]>a[9] || a[8]<a[9]))) return 0;
  *out=(P7DiagReport){a[1],a[2],a[3],a[4],a[5],a[6],a[7],a[8],a[9]};
  return 1;
}

/* Local storage scope only; the account key is never included in the report
 * or displayed/logged. Reject anything except the expected account UUID. */
static inline int p7_diag_path(char *out, size_t n, const char *account, int profile) {
  if (!out || !n || !account || profile <= 0) return 0;
  char key[37];
  if (!*account) strcpy(key,"guest");
  else {
    if (strlen(account)!=36) return 0;
    for (size_t i=0;i<36;i++) {
      if (i==8 || i==13 || i==18 || i==23) {
        if (account[i]!='-') return 0;
      } else if (!isxdigit((unsigned char)account[i])) return 0;
      key[i]=(char)tolower((unsigned char)account[i]);
    }
    key[36]=0;
  }
  int k=snprintf(out,n,"playback-last-%s-p%d.txt",key,profile);
  return k>0 && (size_t)k<n;
}
#endif
