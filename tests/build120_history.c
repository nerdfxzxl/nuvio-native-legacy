// Reuse the discovery fixture's in-memory account and provider test doubles.
#define main originalHistorySuite
#include "cwdup244.c"
#undef main
static int simulateLongFeed;
char *dados_caminho(char *dst, unsigned tam, const char *name) {
  (void)dst; (void)tam; (void)name; return NULL;
}
int trakt_continuar_alocado(CatItem **result) {
  int count = simulateLongFeed ? 122 : CONT_MAX;
  *result = calloc((size_t)count,sizeof **result);
  if (!*result) return -1;
  if (!simulateLongFeed) return trakt_continuar(*result,count);
  for (int i=0;i<count;i++) {
    char id[24]; snprintf(id,sizeof id,"tt%07d",i%121);
    um(*result+i,id,"movie",0,0,50,10000+i);
  }
  return count;
}
int main(void) {
  simulateLongFeed=1;
  CatItem *row=NULL;
  int count=montarContinuarAlocado(&row,CAT_MAX);
  assert(count==121);
  assert(!strcmp(row[0].imdb,"tt0000000") && row[0].retomadoMs==10121);
  for (int i=1;i<count;i++) assert(row[i-1].retomadoMs>row[i].retomadoMs);
  cat_trocar_continuar(row,count);
  assert(cat_copiar_fileira("continue_watching",NULL,0,NULL)==121);
  int copied=cat_copiar_fileira("continue_watching",row,count,NULL);
  assert(copied==count && !strcmp(row[120].imdb,"tt0000001"));
  free(row);
  puts("PASS: 122 candidates merged, newest duplicate retained, all 121 distinct titles published and copied.");
}
