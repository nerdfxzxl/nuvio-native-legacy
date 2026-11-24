#ifndef NV_PLUGIN_TARGET_P6_H
#define NV_PLUGIN_TARGET_P6_H
#include "idbase.h"
#include "stream_id_p6.h"
// Only translate a complete canonical IMDb/TMDB target. Keep opaque IDs for
// direct addon queries; never infer episode 1 or convert a special to season 1.
static inline int nv_plugin_target(const char *id, int serie, char base[24],
                                   long *tmdb, int *t, int *e) {
  *tmdb=0; *t=0; *e=0; base[0]=0;
  if (!nv_stream_id_valid(id)) return 0;
  int campos=idbase_campos_episodio(id,t,e);
  if (serie ? campos!=2 || *t<0 || *e<=0 : campos!=0) return 0;
  char b[24];
  if (!idbase_copiar_exato(id,b,sizeof b)) return 0;
  if (idbase_e_imdb(b)) {
    if (!idbase_imdb_historico(b)) return 0;
    memcpy(base,b,strlen(b)+1);
    return 1;
  }
  if (strncmp(b,"tmdb:",5)) return 0;
  const char *p=b+5;
  if (*p=='t' || *p=='m') {
    if ((*p=='t') != !!serie) return 0;
    ++p;
  }
  if (*p<'0' || *p>'9') return 0;
  long n=0;
  for (;*p;p++) {
    if (*p<'0' || *p>'9' || n>(LONG_MAX-(*p-'0'))/10) return 0;
    n=n*10+(*p-'0');
  }
  if (!n) return 0;
  *tmdb=n; return 1;
}
#endif
