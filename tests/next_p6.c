#include "../src/next_p6.h"
#include <assert.h>
#include <stdio.h>
int main(void) {
  P6NextIntent i={0}; unsigned n=0;
#define CHECK(x) do {assert(x);++n;}while(0)
  CHECK(p6_next_decide(&i,"tt1:1:2",3,8,4,0,0)==P6_NEXT_NONE);
  CHECK(!p6_next_begin(&i,"",1,2,3,8,4));
  CHECK(!p6_next_begin(&i,"tt1:1:2",1,0,3,8,4));
  CHECK(!p6_next_begin(&i,"tt1:1:2",1,2,3,0,4));
  CHECK(p6_next_begin(&i,"tt1:1:2",1,2,3,8,4));
  CHECK(!p6_next_begin(&i,"tt1:1:3",1,3,3,8,4));
  CHECK(p6_next_decide(&i,"tt1:1:2",3,8,4,0,1)==P6_NEXT_WAIT);
  CHECK(p6_next_decide(&i,"tt1:1:2",3,8,4,0,0)==P6_NEXT_CONSUME);
  CHECK(p6_next_decide(&i,"tt1:1:2",3,8,4,1,0)==P6_NEXT_CANCEL);
  CHECK(p6_next_decide(&i,"tt1:1:1",3,8,4,0,0)==P6_NEXT_CANCEL);
  CHECK(p6_next_decide(&i,"tt1:1:2",4,8,4,0,0)==P6_NEXT_CANCEL);
  CHECK(p6_next_decide(&i,"tt1:1:2",3,9,4,0,0)==P6_NEXT_CANCEL);
  CHECK(p6_next_decide(&i,"tt1:1:2",3,8,5,0,0)==P6_NEXT_CANCEL);
  CHECK(p6_next_decide(&i,"",3,8,4,0,0)==P6_NEXT_CANCEL);
  p6_next_clear(&i);
  CHECK(p6_next_decide(&i,"tt1:1:2",3,8,4,0,0)==P6_NEXT_NONE);
  CHECK(p6_next_begin(&i,"tt1:0:1",0,1,6,10,4));
  CHECK(p6_next_player_matches(&i,"tt1:0:1",10,4));
  CHECK(!p6_next_player_matches(&i,"tt1:0:1",11,4));
  p6_next_clear(&i);
  CHECK(!p6_next_player_matches(&i,"tt1:0:1",10,4));
  printf("P6 Next cinematic ownership: %u assertions PASS\n",n);
  return 0;
}
