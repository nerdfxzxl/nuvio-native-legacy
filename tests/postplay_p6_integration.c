// Actual posplay.c, catalogue and episode ordering; fake backend/worker boundaries.
#include "posplay.h"
#include "catalogo.h"
#include "progresso.h"
#include "player.h"
#include "video.h"
#include "fontecache.h"
#include "proximopreparo.h"
#include "intro.h"
#include "ajustes.h"
#include "extras.h"
#include "descoberta.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

int prog_ler(ProgRegistro *out,int max){(void)out;(void)max;return 0;}
int ajustes_idioma(void){return 0;}
const char *i18n(const char *s){return s;}
const char *dados_dir(void){return "";}
const char *sessao_usuario(void){return "p6-posplay-fixture";}
const char *desc_genero_pt(const char *s){return s;}
static int ended, active=1, epT=1, epE=1, autoplay=1, seconds=5, related=1, window=1, learned, cancelled;
static double backendPos, marker;
int video_ativo(void){return active;}
int video_terminou(void){return ended;}
double video_pos(void){return backendPos;}
double video_creditos(void){return marker;}
double intro_creditos_seg(void){return 0;}
double intro_creditos_janela_filme(double d){return fmin(d*.5,fmin(INTRO_CRED_FILME_MAX_S,fmax(INTRO_CRED_FILME_MIN_S,d*INTRO_CRED_FILME_FRAC)));}
double intro_fim_estimado_filme(double d){return d<INTRO_FILME_MIN_S?0:d<INTRO_FILME_CURTO_ATE_S?INTRO_FIM_FILME_CURTO_S:INTRO_FIM_FILME_S;}
void player_episodio_atual(int *t,int *e){*t=epT;*e=epE;}
int player_velocidade_efetiva(void){return 100;}
int player_janela_proximo(double a){(void)a;return window;}
void player_aprender_creditos(void){learned++;}
int ajustes_proximo_auto(void){return autoplay;}
int ajustes_proximo_contagem(void){return seconds;}
int ajustes_fonte_manual(void){return 1;}
int ajustes_fonte_preparar(void){return 0;}
int ajustes_fonte_precarregar(void){return 0;}
void addons_capturar_escopo(FontecacheEscopo *s){memset(s,0,sizeof *s);}
void fontecache_cancelar_proximo(void){}
void fontecache_vod_apagar(const char *a,const char *b,const char *c,const FontecacheEscopo *s){(void)a;(void)b;(void)c;(void)s;}
int fontecache_vod_pegar(const char *a,const char *b,const char *c,const FontecacheEscopo *s,Stream **l,int *n,Uint32 *age){(void)a;(void)b;(void)c;(void)s;*l=NULL;*n=0;*age=0;return 0;}
int fontecache_precarregar_proximo(const char *a,const FontecacheEscopo *s){(void)a;(void)s;assert(0);return 0;}
void proximopreparo_passo(const char *s){(void)s;}
void proximopreparo_cancelar(void){cancelled++;}
int proximopreparo_agendar(int i,int t,int e,const char *a,const char *b,const FontecacheEscopo *s){(void)i;(void)t;(void)e;(void)a;(void)b;(void)s;assert(0);return 0;}
int proximopreparo_renovar(const char *a,const char *b){(void)a;(void)b;assert(0);return 0;}
int extras_n_relacionados(void){return related;}
const char *extras_relacionado_imdb(int i){(void)i;return "tt1234567";}
const char *extras_relacionado_titulo(int i){(void)i;return "Fixture";}
const char *extras_relacionado_ano(int i){(void)i;return "2026";}
const char *extras_relacionado_poster(int i){(void)i;return "";}
void desc_pedir_titulo_semente(const char *a,long b,const char *c,const char *d,const char *e,const char *f){(void)a;(void)b;(void)c;(void)d;(void)e;(void)f;assert(0);}
static void frame(Uint32 now,double pos,int eof,int series,int win){
  ended=eof;backendPos=pos;window=win;
  posplay_atualizar(.016f,now,pos,3600,series,0,win);
}
static void setup(int series,int scene,int season){
  CatItem c={0}; CatEp eps[2]={{0}};
  strcpy(c.imdb,"tt1234567");strcpy(c.tipo,series?"series":"movie");
  epT=season;epE=1;c.temporada=epT;c.episodio=epE;c.posCreditos=scene;
  eps[0].temporada=eps[1].temporada=season;eps[0].episodio=1;eps[1].episodio=2;
  cat_definir_tudo(&c,1,NULL,0);if(series)cat_definir_episodios(0,eps,2);
  posplay_ciclo_evento(SDL_APP_DIDENTERFOREGROUND);posplay_fechar();
  ended=0;active=1;backendPos=0;marker=0;related=1;autoplay=1;seconds=5;
  for(int i=0;i<11;i++)posplay_atualizar(1,100,0,3600,series,0,0);
}
static int key(int k){SDL_Event e={0};e.type=SDL_KEYDOWN;e.key.keysym.sym=k;return posplay_evento(&e);}
static int next(int season){int t,e;int n=posplay_pediu_episodio(&t,&e);if(n)assert(t==season&&e==2);return n;}
int main(void){
  setup(1,0,1);frame(1000,3500,0,1,1);assert(posplay_visivel());
  frame(12000,3600,0,1,1);assert(posplay_visivel()&&!next(1));
  frame(13000,3599.2,1,1,1);assert(posplay_visivel()&&!next(1));
  frame(17999,3600,1,1,1);assert(!next(1));frame(18000,3600,1,1,1);
  assert(!posplay_visivel()&&next(1)&&!next(1));
  setup(1,0,0);seconds=12;frame(1000,3600,1,1,0);assert(posplay_visivel());
  frame(12999,3600,1,1,0);assert(!next(0));frame(13000,3600,1,1,0);
  assert(next(0)&&!next(0));
  setup(1,0,1);frame(1000,3500,0,1,1);assert(key(SDLK_AC_BACK)==1);
  frame(2000,3600,1,1,0);frame(10000,3600,1,1,0);
  assert(!posplay_visivel()&&!next(1)&&!posplay_aguarda_escolha());
  setup(1,1,1);frame(1000,3500,0,1,1);assert(key(SDLK_RETURN)==1);
  posplay_aguardar_busca(1);frame(3000,3600,1,1,1);assert(!posplay_visivel()&&!next(1));
  posplay_aguardar_busca(0);frame(4000,3600,1,1,1);assert(posplay_visivel()&&!next(1));
  frame(9000,3600,1,1,1);assert(next(1));
  setup(1,0,1);frame(1000,3600,1,1,1);
  posplay_ciclo_evento(SDL_APP_WILLENTERBACKGROUND);assert(!key(SDLK_RETURN));
  posplay_ciclo_evento(SDL_APP_DIDENTERFOREGROUND);frame(20000,3600,1,1,1);
  assert(posplay_visivel()&&!next(1));assert(key(SDLK_RETURN)==1&&next(1)&&!next(1));
  setup(1,0,1);frame(1000,3600,1,1,1);posplay_recuar();frame(10000,3600,1,1,1);
  assert(posplay_visivel()&&!next(1));frame(11000,1000,0,1,0);frame(12000,3600,1,1,1);
  frame(17000,3600,1,1,1);assert(next(1));
  setup(1,0,1);autoplay=0;frame(1000,3600,1,1,1);frame(20000,3600,1,1,1);
  assert(posplay_visivel()&&!next(1));assert(key(SDLK_RETURN)==1&&next(1));
  setup(0,0,0);frame(1000,3600,0,0,0);assert(!posplay_visivel());
  frame(2000,3600,1,0,0);assert(posplay_visivel()&&!posplay_sobre_video());
  assert(key(SDLK_AC_BACK)==1);frame(3000,3600,1,0,0);assert(!posplay_visivel());
  setup(0,0,0);related=0;frame(1000,3600,1,0,0);assert(!posplay_visivel());
  setup(0,1,0);frame(1000,3500,0,0,0);assert(posplay_visivel()&&posplay_sobre_video());
  assert(key(SDLK_RETURN)==1&&posplay_pediu_titulo()<0);
  frame(2000,3600,0,0,0);assert(!posplay_visivel());
  frame(3000,3600,1,0,0);assert(posplay_visivel()&&!posplay_sobre_video());
  assert(key(SDLK_RETURN)==1&&posplay_pediu_titulo()==0&&posplay_pediu_titulo()<0);
  assert(cancelled>0&&learned>0);
  puts("P6 actual posplay module: EOF, credits, dismissal, Home, seek, specials and movie recommendation PASS");
  return 0;
}
