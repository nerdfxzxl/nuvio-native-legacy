#include "armids.h"
#include <assert.h>
static int calls;
static char last[256];
char *rede_baixar_st(const char *url,int seconds,const char *const *headers,int *status) {
  (void)seconds; (void)headers;
  ++calls; snprintf(last,sizeof last,"%s",url); *status=200;
  return strdup("[{\"media\":\"TV\",\"imdb\":\"tt123\",\"kitsu\":321,\"thetvdb-season\":1}]");
}
int main(void) {
  char alt[ARM_COLS+1][ARM_ALT_TAM];
  assert(!arm_ids_stream("tt123evil:1:2","series",alt,ARM_COLS+1));
  assert(!arm_ids_stream("kitsu:123evil:2","series",alt,ARM_COLS+1));
  assert(!arm_ids_stream("tt123:1:2147483648","series",alt,ARM_COLS+1));
  assert(!arm_ids_stream("tt123:0:2","series",alt,ARM_COLS+1));
  assert(!arm_ids_stream("tt123:1:2","movie",alt,ARM_COLS+1));
  assert(!arm_ids_stream("kitsu:321:1:2","series",alt,ARM_COLS+1));
  assert(!arm_ids_stream("addon:opaque:1:2","series",alt,ARM_COLS+1));
  ArmIds a;
  assert(!arm_consultar("tmdb:123garbage","series",&a));
  assert(!arm_consultar("kitsu:123/other","series",&a));
  assert(!arm_consultar("tt123:1:2","series",&a));
  assert(calls==0);
  assert(arm_ids_stream("tt123:1:2","series",alt,ARM_COLS+1)==1);
  assert(!strcmp(alt[0],"kitsu:321:2") && calls==1 && strstr(last,"/imdb?id=tt123"));
  arm_cache_limpar();
  puts("P6 actual ARM translation: exact source, malformed targets, specials and overflow PASS");
  return 0;
}
