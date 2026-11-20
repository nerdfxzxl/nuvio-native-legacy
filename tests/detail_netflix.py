#!/usr/bin/env python3
"""Static integration checks for the Netflix-style native detail UI.

The Android workflow also compiles the real C implementation. These checks
protect navigation/data contracts that a C syntax check cannot verify.
"""
from pathlib import Path

detail = Path("src/detail.c").read_text()
episode = Path("src/detail_episodes_netflix.inc").read_text()
trailer = Path("src/trailer.c").read_text()
settings = Path("src/ajustes.c").read_text()
settings_page = Path("src/ajustes_ux_tela.inc").read_text()
extras = Path("src/extras.c").read_text()
workflow = Path(".github/workflows/android-debug.yml").read_text()

def need(what, ok):
    if not ok:
        raise SystemExit("FAILED: " + what)

need("Fullscreen episodes hook", '#include "detail_episodes_netflix.inc"' in detail
     and 'if (detEpEvento(e)) return;' in detail
     and 'if (detEpAberto) { detEpDesenhar(agora); return; }' in detail)
need("Legacy episode carousel not focusable", "case SEC_TEMPORADAS:\n    case SEC_EPISODIOS:\n      return 0;" in detail)
need("Five actions in correct order", all(
    key in detail for key in ("return ACAO_PRIMARIO", "return ACAO_EPISODIOS",
                               "return ACAO_TRAILER", "return ACAO_ASSISTIDO",
                               "return temMais() && n == 0 ? ACAO_MAIS")))
need("Hero scroll independent of page", "scroll=anim_mola2(&velocity,scroll,(float)first*step,dt,NV_MOLA2_SCROLL);" in detail
     and "if (botao + 1 < nBotoes())" in detail)
need("Rows rendered inside viewport", "gfx_recorte(x-9,yTop-8,stackW+18,totalH+16)" in detail)
need("Watched progress is red ONLY on episode thumbnails",
     "primarioPct=-1;" in detail and
     "gfx_cor(track,.5f,.94f,.10f,.14f,1.0f)" in episode)
need("Actual artwork title, AMOLED episode split, and compact season pills",
     "logotitulo_desenhar(" in episode and
     "screenW*.40f" in episode and
     "gfx_cor((GfxRect){0,0,screenW,screenH},0,0,0,0,1)" in episode and
     '"Season %d"' in episode and
     'detEpTouchSeason' in episode)
need("Focused hero action pill, no grey rectangular backgrounds",
     "label=\"More\";ico=\"aj_ellipsis\";" in detail and
     "gfx_cor(row,.5f,.975f,.975f,.975f,k*a)" in detail)

need("Season order ascending", "detEpSeasons[j - 1] > current" in episode)
need("Season metadata cached", "detEpCounts[s]" in episode)
need("Episode cache watches BOTH content and catalog metadata clocks",
     "detEpCatalogVersion != cat_versao_episodios()" in episode and
     "detEpItemRevision != cat_revisao_itens()" in episode)
need("Early CI compiles the trailer, detail, extras, and More translation units",
     all(unit in Path("tools/android/preflight-c.sh").read_text()
         for unit in ("src/detail.c", "src/trailer.c", "src/extras.c", "src/detmais.c")))
need("Episode selection anchored before Dynamic Clock stream panel",
     "pedFontesT=ep->temporada;pedFontesE=ep->episodio;" in episode and
     "stream_folha_abrir_opcoes_ep(watched)" in Path("src/app.c").read_text() and
     "pedOpcoesT=ep->temporada;pedOpcoesE=ep->episodio" in episode)
need("Episode actions use real clock, not a separate drawn overlay",
     "void stream_folha_abrir_opcoes_ep(int watched)" in Path("src/streams.c").read_text()
     and "if(folhaOpcoesEp)" in Path("src/streams.c").read_text()
     and "detEpMenuOpen" not in episode)

need("Unaired episodes blocked", "!epNaoExibido(ep)" in episode)
need("Spoiler preferences preserved", "ajustes_desfocar_nao_assistidos()" in episode)
need("Explicit IMDb provenance", "ep->notaImdb" in episode and
     "extras_ep_nota" not in episode)
need("Full-screen episode FPS diagnostics", 'return "episodes-netflix";' in Path("src/main.c").read_text())
need("More panel right aligned", "r.x = anc.x + anc.w + DM_VAO;" in Path("src/detmais.c").read_text())
need("Trailer restores detail and controls", "trailerControlFocus" in detail
     and "void trailer_recomecar(void)" in trailer)
