#include "../src/fonteid_p1.h"
#include <assert.h>
#include <stdio.h>
static NvFonteId id(const char *addon, const char *hash, int index,
                    const char *file, const char *title, uint64_t bytes, const char *group) {
  NvFonteId i = {addon,hash,index,file,title,bytes,group}; return i;
}
int main(void) {
  NvFonteId a=id("addon-a","ABCD",2,"","release",10000,"g");
  NvFonteId b=id("addon-a","abcd",2,"","release",10000,"g");
  assert(nv_fonte_mesma_liberacao(&a,&b)); // URL token not an input
  b.addon="addon-b"; assert(!nv_fonte_mesma_liberacao(&a,&b));
  b.addon="addon-a"; b.fileIdx=3; assert(!nv_fonte_mesma_liberacao(&a,&b));
  b.fileIdx=2; b.bytes=9000; assert(!nv_fonte_mesma_liberacao(&a,&b));
  a=id("addon-a","",0,"film.mkv","film",0,"");
  b=id("addon-a","",0,"other.mkv","film",0,"");
  assert(!nv_fonte_mesma_liberacao(&a,&b));
  b.arquivo="film.mkv"; assert(nv_fonte_mesma_liberacao(&a,&b));
  a=id("addon-a","",0,"","same",0,"");
  b=id("addon-a","",0,"","same",0,"");
  assert(!nv_fonte_mesma_liberacao(&a,&b));
  a.bytes=999; b.bytes=999; assert(nv_fonte_mesma_liberacao(&a,&b));
  b.bytes=0; assert(!nv_fonte_mesma_liberacao(&a,&b));
  a.bytes=0; a.grupo="set-1"; b.grupo="set-1";
  assert(nv_fonte_mesma_liberacao(&a,&b));
  b.grupo="set-2"; assert(!nv_fonte_mesma_liberacao(&a,&b));
  puts("P1 immutable release identity: PASS");
  return 0;
}
