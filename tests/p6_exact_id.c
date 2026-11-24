#include "js.h"
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
  puts("P6 exact metadata/history identifier decode PASS");
}
