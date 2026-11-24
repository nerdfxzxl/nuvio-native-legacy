#include "../src/restore_p6.h"
#include <assert.h>
#include <stdio.h>
int main(void) {
  P6RestoreTicket t = {0}, out;
  strcpy(t.task, "d1218974-02e1-4d91-a4bf-c59e5e668c40");
  strcpy(t.account, "account"); strcpy(t.title, "kitsu:123");
  strcpy(t.type, "series"); strcpy(t.target, "kitsu:123:5");
  memset(t.fingerprint, 'a', 64); memset(t.addon, 'b', 32); memset(t.hash, 'c', 40);
  t.profile = 2; t.season = 1; t.episode = 5; t.file = 3; t.bytes = 10000;
  t.posMs = 450000; t.durMs = 1500000;
  char buf[640];
  assert(p6_restore_encode(&t, buf, sizeof buf));
  assert(p6_restore_decode(buf, &out));
  t.audioTrack = 0x987654321abcULL; t.subtitleMode = 3;
  strcpy(t.external, "a0123456789abcdef");
  assert(p6_restore_encode(&t, buf, sizeof buf));
  assert(p6_restore_decode(buf, &out));
  assert(out.audioTrack == t.audioTrack && out.subtitleMode == 3 && !strcmp(out.external, t.external));
  assert(p6_restore_duration(1500000, 1499900));
  assert(!p6_restore_duration(1500000, 1400000));
  assert(!p6_restore_duration(1500000, 30000));
  assert(out.posMs == t.posMs && out.durMs == t.durMs && !strcmp(out.target, t.target));
  assert(p6_restore_owner(&out, t.task, t.account, 2));
  assert(!p6_restore_owner(&out, "", t.account, 2));
  assert(!p6_restore_owner(&out, t.task, "another", 2));
  assert(!p6_restore_owner(&out, t.task, t.account, 1));
  assert(p6_restore_source(&out, t.fingerprint, "", "", -1, 0));
  assert(p6_restore_source(&out, "", t.addon, t.hash, 3, 10000));
  assert(!p6_restore_source(&out, "", t.addon, t.hash, 4, 10000));
  assert(!p6_restore_source(&out, "", t.addon, t.hash, 3, 9999));
  assert(!p6_restore_source(&out, "", "another", t.hash, 3, 10000));
  assert(!p6_restore_source(&out, "", t.addon, "", 3, 10000));
  assert(!p6_restore_decode("bad", &out));
  assert(!p6_restore_encode(&t, buf, 12));
  t.posMs = t.durMs;
  assert(!p6_restore_encode(&t, buf, sizeof buf));
  t.posMs = 450000; t.episode = 0;
  assert(!p6_restore_encode(&t, buf, sizeof buf));
  strcpy(t.type, "movie"); t.season = 0;
  assert(!p6_restore_encode(&t, buf, sizeof buf));
  strcpy(t.target, t.title);
  assert(p6_restore_encode(&t, buf, sizeof buf));
  assert(p6_restore_decode(buf, &out));
  strcpy(t.addon, "-"); strcpy(t.hash, "-"); t.file = -1; t.bytes = 0;
  assert(p6_restore_encode(&t, buf, sizeof buf));
  assert(p6_restore_decode(buf, &out));
  assert(!p6_restore_source(&out, "", "", "", -1, 0));
  puts("P6 paused task ticket / exact source proof: 29 assertions PASS");
}
