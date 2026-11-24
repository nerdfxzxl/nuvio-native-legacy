#include "catalogo.h"
#include "idbase.h"
#include "cwfrente.h"
#include "progresso.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>

int prog_ler(ProgRegistro *out, int max) { (void)out; (void)max; return 0; }
int ajustes_idioma(void) { return 0; }
const char *i18n(const char *s) { return s; }
const char *dados_dir(void) { return ""; }
const char *sessao_usuario(void) { return "episode-id-512"; }
const char *desc_genero_pt(const char *s) { return s; }

static CatItem title(int parcial) {
  CatItem c = {0};
  snprintf(c.imdb, sizeof c.imdb, "tt21975436");
  snprintf(c.tipo, sizeof c.tipo, "series");
  snprintf(c.titulo, sizeof c.titulo, "Kaiju No. 8");
  c.epsParciais = parcial;
  return c;
}
static CatEp ep(int t, int e, int tvdb) {
  CatEp x = {0}; x.temporada = t; x.episodio = e; x.tvdbEp = tvdb; return x;
}
int main(void) {
  CatItem c = title(0);
  CatEp v[] = {ep(1,12,10364437), ep(2,1,10567809), ep(2,2,11209906)};
  int t = 0, e = 0;
  cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, v, 3);
  assert(cat_episodios_finais(0, c.imdb));
  assert(cat_episodio_tvdb(0, c.imdb, 2, 1) == 10567809);
  assert(cat_episodio_por_tvdb(0, c.imdb, 10567809, &t, &e) && t == 2 && e == 1);

  c = title(1); cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, v, 3);
  assert(!cat_episodios_finais(0, c.imdb));
  assert(cat_episodio_tvdb(0, c.imdb, 2, 1) == 0);
  assert(cat_episodio_tvdb_qualquer(0, c.imdb, 2, 1) == 10567809);
  assert(!cat_episodio_existe_final(0, c.imdb, 2, 1));
  assert(!cat_episodio_por_tvdb(0, c.imdb, 10567809, &t, &e));

  c = title(0); cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, v, 3);
  assert(cat_episodio_existe_final(0, c.imdb, 2, 1));
  assert(!cat_episodio_existe_final(0, c.imdb, 1, 13));

  c = title(0);
  CatEp special[] = {ep(0,1,7001)};
  cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, special, 1);
  assert(cat_episodio_tvdb(0, c.imdb, 0, 1) == 7001);
  assert(cat_episodio_por_tvdb(0, c.imdb, 7001, &t, &e) && t == 0 && e == 1);

  c = title(0);
  CatEp amb[] = {ep(1,13,10567809), ep(2,1,10567809)};
  cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, amb, 2);
  assert(cat_episodio_tvdb(0, c.imdb, 1, 13) == 0);
  assert(cat_episodio_tvdb(0, c.imdb, 2, 1) == 0);
  assert(!cat_episodio_por_tvdb(0, c.imdb, 10567809, &t, &e));
  // Exact opaque video survives provisional renumbering; ambiguity is rejected.
  CatEp opaque[] = {ep(1,13,10567809), ep(2,1,11209906)};
  snprintf(opaque[0].vid, sizeof opaque[0].vid, "addon:episode-unique");
  snprintf(opaque[1].vid, sizeof opaque[1].vid, "addon:episode-other");
  c = title(1); cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, opaque, 2);
  assert(!cat_episodio_por_video(0,c.imdb,"addon:episode-unique",&t,&e));
  c = title(0); opaque[0].temporada = 2; opaque[0].episodio = 3;
  cat_definir_tudo(&c, 1, NULL, 0); cat_definir_episodios(0, opaque, 2);
  assert(cat_episodio_por_video(0,c.imdb,"addon:episode-unique",&t,&e) && t == 2 && e == 3);
  assert(cat_episodio_por_tvdb(0,c.imdb,10567809,&t,&e) && t == 2 && e == 3);
  snprintf(opaque[1].vid, sizeof opaque[1].vid, "addon:episode-unique");
  cat_definir_episodios(0, opaque, 2);
  assert(!cat_episodio_por_video(0,c.imdb,"addon:episode-unique",&t,&e));
  char ambiguous[128];
  assert(!cat_id_stream(0,2,3,ambiguous,sizeof ambiguous) && !ambiguous[0]);
  c=title(1); CatEp numeric[]={ep(1,5,0),ep(1,6,0)};
  snprintf(numeric[0].vid,sizeof numeric[0].vid,"kitsu:123:5");
  cat_definir_tudo(&c,1,NULL,0); cat_definir_episodios(0,numeric,2);
  char vid[64]; uint64_t before=0,after=0; int tv=0;
  assert(cat_episodio_identidade(0,c.imdb,1,5,&tv,vid,sizeof vid,&before)==1);
  assert(!tv && !strcmp(vid,"kitsu:123:5") && before);
  c=title(0); numeric[0].temporada=2; numeric[0].episodio=1;
  cat_definir_tudo(&c,1,NULL,0); cat_definir_episodios(0,numeric,2);
  assert(cat_episodio_por_video(0,c.imdb,vid,&t,&e) && t==2 && e==1);
  assert(cat_episodio_identidade(0,c.imdb,2,1,&tv,vid,sizeof vid,&after)==2 && before!=after);
  assert(cat_episodio_identidade(0,c.imdb,1,5,&tv,vid,sizeof vid,&after)==-1);
  // UI and stream IDs must fit exactly, never expose a truncated alias.
  char small[8], cw[64], large[96], longid[64];
  memset(longid,'x',63); memcpy(longid,"addon:",6); longid[63]=0;
  assert(idbase_compor_episodio("kitsu:123",0,7,cw,sizeof cw));
  assert(!strcmp(cw,"kitsu:123:0:7"));
  assert(!idbase_compor_episodio(longid,123,456,cw,sizeof cw) && !cw[0]);
  assert(idbase_compor_episodio(longid,123,456,large,sizeof large));
  assert(strlen(large)==71);
  assert(!cat_id_stream(0,2,1,small,sizeof small) && !small[0]);
  assert(cat_id_stream(0,2,1,large,sizeof large) && !strcmp(large,"kitsu:123:5"));
  c=title(0); memcpy(c.imdb,longid,sizeof c.imdb);
  cat_definir_tudo(&c,1,NULL,0);
  assert(!cat_id_stream(0,1,123,cw,sizeof cw) && !cw[0]);
  assert(cat_id_stream(0,1,123,large,sizeof large) && strlen(large)==67);
  assert(!cat_id_stream(0,2,123,large,sizeof large) && !large[0]);
  assert(!cat_id_stream(0,-1,0,small,sizeof small) && !small[0]);
  assert(cw_card_episodio(&c,longid,0,456));
  assert(!strcmp(c.imdb,longid) && c.temporada==0 && c.episodio==456);
  CatEp published=ep(0,456,0);
  snprintf(published.vid,sizeof published.vid,"addon:exact-video");
  cat_definir_tudo(&c,1,NULL,0); cat_definir_episodios(0,&published,1);
  assert(cat_episodio_existe_final(0,longid,0,456));
  assert(cat_id_stream(0,c.temporada,c.episodio,cw,sizeof cw) && !strcmp(cw,"addon:exact-video"));
  assert(!cat_id_stream(0,0,457,cw,sizeof cw) && !cw[0]);
  c=title(0); published=ep(1,2,0); published.vidInvalido=1;
  cat_definir_tudo(&c,1,NULL,0); cat_definir_episodios(0,&published,1);
  assert(!cat_id_stream(0,1,2,large,sizeof large) && !large[0]);
  published.vidInvalido=0; cat_definir_episodios(0,&published,1);
  assert(cat_id_stream(0,1,2,large,sizeof large) && !strcmp(large,"tt21975436:1:2"));
  CatEp wide[]={ep(0,1,0),ep(0,2,0)};
  memset(wide[0].vid,'x',sizeof wide[0].vid-1);
  memcpy(wide[1].vid,wide[0].vid,sizeof wide[0].vid);
  wide[1].vid[sizeof wide[1].vid-2]='y';
  cat_definir_episodios(0,wide,2);
  char wire[NV_STREAM_ID_CAP];
  assert(cat_id_stream(0,0,1,wire,sizeof wire) && !strcmp(wire,wide[0].vid));
  assert(cat_id_stream(0,0,2,wire,sizeof wire) && !strcmp(wire,wide[1].vid));
  assert(cat_episodio_por_video(0,c.imdb,wire,&t,&e) && t==0 && e==2);
  assert(!cat_id_stream(0,0,1,large,sizeof large) && !large[0]);
  assert(cat_indice_stream_exato(0,c.imdb,0,2,wire)==0);
  assert(cat_indice_stream_exato(0,c.imdb,0,1,wire)==-1);
  assert(!cat_id_stream(-1,0,2,wire,sizeof wire));
  assert(!cat_id_stream(1,0,2,wire,sizeof wire));
  CatItem pair[2]={title(0),title(0)};
  strcpy(pair[0].imdb,"tt999999"); strcpy(pair[1].imdb,c.imdb);
  cat_definir_tudo(pair,2,NULL,0); cat_definir_episodios(1,wide,2);
  assert(cat_indice_stream_exato(0,c.imdb,0,2,wide[1].vid)==1);
  assert(cat_indice_stream_exato(0,pair[0].imdb,0,2,wide[1].vid)==-1);
  CatEp conflict[]={ep(1,1,0),ep(1,1,0)};
  strcpy(conflict[0].vid,"addon:one"); strcpy(conflict[1].vid,"addon:two");
  cat_definir_episodios(1,conflict,2);
  assert(!cat_id_stream(1,1,1,wire,sizeof wire));
  // A real CW composite may find its exact base metadata, while an opaque
  // title with the same numeric suffix outside CW is not that title.
  CatItem anime[2]={title(0),title(0)};
  strcpy(anime[1].imdb,"kitsu:123");
  assert(cw_card_episodio(&anime[0],anime[1].imdb,1,5));
  CatEp animeEp=ep(1,5,0); strcpy(animeEp.vid,"kitsu:123:5");
  CatFileira row={0}; strcpy(row.chave,"continue_watching"); row.n=1;
  cat_definir_tudo(anime,2,&row,1); cat_definir_episodios(1,&animeEp,1);
  assert(cat_id_stream(0,1,5,wire,sizeof wire) && !strcmp(wire,animeEp.vid));
  cat_definir_tudo(anime,2,NULL,0); cat_definir_episodios(1,&animeEp,1);
  assert(!cat_id_stream(0,1,5,wire,sizeof wire));
  puts("catalog_episode_identity512: PASS");
  return 0;
}
