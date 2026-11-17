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
static Uint32 detEpOkSince;
#define DET_EP_STEP 278.0f
#define DET_SEASON_STEP 86.0f

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
  detEpScroll = fmaxf(0.0f, (float)(detEpRow - 1) * DET_EP_STEP);
  detEpVel = 0;
  detEpSeasonScroll = fmaxf(0.0f, (float)(detEpSzn - 3) * DET_SEASON_STEP);
  detEpSeasonVel = 0;
  detEpOkSince = 0;
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
  const float step = DET_EP_STEP, seasonStep = DET_SEASON_STEP;
  float maxScroll = fmaxf(0.0f, (float)detEpRows * step - (NV_TELA_H-142.0f));
  float target = fmaxf(0.0f, ((float)detEpRow - 1.0f) * step);
  if (target > maxScroll) target = maxScroll;
  detEpScroll = anim_mola2(&detEpVel, detEpScroll, target, dt, NV_MOLA2_SCROLL);
  float maxSeason = fmaxf(0.0f, (float)detEpNSeasons * seasonStep - (NV_TELA_H-317.0f));
  float targetSeason = fmaxf(0.0f, ((float)detEpSzn - 3.0f) * seasonStep);
  if (targetSeason > maxSeason) targetSeason = maxSeason;
  detEpSeasonScroll = anim_mola2(&detEpSeasonVel, detEpSeasonScroll,
                                 targetSeason, dt, NV_MOLA2_SCROLL);
}

static int detEpEvento(const SDL_Event *ev) {
  if (!detEpAberto) return 0;
  if (ev->type != SDL_KEYDOWN && ev->type != SDL_KEYUP) return 1;
  SDL_Keycode key=ev->key.keysym.sym;
  int ok=(key==SDLK_RETURN || key==SDLK_KP_ENTER);
  // Trigger exactly once on KEYUP; never auto-play when OK is held.
  if (ev->type==SDL_KEYUP) {
    if (!ok || !detEpOkSince) return 1;
    Uint32 held=SDL_GetTicks()-detEpOkSince;
    detEpOkSince=0;
    if(detEpCol==0) {detEpCol=1;return 1;}
    const CatEp *ep=detEpAt(detEpRow);
    if(ep && ep->temporada>=0 && ep->episodio>0 && !epNaoExibido(ep)) {
      if(held>=NV_HOLD_MS) {
        // Episode action menu belongs to the actual expanded Dynamic Clock.
        pedOpcoesT=ep->temporada;pedOpcoesE=ep->episodio;pedOpcoesEp=1;
      } else {
        pedFontesT=ep->temporada;pedFontesE=ep->episodio;
        pedFontes=1;
        // The stream Dynamic Clock overlays this view rather than closing it.
      }
    }
    return 1;
  }
  if(key==SDLK_ESCAPE || key==SDLK_AC_BACK ||
     key==SDLK_BACKSPACE || key==SDLK_DELETE ||
     ev->key.keysym.scancode==NV_SCANCODE_BACK) {
    detEpAberto=0;
    detEpOkSince=0;
    return 1;
  }
  if(ok) {
    if(!ev->key.repeat && !detEpOkSince)detEpOkSince=SDL_GetTicks();
    return 1;
  }
  if(key==SDLK_LEFT)detEpCol=0;
  if(key==SDLK_RIGHT)detEpCol=1;
  if(key==SDLK_UP || key==SDLK_DOWN) {
    int dir=key==SDLK_DOWN?1:-1;
    if(detEpCol==0) {
      int next=detEpSzn+dir;
      if(next>=0 && next<detEpNSeasons) {
        detEpRebuild(detEpSeasons[next],1);
        detEpScroll=detEpVel=0;
      }
    } else {
      int next=detEpRow+dir;
      if(next>=0 && next<detEpRows)detEpRow=next;
    }
  }
  return 1;
}

