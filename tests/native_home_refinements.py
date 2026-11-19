#!/usr/bin/env python3
"""Fast guardrails for the owner's native Home, icon and gesture refinements.

These assertions are NOT a screenshot or device test. The NDK preflight must
also compile main.c, home.c and detail.c before an APK run can proceed.
"""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
def read(name):
    return (root / name).read_text(encoding="utf-8")
def require(ok, message):
    if not ok:
        raise AssertionError(message)

home = read("src/home.c")
main = read("src/main.c")
icon = read("src/episodeicon.h")
detail = read("src/detail.c")
workflow = read(".github/workflows/android-debug.yml")
preflight = read("tools/android/preflight-c.sh")

require("portraitExpand = ajustes_expandir_poster() && podeExpandir(r);" in home and
        "portraitTex" not in home, "Use one image throughout expansion")
require("memcpy(expansionStart, expansion" in home and "neighborOffset += extraWidth" in home,
        "Retarget from displayed values and place neighbors using the actual width")
require("ajustes_foco_instantaneo()" in home and "posterFocusInstantLocal" in read("src/ajustes.c"),
        "Instant focus must persist")
# Landscape artwork in a portrait box must COVER, not contain/letterbox.
cover = home.split("gfx_card_forcar_cover_atual = (", 1)[1].split(") ? 1.0f", 1)[0]
for item in ("FILEIRA_NORMAL", "FILEIRA_POSTER", "FILEIRA_LARGA"):
    require("tipo == " + item in cover, "Missing cover rule for " + item)
require("gfx_tex_aspect_atual = tex_aspecto(caminho)" in home,
        "Cover crop must use the real image aspect ratio")
gfx = read("src/gfx.c")
require("vec2 coverAsp" in gfx,
        "Renderer must use aspect-preserving cover coordinates")
require("uv0.x += uPar.y * (1.0 - visible)" in gfx and "cropBias" in home,
        "Portrait subject crop must respect the existing artwork saliency")

# Source guards for honest focused-card metadata and ambient colors.
expanded_labels = home.split("static void desenhaFaixaAberta", 1)[1].split("// ZERO.", 1)[0]
require("tend_delta(" not in expanded_labels and "i18n(\"Novo\")" not in expanded_labels,
        "Remove untrusted trending movement/New labels from expanded cards")
require("badge_imdb(" in expanded_labels and "ci->meta" in expanded_labels,
        "Preserve IMDb badge and year while removing speculative labels")
require("fileiraItemIndice(f, foco.coluna)" in home and "tex_publicar_paleta(arte)" in home,
        "Immersive colors must follow the actual focused item's cached palette")
require('"Resume"' in detail and '"Play"' in detail and '"Resume S%dE%d"' in detail,
        "Detail Play and Resume should use consistent English action text")
require("4, /* Dynamic layout background: AMOLED Black by default" in read("src/ajustes_ux_padrao.inc"),
        "Pure AMOLED black must remain the default on new installs")

# Artwork title is independently drawn inside both portrait and wide cards.
require("desenhaLogoCartao(cItem,px,py,w,h," in home,
        "Missing per-card title-artwork draw")
require("artehero_logo_sessao_larg(ci, maxW)" in home,
        "Use an independent cached title-logo URL")
require("largo ? px + w*0.055f : px + (w-tw)*0.5f" in home,
        "Title logo must be centered in portrait, left in landscape")
require("if (!logoCard) txt_desenhar_alpha(tn" in home,
        "Do not draw a duplicate generic title over existing artwork logo")

# The reference glyph is two L corners behind a single empty front box.
require("episodeicon_draw" in icon and "gfx_cor" in icon,
        "Episode icon must be drawn natively, not from a screenshot")
require(icon.count("gfx_cor((GfxRect)") == 8,
        "Expected two 2-stroke L shapes and one 4-stroke front rectangle")
require("float iconInk=1.0f-k;" in detail,
        "Focused Episodes icon should use full black")
require("episodeicon_draw" in detail, "Detail must use the shared icon")

# Touch mode preserves the original selector's remote gestures.
require("const int selector=stream_folha_aberta() && st_dono()!=ST_BUSCA;" in main,
        "Stream selector must retain its independent gesture handler")
require("(selector || ajustes_gesto_remoto()) && !texto_sistema_aberto()" in main,
        "Native gesture mode must be active beyond the selector")
require("SDL_IsTextInputActive();" not in main.split("static int phoneRemoteEnabled(void)",1)[1].split("}",1)[0],
        "SDL text-input state is not a reliable global gesture gate")
require("phoneRemoteRepeat();" in main and "phoneFingerDown=0;phoneDirection=SDLK_UNKNOWN;" in main,
        "Swipe-and-hold must repeat and stop immediately on release")

# Input boundary guards: repeat events retain full speed within the row/actions
# and only a fresh physical press crosses the sidebar or More threshold.
require("homeLeftHeldFromRow" in home and "e->key.repeat || homeLeftHeldFromRow" in home,
        "Held Left must stop on first poster without opening the sidebar")
require("foco.coluna > 0" in home and "foco.coluna = 0;" in home,
        "Home Back must first return to the beginning of the current row")
require("downHeldFromActions" in detail and "e->key.repeat || downHeldFromActions" in detail,
        "Held Down must stop on More while repeated action navigation survives")
require("phoneSecondTap" in main and
        "phoneRemoteKeyEvent(SDLK_RETURN,SDL_KEYDOWN,0)" in main and
        "phoneRemoteKeyEvent(SDLK_RETURN,SDL_KEYUP,0)" in main,
        "Double tap + hold must map to a balanced native long-OK gesture")
require("0.898f,0.035f,0.078f" in detail and "prog_por_chave(key,&progress)" in detail,
        "Resume progress must be red and sourced from saved checkpoint data")
settings = read("src/ajustes.c")
settings_page = read("src/ajustes_ux_tela.inc")
require("OPC(AJ_TAM_GRANDE_DP)" not in settings_page and "fil_preview_dp" in read("src/ctxmenu.c"),
        "Row size belongs in the row style editor")
require("sharedPosterUrl" in detail and "de = item.rect;" in detail and
        "item.rect.w < NV_TELA_W * 0.95f" in detail,
        "Open/close must share actual poster bounds without enabling the old carousel")
require("carro = 0;" in detail,
        "Normal detail opening must not re-enable the retired carousel")

# Don't accidentally reintroduce the ~30-minute apt install.
require("python3-pil" in workflow and "ffmpeg" not in workflow.split("name: Install Android NDK and SDL development headers",1)[1].split("name: Compile-check",1)[0],
        "Use lightweight host Pillow conversion instead of the expensive FFmpeg package")
require("gradle/actions/setup-gradle@v4" in workflow,
        "Cache Gradle dependencies for faster repeat runs")
require("Save completed P2P engine for retries" in workflow,
        "Save completed P2P immediately so later APK errors do not discard its cache")
require("id: p2p-cache" in workflow and "steps.p2p-cache.outputs.cache-hit != 'true'" in workflow,
        "P2P must skip rebuild on an exact cache hit")
require("src/home.c" in preflight and "python3 tests/native_home_refinements.py" in preflight,
        "Compile Home and run these checks in the no-APK preflight")
print("Native Home refinement source guards: passed")
