#include "../src/buffer_profile.h"
#include <assert.h>
int main(void) {
  assert(nv_overall_bps("Overall bit rate : 29.999 Mb/s") == 29999000);
  assert(nv_overall_bps("Size: 40 GB\nVideo\nBit rate : 25 Mb/s") == 0);
  assert(nv_overall_bps("Overall bit rate : 29 MB/s") == 0);
  assert(nv_overall_bps("Overall bit rate : 30 Mb/s (estimated)") == 0);
  assert(nv_overall_bps("Overall bit rate : 10 Mb/s\nOverall bit rate : 31 Mb/s") == 0);
  assert(nv_overall_bps("Overall bit rate mode: Variable\r\nOverall bit rate : 30000 kb/s") == 30000000);
  assert(nv_buffer_low(29999999,30) && !nv_buffer_low(30000000,30));
  assert(!nv_buffer_low(30000001,30) && !nv_buffer_low(-1,30) && !nv_buffer_low(0,30));
  assert(nv_buffer_low(30000000,31) && !nv_buffer_low(30000000,29));
}
