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
need("Watch progress preserved", "primarioPct = retomando" in detail)
need("Season order ascending", "detEpSeasons[j - 1] > current" in episode)
need("Season metadata cached", "detEpCounts[s]" in episode)
need("Episode cache watches BOTH content and catalog metadata clocks",
     "detEpCatalogVersion != cat_versao_episodios()" in episode and
     "detEpItemRevision != cat_revisao_itens()" in episode)
need("Early CI compiles the trailer, detail, extras, and More translation units",
     all(unit in Path("tools/android/preflight-c.sh").read_text()
         for unit in ("src/detail.c", "src/trailer.c", "src/extras.c", "src/detmais.c")))
need("Episode selection anchored before source panel", "pedFontesT = ep->temporada; pedFontesE = ep->episodio" in episode)
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
print("PASS: Netflix detail integration invariants")