static void detEpDesenhar(Uint32 agora) {
  (void)agora;
  const CatItem *ci = cat_item(idx);
  const float screenW = NV_TELA_W, screenH = NV_TELA_H;
  const float leftW = screenW * .40f, startX = leftW + 23.0f, rowsTop = 108.0f;
  gfx_sem_recorte();
  // True AMOLED-black columns, without a colour halo or grey panel.
  gfx_cor((GfxRect){0,0,screenW,screenH},0,0,0,0,1);
  const char *seriesName = ci && ci->titulo[0] ? ci->titulo : "Episodes";
  logotitulo_desenhar(ci,seriesName,TXT_HEADLINE,107.0f,94.0f,
                     leftW-188.0f,162.0f,leftW-188.0f,1.0f);
  gfx_recorte(38.0f,306.0f,leftW-76.0f,screenH-337.0f);
  for (int s = 0; s < detEpNSeasons; s++) {
    float y = 326.0f + (float)s * DET_SEASON_STEP - detEpSeasonScroll;
    if (y + 67.0f < 306.0f || y > screenH - 30.0f) continue;
    int focused = s == detEpSzn && detEpCol == 0;
    char label[100];
    snprintf(label,sizeof label,"Season %d  ·  %d episodes",
             detEpSeasons[s],detEpCounts[s]);
    TxtLinha lt=txt_linha(TXT_CALLOUT,label,focused?18:220,
                        focused?18:220,focused?18:220,255);
    float pw=(float)lt.w+54.0f;
    if(pw>leftW-110.0f)pw=leftW-110.0f;
    GfxRect chip={(leftW-pw)*.5f,y,pw,67.0f};
    if(focused)gfx_cor(chip,.5f,.975f,.975f,.975f,1);
    txt_desenhar_alpha(lt,chip.x+(chip.w-lt.w)*.5f,
                       chip.y+(chip.h-lt.h)*.5f,1);
  }
  gfx_sem_recorte();
  gfx_recorte(startX,rowsTop,screenW-startX-20.0f,screenH-rowsTop-15.0f);
  for (int r=0; r<detEpRows; r++) {
    const CatEp *ep = detEpAt(r);
    if (!ep) continue;
    float y = rowsTop + (float)r * DET_EP_STEP - detEpScroll;
    if (y > screenH || y + DET_EP_STEP < rowsTop) continue;
    int focused = detEpCol == 1 && r == detEpRow;
    GfxRect card={startX,y,screenW-startX-51.0f,DET_EP_STEP-13.0f};
    if (focused) {
      gfx_cor(card,.09f,.06f,.06f,.06f,.82f);
      gfx_anel_fora(card,.09f,0,2.0f,1,1,1,.85f);
    }
    GfxRect thumb={startX+15.0f,y+18.0f,395.0f,222.0f};
    const char *art=ep->thumb[0] ? ep->thumb
      : ci && ci->backdrop[0] ? ci->backdrop : NULL;
    GLuint tex=art?tex_obter_larg(art,thumb.w):0;
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
    char id[40]; snprintf(id,sizeof id,"S%dE%d",ep->temporada,ep->episodio);
    TxtLinha idText=txt_linha(TXT_CAPTION2,id,251,253,255,255);
    GfxRect epBadge={thumb.x+10,thumb.y+10,(float)idText.w+24.0f,28.0f};
    gfx_cor(epBadge,.5f,0,0,0,.23f);
    gfx_anel_fora(epBadge,.5f,0,1.0f,1,1,1,.15f);
    if (watched) {
      GfxRect marker={thumb.x+thumb.w-43,thumb.y+10,31,31};
      gfx_cor(marker,.5f,1,1,1,1);
      gfx_icone((GfxRect){marker.x+5,marker.y+5,21,21},
                 "aj_check",.04f,.04f,.04f,1);
    }
    txt_desenhar_alpha(idText,epBadge.x+(epBadge.w-idText.w)*.5f,
                       epBadge.y+(epBadge.h-idText.h)*.5f,1);
    // Selected row progress is shown separately from its episode information.
    if (ci && ci->temporada==ep->temporada && ci->episodio==ep->episodio
        && ci->progresso>0 && ci->progresso<100) {
      GfxRect track={thumb.x+8.0f,thumb.y+thumb.h-8.0f,thumb.w-16.0f,4.0f};
      gfx_cor(track,.5f,.85f,.85f,.87f,.30f);
      track.w *= ci->progresso / 100.0f;
      gfx_cor(track,.5f,.94f,.10f,.14f,1.0f);
    }
    float tx=thumb.x+thumb.w+27.0f;
    float maxW=card.x+card.w-tx-16.0f;
    char title[280];
    if (ep->nome[0]) snprintf(title,sizeof title,"%s",ep->nome);
    else snprintf(title,sizeof title,"Episode %d",ep->episodio);
    txt_desenhar_alpha(txt_linha_corta(TXT_CALLOUT,title,250,251,253,255,maxW),
                       tx,y+17.0f,1.0f);
    char meta[180]="";
    if (ep->duracao[0]) {
      int mins=atoi(ep->duracao);
      if(mins>=60)snprintf(meta,sizeof meta,"%dh %dm",mins/60,mins%60);
      else snprintf(meta,sizeof meta,"%s",ep->duracao);
    }
    if (ep->data[0]) {
      size_t used=strlen(meta);
      snprintf(meta+used,sizeof(meta)-used,"%s%s",used?"  ·  ":"",ep->data);
    }
    if (meta[0]) txt_desenhar_alpha(txt_linha_corta(TXT_CAPTION2,meta,
          190,197,208,255,maxW),tx,y+57.0f,.95f);
    if (detEpShowRating && ep->notaImdb>0) {
      float mw=notasui_marca(EX_IMDB,tx,y+109.0f,29.0f,1);
      char score[25];snprintf(score,sizeof score,"%d.%d",
                             ep->notaImdb/10,ep->notaImdb%10);
      txt_desenhar_alpha(txt_linha(TXT_CAPTION2,score,241,241,241,255),
                         tx+mw+12.0f,y+96.0f,1);
    }
    if (detEpShowDesc && ep->sinopse[0])
      txt_bloco(TXT_PG_FIM,ep->sinopse,210,210,210,
                tx,y+134.0f,maxW,26.0f,1.0f,4);
    if (epNaoExibido(ep))
      txt_desenhar_alpha(txt_linha(TXT_CAPTION2,"Not yet aired",
                              247,208,111,255),tx,y+233.0f,1.0f);
  }
  gfx_sem_recorte();

}
#endif
