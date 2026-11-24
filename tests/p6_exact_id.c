#include "js.h"
#include "stream_id_p6.h"
#include "plugin_target_p6.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>
int main(void) {
  char id[64], full[64], json[512];
  memset(full,'x',63); full[63]=0;
  snprintf(json,sizeof json,"{\"nested\":{\"id\":\"wrong\"},\"id\":\"%s\"}",full);
  assert(js_id_raiz_em(json,NULL,"id",id,sizeof id)==1 && !strcmp(id,full));
  snprintf(json,sizeof json,"{\"id\":\"%sX\"}",full);
  assert(js_id_raiz_em(json,NULL,"id",id,sizeof id)==-1 && !id[0]);
  assert(js_id_raiz_em("{\"id\":\"addon:\\u0041\\/b\"}",NULL,"id",id,sizeof id)==1 && !strcmp(id,"addon:A/b"));
  assert(js_id_raiz_em("{\"id\":\"x\\u0000y\"}",NULL,"id",id,sizeof id)==-1 && !id[0]);
  assert(js_id_raiz_em("{\"id\":\"x\\uD800y\"}",NULL,"id",id,sizeof id)==-1 && !id[0]);
  assert(js_id_raiz_em("{\"id\":\"a\",\"id\":\"b\"}",NULL,"id",id,sizeof id)==-1 && !id[0]);
  assert(js_id_raiz_em("{\"nested\":{\"id\":\"wrong\"}}",NULL,"id",id,sizeof id)==0 && !id[0]);
  assert(js_id_raiz_em("{\"id\":null}",NULL,"id",id,sizeof id)==0 && !id[0]);
  assert(js_id_raiz_em("{\"id\":123}",NULL,"id",id,sizeof id)==-1 && !id[0]);
  char a[NV_STREAM_ID_CAP], b[NV_STREAM_ID_CAP], over[NV_STREAM_ID_CAP+1], decoded[NV_STREAM_ID_CAP];
  memset(a,'x',sizeof a-1); a[sizeof a-1]=0;
  memcpy(b,a,sizeof a); b[sizeof b-2]='y';
  memset(over,'x',sizeof over-1); over[sizeof over-1]=0;
  assert(nv_stream_id_copy(decoded,sizeof decoded,a) && !strcmp(a,decoded));
  assert(nv_stream_id_copy(decoded,sizeof decoded,b) && strcmp(a,decoded));
  assert(!nv_stream_id_copy(decoded,sizeof decoded,over) && !decoded[0]);
  assert(!nv_stream_id_copy(decoded,sizeof decoded,"bad id") && !decoded[0]);
  snprintf(json,sizeof json,"{\"id\":\"%s\"}",a);
  assert(js_id_raiz_em(json,NULL,"id",decoded,sizeof decoded)==1 && !strcmp(decoded,a));
  snprintf(json,sizeof json,"{\"id\":\"%s\"}",over);
  assert(js_id_raiz_em(json,NULL,"id",decoded,sizeof decoded)==-1 && !decoded[0]);
  assert(js_id_raiz_em("{\"id\":\"\"}",NULL,"id",decoded,sizeof decoded)==-1 && !decoded[0]);
  assert(nv_stream_id_valid("addon:\xc3\xa9"));
  assert(!nv_stream_id_valid("addon:\xc0\xaf"));
  assert(!nv_stream_id_valid("addon:\xed\xa0\x80"));
  assert(!nv_stream_id_valid("addon:\xe2"));
  char base[24]; long tm; int t,e;
  assert(nv_plugin_target("tt123:0:7",1,base,&tm,&t,&e) && !strcmp(base,"tt123") && !tm && t==0 && e==7);
  assert(nv_plugin_target("tmdb:t123:2:7",1,base,&tm,&t,&e) && tm==123 && t==2 && e==7);
  assert(nv_plugin_target("tmdb:123",0,base,&tm,&t,&e) && tm==123 && !t && !e);
  assert(!nv_plugin_target("tt123",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tt123:1",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tt123abc:1:2",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tt123:2147483648:1",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tmdb:123abc:1:2",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tmdb:9999999999999999999999:1:2",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tmdb:m123:1:2",1,base,&tm,&t,&e));
  assert(!nv_plugin_target("tt123:1:2",0,base,&tm,&t,&e));
  assert(!nv_plugin_target("addon:opaque:1:2",1,base,&tm,&t,&e));
  assert(idbase_campos_episodio("tt123:2147483647:1",&t,&e)==2 && t==2147483647 && e==1);
  assert(idbase_campos_episodio("tt123:2147483648:1",&t,&e)==-1);
  puts("P6 exact metadata/history identifier decode and canonical plugin translation PASS");
}