need("Series Details positioned in document", "topoSec[SEC_DETALHES] = conteudoSec[SEC_DETALHES] = y;" in detail)
need("Series metadata parsed", 'serie ? "first_air_date" : "release_date"' in extras)
need("Episode visibility switches", all(
    name in settings and name in settings_page for name in
    ("AJ_DET_EP_DESCRICAO", "AJ_DET_EP_IMDB")))
need("Optional Trakt/TMDB build configuration", all(
    name in workflow for name in ("secrets.TRAKT_CLIENT_ID", "secrets.TMDB_API_KEY")))
need("Three clear trailer controls and exact-source resumed position",
     "trailerControlFocus < 2" in detail
     and "trailer_alternar_som()" in detail
     and "trailerRetomadaPos=video_pos()" in trailer)
need("Details positioned and DRAWN using stacked document geometry",
     "case SEC_DETALHES:  // same stacked position" in detail
     and "case SEC_DETALHES: desenhaDetalhes(x, y, f, a)" in detail)
need("Ratings only, focused whole card, no retention or fingerprint",
     "return t>=0 && extras_n_eps(t)>0 ? 1 : 0;" in detail
     and "b.focado" in detail and
     "Ratings by episode" in Path("src/serieaud.c").read_text()
     and "cardDigital((GfxRect)" not in Path("src/serieaud.c").read_text()[Path("src/serieaud.c").read_text().find("float serieaud_bloco(float "):])
need("Explicit stream selection before restarting an episode",
     "folhaInicioDepoisEscolha=choice==1" in Path("src/app.c").read_text()
     and "if(folhaInicioDepoisEscolha)player_do_inicio();" in Path("src/app.c").read_text())
need("Episode touch and hold feedback", "ponteiro_rolagem(detEpTouchScroll)" in episode
     and "ilha_atividade(p>=1.0f?" in episode)
need("Reviews use the compact season/episode picker, not fullscreen Episodes",
     "static void comentariosEscolher(void)" in detail
     and "comMenuAbrir();" in detail
     and "if (comMenuEvento(e)) return;" in detail
     and "comMenuDesenhar(1.0f);" in detail
     and "detEpAbrirReview(t,e)" not in detail)
need("Review dropdown has five-row viewport and accessible focus",
     "#define COM_MENU_VISIBLE 5" in detail
     and "comMenuAjustarFoco(comMenuFocus+delta)" in detail
     and "comSeasonNav" in detail
     and "comSeasonPointerSelect" in detail
     and "comMenuPointerSelect" in detail)
need("Review episode choice refreshes Trakt with the selected season/episode",
     "comEpT=comMenuSeason;comEpE=choice;" in detail
     and "extras_pedir_comentarios_ep(ci->imdb,comEpT,comEpE)" in detail)
need("Review Season control animates from Episode without legacy hold hint",
     "comSeasonAnim" in detail
     and "if (foco.coluna == 2) comentariosEscolher();" in detail
     and "COM_DICA_CHAVE" not in detail)
need("No poster vignette baked over artwork", "gfx_rect(alvo,tex,GFX_CARD,0,0,0,0,1,1,1,alpha)" in detail)
# Build 122 regressions: the Build 121 editor restored the row style every
# frame while its slider was open, cancelling preview and even saved results.
ctx = Path("src/ctxmenu.c").read_text()
rows = Path("src/fileiras.c").read_text()
home = Path("src/home.c").read_text()
clock = Path("src/ilha.c").read_text()
need("Row-size page keeps current style alive",
     'estVivo && (!aberto || (pagina != 1 && pagina != 3))' in ctx)
need("Saving row size commits style, value and dialog origin",
     'fil_definir_tipo(filChave,t);' in ctx and
     'fil_preview_dp(filChave,t,sizeDp,1);' in ctx and
     'estOrigTipo = fil_tipo(filChave);' in ctx)
need("Style size persisted by exact row and exact style, without aliases",
     'static int styleFamily(int t) { return t; }' in rows and
     'linhas[i].styleDp[previewStyle]=dp==FIL_SIZE_DP_DEFAULT?0:dp' in rows)
need("Style menu shows dedicated unobstructed hold hint",
     '"Hold for options"' in ctx and
     'm.y + m.h - hintH + (hintH - hint.h) * 0.5f' in ctx)
