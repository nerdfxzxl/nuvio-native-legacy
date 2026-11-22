// P1: immutable release identity for same-source URL renewal.
// Pure C so tests can run without SDL/Android. Never compare signed CDN URLs.
#ifndef NV_FONTEID_P1_H
#define NV_FONTEID_P1_H
#include <stdint.h>
#include <string.h>
#include <strings.h>

typedef struct {
  const char *addon, *infoHash;
  int fileIdx;
  const char *arquivo, *rotulo;
  uint64_t bytes;
  const char *grupo;
} NvFonteId;

static inline int nv_fonte_mesma_liberacao(const NvFonteId *a, const NvFonteId *b) {
  if (!a || !b || !a->addon || !b->addon || !a->addon[0] ||
      strcmp(a->addon, b->addon)) return 0;
  // Known exact sizes are authoritative even for the same torrent hash.
  if (a->bytes && b->bytes && a->bytes != b->bytes) return 0;
  if (a->infoHash && a->infoHash[0]) {
    return b->infoHash && b->infoHash[0] &&
           !strcasecmp(a->infoHash, b->infoHash) && a->fileIdx == b->fileIdx;
  }
  if (a->arquivo && a->arquivo[0])
    return b->arquivo && b->arquivo[0] && !strcmp(a->arquivo, b->arquivo);
  if (!a->rotulo || !a->rotulo[0] || !b->rotulo ||
      strcmp(a->rotulo, b->rotulo)) return 0;
  // Filename-free URL entries need corroboration: label alone is ambiguous.
  return (a->bytes && b->bytes && a->bytes == b->bytes) ||
         (a->grupo && a->grupo[0] && b->grupo &&
          !strcmp(a->grupo, b->grupo));
}
#endif
