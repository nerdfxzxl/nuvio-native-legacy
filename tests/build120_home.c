#include "home.c"
#include <assert.h>
static CatItem cards[24];
static CatFileira shelves[3];
static int total=24;
int cat_n(void) { return total; }
int cat_n_fileiras(void) { return 3; }
const CatFileira *cat_fileira(int i) { return i>=0 && i<3 ? &shelves[i] : NULL; }
const CatItem *cat_item(int i) { return i>=0 && i<total ? &cards[i] : NULL; }
int ajustes_ocultar_filmes_vistos(void) { return 0; }
int cat_visto(const CatItem *ci) { (void)ci;return 0; }
const char *fil_hero_fonte(void) { return "@manual"; }
int fil_hero_selected(const char *key) { return key[0]=='r'; }
Uint64 SDL_GetPerformanceCounter(void) { return 12345; }
int amigosfil_indice_cat(int col) { (void)col; return -1; }
static void longRow(void) {
  static int indices[620];
  Fileira row={0}; row.n=620; row.historyIndices=indices;
  for (int i=0;i<row.n;i++) {
    indices[i]=row.n-i;
    assert(fileiraItemIndice(&row,i)==620-i);
    int slot=historySlot(0,i);
    assert(slot>=0 && slot<MAX_CARDS);
    assert(animFoco[0][slot]==0 && expansion[0][slot]==0);
    animFoco[0][slot]=expansion[0][slot]=1;
  }
  int slot=historySlot(0,17);
  assert(animFoco[0][slot]==0 && expansion[0][slot]==0);
  assert(fileiraItemIndice(&row,-1)==-1 && fileiraItemIndice(&row,620)==-1);
  puts("PASS: 620 history indices with bounded, resettable focus animation slots.");
}
int main(void) {
  longRow();
  for (int i=0;i<24;i++) snprintf(cards[i].imdb,sizeof cards[i].imdb,"tt%07d",i+1);
  for (int i=0;i<3;i++) {
    snprintf(shelves[i].chave,sizeof shelves[i].chave,"r%d",i);
    shelves[i].ini=8*i; shelves[i].n=8;
  }
  strcpy(cards[8].imdb,cards[0].imdb);
  heroSemente=12345; heroMontarSet(); assert(heroSetN==10);
  int perRow[3]={0}; char first[10][64];
  for (int i=0;i<10;i++) {
    strcpy(first[i],cards[heroSet[i]].imdb); perRow[heroSet[i]/8]++;
    for (int j=0;j<i;j++) assert(strcmp(first[j],first[i]));
  }
  for (int r=0;r<3;r++) assert(perRow[r]>=3 && perRow[r]<=4);
  for (int r=0;r<3;r++) for (int j=0;j<4;j++) {
    CatItem tmp=cards[8*r+j]; cards[8*r+j]=cards[8*r+7-j]; cards[8*r+7-j]=tmp;
  }
  heroMontarSet(); for (int i=0;i<10;i++) assert(!strcmp(first[i],cards[heroSet[i]].imdb));
  for (int r=0;r<3;r++) shelves[r].n=1;
  heroMontarSet(); assert(heroSetN<=3 && heroSetN>0);
  printf("PASS: ten unique titles split 3/3/4, stable identity after metadata reordering, short pool handled.\n");
}