need("Poster long OK reuses Play native Dynamic Clock activity",
     'ilha_atividade(i18n(p >= 1.0f ? "Solte para abrir opções" : "Segure para opções"), p);' in home and
     'ilha_segurar_cartaz' not in home and 'posterHoldClock' not in clock)
need("Netflix stills enlarge responsively, badge is bigger/darker",
     '#define DET_EP_STEP 380.0f' in episode and
     'thumbW*9.0f/16.0f' in episode and
     'TXT_CALLOUT,id' in episode and
     '.035f,.82f' in episode)
need("Current season remains highlighted while its episodes have focus",
     'int selected = s == detEpSzn;' in episode and
     'if(selected)gfx_cor(pill,' in episode)
need("Style-specific size range is wired to UI, persistence and reload",
     "int fil_size_max_dp(int t)" in rows and
     "unsigned short styleDp[FIL_TIPO_N]" in rows and
     "dp<=fil_size_max_dp(t)" in rows and
     "const int styleMax=fil_size_max_dp(estTipo(estFoco));" in ctx and
     "snprintf(maxLabel,sizeof maxLabel" in ctx)
need("Held D-pad accelerates smoothly using elapsed time rather than repeat",
     "sizeHoldFrame(agora)" in ctx and
     "sizeHoldDir=dir; sizeHoldSince=sizeHoldLast=SDL_GetTicks();" in ctx and
     "sizeMove(dir); // initial single press = exactly 1 dp" in ctx and
     "float speed=9.0f+221.0f*ramp*ramp;" in ctx and
     "sizeHoldFraction+=speed*dt;" in ctx and
     "if (sizeHoldDir!=dir)" in ctx and
     "sizeHoldStop();" in ctx)
need("Size editor release resets held movement",
     "if (pagina==3 && e->type==SDL_KEYUP)" in ctx and
     "sizeHoldDir==-1 && key==SDLK_LEFT" in ctx and
     "sizeHoldDir==1 && key==SDLK_RIGHT" in ctx)
# Build 124: preserve title artwork while removing only the Home poster
# bottom fade, and keep the new metadata toggles independent and persisted.
settings_ui = Path("src/ajustes_ux_tela.inc").read_text()
settings_defaults = Path("src/ajustes_ux_padrao.inc").read_text()
settings_header = Path("src/ajustes.h").read_text()
need("Independent poster metadata switches are in Posters and trailers",
     'ROT("Poster information"),' in settings_ui
     and "OPC(AJ_POSTER_GENERO), OPC(AJ_NOTAS_HOME)" in settings_ui
     and settings_ui.count("OPC(AJ_NOTAS_HOME)") == 1
     and settings_ui.count("OPC(AJ_POSTER_GENERO)") == 1)
need("Poster genre is an append-only persisted setting, on by default",
     "AJ_POSTER_GENERO," in settings
     and '"posterGenreLocal"' in settings
     and 'ESC("Genre on posters", V_LIGA, 2)' in settings
     and "Genre on Home posters: On by default" in settings_defaults
     and "int   ajustes_poster_genero(void)" in settings_header)
need("Genre and IMDb on posters independently control drawing",
     "ajustes_poster_genero()" in home
     and "ajustes_notas_home()" in home
     and 'const char *genero = (semMeta || !ajustes_poster_genero()) ? NULL' in home
     and "int imdbRating = orig && ci && ajustes_notas_home()" in home)
need("Wide poster artwork no longer receives lower-half gradient",
     "if (editorial(tipo)) gfx_veu_card_por(NV_EDITORIAL_VEU_F" not in home
     and "if (abre > 0.01f && cItem) gfx_veu_card_por" not in home
     and "const float fVeu = larga ? NV_DIN_LARGA_VEU" not in home
     and 'No wide dark ramp below editorial title artwork' not in home)
need("Episode list no longer exposes top bar or prior card sliver",
     "#define DET_EP_ROWS_TOP 48.0f" in episode
     and "#define DET_EP_FOCUS_TOP_GAP 36.0f" in episode
     and "rowsTop=DET_EP_ROWS_TOP" in episode
     and "detEpRow*step-DET_EP_FOCUS_TOP_GAP" in episode
     and "logotitulo_desenhar(ci,seriesName,TXT_HEADLINE," in episode)
print("PASS: Netflix detail refinements, Home poster metadata and upper episode strip")
