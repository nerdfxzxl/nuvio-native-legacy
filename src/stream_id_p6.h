#ifndef NV_STREAM_ID_P6_H
#define NV_STREAM_ID_P6_H
#include <stddef.h>
#include <string.h>
// 127 wire bytes plus NUL; title storage remains unchanged.
#define NV_STREAM_ID_CAP 128
static inline int nv_stream_id_valid(const char *id) {
  if (!id) return 0;
  size_t i=0;
  for (; i<NV_STREAM_ID_CAP && id[i]; ++i) {
    unsigned char c=(unsigned char)id[i];
    if(c<=0x20 || c==0x7f) return 0;
  }
  return i>0 && i<NV_STREAM_ID_CAP;
}
static inline int nv_stream_id_copy(char *dst,size_t cap,const char *id) {
  if (!dst || !cap) return 0;
  if (!nv_stream_id_valid(id) || strlen(id)>=cap) { dst[0]=0; return 0; }
  memmove(dst,id,strlen(id)+1); return 1;
}
#endif
