#include "playback_diag_p7.h"
#include <assert.h>
int main(void) {
  P7Diag d={0}; P7DiagReport read={0}; char text[256], a[96], b[96];
  p7_diag_sample(&d,1000,1,0,1,0,0,0); // initial buffering is not a rebuffer
  p7_diag_sample(&d,2000,1,1,0,0,0,1000);
  p7_diag_sample(&d,3000,1,1,0,0,0,1001000);
  p7_diag_sample(&d,4000,1,0,1,1,0,1001000);
  p7_diag_sample(&d,5000,1,0,1,1,0,1001000);
  p7_diag_sample(&d,6000,1,0,0,0,1,1001000);
  p7_diag_gap(&d); // background/retained gap must not count as playback
  p7_diag_sample(&d,60000,2,1,0,0,0,0);
  p7_diag_sample(&d,61000,2,1,0,0,0,-1); // unknown speed
  assert(d.report.samples==8 && d.report.playMs==2000);
  assert(d.report.bufferMs==1000 && d.report.buffers==1);
  assert(d.report.recoveries==1 && d.report.errors==1);
  assert(d.report.speedN==1 && d.report.speedSum==8000 && d.report.peakKbps==8000);
  assert(p7_diag_encode(&d.report,text,sizeof text));
  assert(strspn(text,"0123456789 \n")==strlen(text));
  assert(p7_diag_decode(text,&read) && !memcmp(&read,&d.report,sizeof read));
  assert(!p7_diag_decode("1 8 0 0 0 0 0 0 0 0 https://signed.invalid/token",&read));
  assert(!p7_diag_decode("1 18446744073709551616 0 0 0 0 0 0 0 0",&read));
  assert(!p7_diag_decode("1 1 0 0 0 0 0 0 1 1",&read));
  assert(p7_diag_path(a,sizeof a,"12345678-1234-1234-1234-123456789abc",1));
  assert(p7_diag_path(b,sizeof b,"12345678-1234-1234-1234-123456789abc",2) && strcmp(a,b));
  assert(p7_diag_path(b,sizeof b,"12345678-1234-1234-1234-123456789abd",1) && strcmp(a,b));
  assert(p7_diag_path(b,sizeof b,"",1) && strcmp(a,b));
  assert(!p7_diag_path(b,sizeof b,"../../secret",1));
  assert(!p7_diag_path(b,5,"",1));
  puts("PASS: P7 numeric playback diagnostics, measured speed, recovery, gaps, profile/account scope");
  return 0;
}
