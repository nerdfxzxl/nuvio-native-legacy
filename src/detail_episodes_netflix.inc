// Netflix-inspired full-screen episode selector, drawn by Legacy's native
// renderer. No Compose view tree, no network/bitmap work in the frame loop.
// Included by detail.c after episodioPadrao and before detail_evento_.
#ifndef NV_DETAIL_EPISODES_NETFLIX_INC
#define NV_DETAIL_EPISODES_NETFLIX_INC

static int detEpAberto, detEpSzn, detEpRow, detEpCol;
static int detEpSeasons[CAT_TEMP_MAX], detEpCounts[CAT_TEMP_MAX], detEpNSeasons;
static int detEpRowIndexes[2600], detEpRows;
// Episode bytes and catalog item metadata have separate atomic clocks.
// Season metadata can arrive after the episode list without bumping epVersao.
static unsigned detEpCatalogVersion, detEpItemRevision;
static float detEpScroll, detEpVel, detEpSeasonScroll, detEpSeasonVel;
static int detEpShowDesc = 1, detEpShowRating = 1;

static void detEpRebuild(int targetSeason, int targetEpisode) {
  const CatItem *ci = cat_item(idx);
  int n = cat_n_episodios(idx);
  detEpNSeasons = 0;
  memset(detEpCounts, 0, sizeof detEpCounts);
  for (int i = 0; ci && i < ci->nTemporadas; i++) {
    int season = ci->temporadas[i], exists = 0;
    if (season <= 0) continue;
    for (int j = 0; j < detEpNSeasons; j++)
      if (detEpSeasons[j] == season) exists = 1;
    if (!exists && detEpNSeasons < CAT_TEMP_MAX)
      detEpSeasons[detEpNSeasons++] = season;
  }
  // Some catalogs contain episode rows before their season metadata arrives.
  for (int i = 0; i < n; i++) {
    const CatEp *ep = cat_episodio(idx, i);
    if (!ep || ep->temporada <= 0) continue;
    int found = 0;
    for (int j = 0; j < detEpNSeasons; j++)
      if (detEpSeasons[j] == ep->temporada) { found = 1; break; }
    if (!found && detEpNSeasons < CAT_TEMP_MAX)
      detEpSeasons[detEpNSeasons++] = ep->temporada;
  }
  // Sort ascending; never reproduce Netflix's descending-season screenshot bug.
  for (int i = 1; i < detEpNSeasons; i++) {
    int current = detEpSeasons[i], j = i;
    while (j && detEpSeasons[j - 1] > current) {
      detEpSeasons[j] = detEpSeasons[j - 1]; j--;
    }
    detEpSeasons[j] = current;
  }
  // Season counts are metadata; cache them once, not O(seasons × episodes)
  // every 60 Hz frame, especially for very long anime series.
  for (int i = 0; i < n; i++) {
    const CatEp *ep = cat_episodio(idx, i);
    if (!ep) continue;
    for (int j = 0; j < detEpNSeasons; j++)
      if (detEpSeasons[j] == ep->temporada) { detEpCounts[j]++; break; }
  }
  if (!detEpNSeasons) { detEpSzn = detEpRows = detEpRow = 0; return; }
  detEpSzn = 0;
  for (int j = 0; j < detEpNSeasons; j++)
    if (detEpSeasons[j] == targetSeason) { detEpSzn = j; break; }
  detEpRows = 0;
  for (int i = 0; i < n; i++) {
    const CatEp *ep = cat_episodio(idx, i);
    if (!ep || ep->temporada != detEpSeasons[detEpSzn]) continue;
    if (detEpRows < (int)(sizeof detEpRowIndexes / sizeof *detEpRowIndexes))
      detEpRowIndexes[detEpRows++] = i;
  }
  // The Cinemeta list is usually ordered; tolerate mixed/unsorted sources.
  for (int i = 1; i < detEpRows; i++) {
    int current = detEpRowIndexes[i], j = i;
    const CatEp *curEp = cat_episodio(idx, current);
    while (j) {
      const CatEp *prev = cat_episodio(idx, detEpRowIndexes[j - 1]);
      if (!prev || !curEp || prev->episodio <= curEp->episodio) break;
      detEpRowIndexes[j] = detEpRowIndexes[j - 1]; j--;
    }
    detEpRowIndexes[j] = current;
  }
  detEpRow = 0;
  for (int j = 0; j < detEpRows; j++) {
    const CatEp *ep = cat_episodio(idx, detEpRowIndexes[j]);
    if (ep && ep->episodio == targetEpisode) { detEpRow = j; break; }
  }
  detEpCatalogVersion = cat_versao_episodios();
  detEpItemRevision = cat_revisao_itens();
}

