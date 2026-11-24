#ifndef NV_STREAM_ID_P6_H
#define NV_STREAM_ID_P6_H
#include <stddef.h>
#include <string.h>
// 127 wire bytes plus NUL; title storage remains unchanged.
#define NV_STREAM_ID_CAP 128
static inline int nv_stream_id_valid(const char *id) {
  if (!id) return 0;
  size_t i=0;
  while (i<NV_STREAM_ID_CAP && id[i]) {
    unsigned char c=(unsigned char)id[i];
    if(c<=0x20 || c==0x7f) return 0;
    if(c<0x80) { ++i; continue; }
    unsigned n=c>=0xc2 && c<=0xdf ? 2 : c>=0xe0 && c<=0xef ? 3 : c>=0xf0 && c<=0xf4 ? 4 : 0;
    if(!n || i+n>=NV_STREAM_ID_CAP) return 0;
    for(unsigned j=1;j<n;j++) {
      unsigned char b=(unsigned char)id[i+j];
      if(b<0x80 || b>0xbf) return 0;
      if(j==1 && ((c==0xe0 && b<0xa0)||(c==0xed && b>0x9f)||(c==0xf0 && b<0x90)||(c==0xf4 && b>0x8f))) return 0;
    }
    i+=n;
  }
  return i>0 && i<NV_STREAM_ID_CAP;
}
static inline int nv_stream_id_copy(char *dst,size_t cap,const char *id) {
  if (!dst || !cap) return 0;
  if (!nv_stream_id_valid(id) || strlen(id)>=cap) { dst[0]=0; return 0; }
  memmove(dst,id,strlen(id)+1); return 1;
}
#endif
