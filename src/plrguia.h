// Native NuvioTV-style top-left parental warning overlay. Independent
// from the Dynamic Clock and player controls. Staggered reveal and reverse
// fade timings follow official NuvioTV dev/ParentalGuideOverlay.kt.
#ifndef NV_PLRGUIA_H
#define NV_PLRGUIA_H

// Called each frame during the startup warning window. `tg` is seconds since
// the overlay first became eligible; osd/classificacao kept for API stability.
void plrguia_pedir(float tg, float osd, const char *classificacao);

#endif