static void detEpAbrir(void) {
  int t = 1, e = 1, origin = 0;
  if (!ehSerie()) return;
  episodioPadrao(&t, &e, &origin);
  detEpRebuild(t, e);
  detEpCol = 1; // Focus on currently watching/next, not the season list.
  detEpShowDesc = ajustes_det_ep_descricao();
  detEpShowRating = ajustes_det_ep_imdb();
  detEpScroll = fmaxf(0.0f, (float)(detEpRow - 1) * 207.0f);
  detEpVel = 0;
  detEpSeasonScroll = fmaxf(0.0f, (float)(detEpSzn - 3) * 88.0f);
  detEpSeasonVel = 0;
  detEpAberto = 1;
  // A full-screen episode view must never continue an autoplay trailer.
  if (trailer_aberto()) trailer_fechar();
}

static const CatEp *detEpAt(int row) {
  return row >= 0 && row < detEpRows
    ? cat_episodio(idx, detEpRowIndexes[row]) : NULL;
}
static void detEpAtualizar(float dt) {
  if (!detEpAberto) return;
  if (detEpCatalogVersion != cat_versao_episodios() ||
      detEpItemRevision != cat_revisao_itens()) {
    int season = detEpNSeasons ? detEpSeasons[detEpSzn] : 1;
    int episode = detEpAt(detEpRow) ? detEpAt(detEpRow)->episodio : 1;
    detEpRebuild(season, episode);
  }
  const float step = 207.0f, seasonStep = 88.0f;
  float maxScroll = fmaxf(0.0f, (float)detEpRows * step - 850.0f);
  float target = fmaxf(0.0f, ((float)detEpRow - 1.0f) * step);
  if (target > maxScroll) target = maxScroll;
  detEpScroll = anim_mola2(&detEpVel, detEpScroll, target, dt, NV_MOLA2_SCROLL);
  float maxSeason = fmaxf(0.0f, (float)detEpNSeasons * seasonStep - 785.0f);
  float targetSeason = fmaxf(0.0f, ((float)detEpSzn - 3.0f) * seasonStep);
  if (targetSeason > maxSeason) targetSeason = maxSeason;
  detEpSeasonScroll = anim_mola2(&detEpSeasonVel, detEpSeasonScroll,
                                 targetSeason, dt, NV_MOLA2_SCROLL);
}

static int detEpEvento(const SDL_Event *ev) {
  if (!detEpAberto) return 0;
  if (ev->type != SDL_KEYDOWN) return 1;
  if (ev->key.repeat && ev->key.keysym.sym == SDLK_RETURN) return 1;
  SDL_Keycode key = ev->key.keysym.sym;
  if (key == SDLK_ESCAPE || key == SDLK_AC_BACK ||
      key == SDLK_BACKSPACE || key == SDLK_DELETE ||
      ev->key.keysym.scancode == NV_SCANCODE_BACK) {
    detEpAberto = 0;
    return 1;
  }
  if (key == SDLK_LEFT) detEpCol = 0;
  if (key == SDLK_RIGHT) detEpCol = 1;
  if (key == SDLK_UP || key == SDLK_DOWN) {
    int direction = key == SDLK_DOWN ? 1 : -1;
    if (detEpCol == 0) {
      int next = detEpSzn + direction;
      if (next >= 0 && next < detEpNSeasons) {
        detEpRebuild(detEpSeasons[next], 1);
        detEpScroll = detEpVel = 0;
      }
    } else {
      int next = detEpRow + direction;
      if (next >= 0 && next < detEpRows) detEpRow = next;
    }
  }
  if (key == SDLK_RETURN || key == SDLK_KP_ENTER) {
    if (detEpCol == 0) { detEpCol = 1; return 1; }
    const CatEp *ep = detEpAt(detEpRow);
    if (ep && ep->temporada > 0 && ep->episodio > 0 && !epNaoExibido(ep)) {
      // Capture the exact selected episode before Dynamic Clock steals focus.
      pedFontesT = ep->temporada; pedFontesE = ep->episodio;
      pedFontes = 1;
      detEpAberto = 0;
    }
  }
  return 1;
}

static void detEpDesenhar(Uint32 agora) {
  (void)agora;
  const CatItem *ci = cat_item(idx);
  const float screenW = NV_TELA_W, screenH = NV_TELA_H;
  const float leftW = 405.0f, startX = 485.0f, rowsTop = 160.0f;
  gfx_sem_recorte();
  gfx_cor((GfxRect){0,0,screenW,screenH},0.0f,.024f,.027f,.034f,1.0f);
  gfx_cor((GfxRect){0,0,leftW,screenH},0.0f,.041f,.045f,.055f,1.0f);
  const char *seriesName = ci && ci->titulo[0] ? ci->titulo : "Episodes";
  txt_desenhar_alpha(txt_linha_corta(TXT_HEADLINE,seriesName,253,253,254,255,
                      screenW - 140.0f),60.0f,48.0f,1.0f);
  txt_desenhar_alpha(txt_linha(TXT_CAPTION2,"SEASONS",175,184,197,255),
                     60.0f,132.0f,1.0f);
  txt_desenhar_alpha(txt_linha(TXT_CAPTION2,"EPISODES",175,184,197,255),
                     startX,132.0f,1.0f);
  gfx_recorte(36.0f,180.0f,leftW-48.0f,screenH-225.0f);
  for (int s = 0; s < detEpNSeasons; s++) {
    float y = 195.0f + (float)s * 88.0f - detEpSeasonScroll;
    if (y + 76.0f < 180.0f || y > screenH - 40.0f) continue;
    GfxRect chip = {48.0f,y,330.0f,76.0f};
    int selected = s == detEpSzn, focused = selected && detEpCol == 0;
    if (focused) {
      gfx_vidro_painel(chip,0.23f,0.65f,1.0f);
      gfx_cor(chip,0.23f,.25f,.29f,.36f,.35f);
    } else
      gfx_cor(chip,0.23f,selected ? .18f:.09f,selected ? .20f:.10f,
              selected ? .25f:.12f,selected ? .96f:.75f);
    char title[60]; snprintf(title,sizeof title,"Season %d",detEpSeasons[s]);
    txt_desenhar_alpha(txt_linha(TXT_CALLOUT,title,247,247,249,255),
                       chip.x+20,chip.y+12.0f,focused?1.0f:.85f);
    char epCount[32];
    snprintf(epCount,sizeof epCount,"%d episodes",detEpCounts[s]);
    txt_desenhar_alpha(txt_linha(TXT_CAPTION2,epCount,177,184,195,255),
                       chip.x+20,chip.y+46.0f,.86f);
  }
  gfx_sem_recorte();
  gfx_recorte(startX,rowsTop,screenW-startX-38.0f,screenH-rowsTop-20.0f);
  for (int r=0; r<detEpRows; r++) {
    const CatEp *ep = detEpAt(r);
    if (!ep) continue;
    float y = rowsTop + (float)r * 207.0f - detEpScroll;
    if (y > screenH || y + 197.0f < rowsTop) continue;
    int focused = detEpCol == 1 && r == detEpRow;
    GfxRect card={startX,y,screenW-startX-55.0f,191.0f};
    if (focused) {
      gfx_cor(card,0.10f,.12f,.15f,.20f,.98f);
      gfx_luz_canto(card,0.10f,card.w*.25f,-20.0f,card.w*.7f,
                    .38f,.45f,.56f,.08f);
    } else {
      gfx_cor(card,0.10f,.065f,.069f,.078f,.65f);
    }
    GfxRect thumb={startX+12.0f,y+12.0f,300.0f,167.0f};
    const char *art=ep->thumb[0] ? ep->thumb
      : ci && ci->backdrop[0] ? ci->backdrop : NULL;
    GLuint tex=art?tex_obter_larg(art,300.0f):0;
    const int watched = ci && ci->imdb[0] &&
      vistoep_estado(ci->imdb,ep->temporada,ep->episodio)==1;
    // Match Legacy's spoiler setting. Never briefly flash a sharp thumbnail
    // while the GPU blur texture is still being prepared.
    if (tex && ajustes_desfocar_nao_assistidos() && !watched) {
      GLuint blurred = gfx_desfocado(tex,art);
      tex = blurred ? blurred : 0;
    }
    if (tex) {
      gfx_tex_aspect_atual=tex_aspecto(art);
      gfx_rect(thumb,tex,GFX_CARD,0,0,0,0.08f,1,1,1,1);
      gfx_tex_aspect_atual=0;
    } else gfx_cor(thumb,.08f,.12f,.13f,.15f,1.0f);
    // Dark liquid-glass episode identifier; no white focus rectangle.
    GfxRect epBadge={thumb.x+11.0f,thumb.y+10.0f,94.0f,34.0f};
    if (focused) gfx_vidro_painel(epBadge,.44f,.55f,1.0f);
    else gfx_cor(epBadge,.44f,.015f,.018f,.025f,.88f);
    if (watched) {
      GfxRect marker={thumb.x+thumb.w-48.0f,thumb.y+10.0f,34.0f,34.0f};
      gfx_cor(marker,.5f,.015f,.018f,.025f,.9f);
      gfx_icone((GfxRect){marker.x+6,marker.y+6,22,22},
                 "aj_check",1,1,1,1);
    }
    char id[40]; snprintf(id,sizeof id,"S%dE%d",ep->temporada,ep->episodio);
    txt_desenhar_alpha(txt_linha(TXT_CAPTION2,id,251,253,255,255),
                       epBadge.x+12.0f,epBadge.y+8.0f,1.0f);
    // Selected row progress is shown separately from its episode information.
    if (ci && ci->temporada==ep->temporada && ci->episodio==ep->episodio
        && ci->progresso>0 && ci->progresso<100) {
      GfxRect track={thumb.x+8.0f,thumb.y+thumb.h-8.0f,thumb.w-16.0f,4.0f};
      gfx_cor(track,.5f,.85f,.85f,.87f,.30f);
      track.w *= ci->progresso / 100.0f;
      gfx_cor(track,.5f,.93f,.94f,.97f,1.0f);
    }
    float tx=thumb.x+thumb.w+28.0f;
    float maxW=card.x+card.w-tx-16.0f;
    char title[280];
    if (ep->nome[0]) snprintf(title,sizeof title,"%s",ep->nome);
    else snprintf(title,sizeof title,"Episode %d",ep->episodio);
    txt_desenhar_alpha(txt_linha_corta(TXT_CALLOUT,title,250,251,253,255,maxW),
                       tx,y+17.0f,1.0f);
    char meta[180]="";
    if (ep->duracao[0]) snprintf(meta,sizeof meta,"%s",ep->duracao);
    if (ep->data[0]) {
      size_t used=strlen(meta);
      snprintf(meta+used,sizeof(meta)-used,"%s%s",used?"  ·  ":"",ep->data);
    }
    if (meta[0]) txt_desenhar_alpha(txt_linha_corta(TXT_CAPTION2,meta,
          190,197,208,255,maxW),tx,y+57.0f,.95f);
    if (detEpShowRating && ep->notaImdb>0) {
      char score[48]; snprintf(score,sizeof score,"IMDb %d.%d",
                                ep->notaImdb/10,ep->notaImdb%10);
      txt_desenhar_alpha(txt_linha(TXT_CAPTION2,score,246,211,74,255),
                         tx,y+84.0f,1.0f);
    }
    if (detEpShowDesc && ep->sinopse[0])
      txt_bloco(TXT_PG_FIM,ep->sinopse,191,197,209,
                tx,y+113.0f,maxW,26.0f,1.0f,2);
    if (epNaoExibido(ep))
      txt_desenhar_alpha(txt_linha(TXT_CAPTION2,"Not yet aired",
                              247,208,111,255),tx,y+159.0f,1.0f);
  }
  gfx_sem_recorte();
}
#endif
