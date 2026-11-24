// Identidade de um titulo: onde acaba o id "base" e comeca o episodio.
//
// POR QUE ISTO EXISTE. O app inteiro assumia "tt1234567" (IMDb) e cortava no
// PRIMEIRO ':' para separar o id do ":temporada:episodio". Vale para o IMDb e
// so para ele: um titulo de catalogo de addon de anime chega como "kitsu:123",
// "mal:456", "anilist:789" ou "xperience:abc", e o corte no primeiro ':' os
// reduzia a "kitsu"/"mal"/"anilist" — a mesma chave de progresso, de preferencia
// de fonte e de arte para TODOS os animes do mesmo addon (o vazamento que o #37
// ja tinha achado nos canais, "cs:channel:<hash>").
//
// A regra, uma so e com nome: id do IMDb ("tt...") corta no primeiro ':'; todo
// outro id e "<prefixo>:<id>" e corta no SEGUNDO ':' ("tmdb:t1399", "kitsu:41370").
// Mesma regra que arteesc_chave (arteescolha.c) ja usava sozinha.
//
// A suffix is stripped only when it is a complete numeric episode suffix.
// Other opaque colon-separated components remain part of the title identity.
//
// So funcoes estaticas no cabecalho: quem precisa da regra nao precisa linkar
// mais um modulo (alguns testes compilam pedacos do app sem o resto).
#ifndef NV_IDBASE_H
#define NV_IDBASE_H
#include <stddef.h>
#include <stdio.h>
#include <string.h>

// 1 quando o id e do IMDb.
static inline int idbase_e_imdb(const char *id) {
  return id && id[0] == 't' && id[1] == 't';
}

// Tamanho do id BASE (sem ":temporada:episodio"). 0 para id nulo.
static inline size_t idbase_len(const char *id) {
  size_t n;
  if (!id) return 0;
  n = strcspn(id, ":");
  if (!idbase_e_imdb(id) && id[n] == ':') n += 1 + strcspn(id + n + 1, ":");
  if (id[n] == ':') {
    const char *p = id + n + 1;
    int partes = 0;
    while (*p) {
      if (*p < '0' || *p > '9') return strlen(id);
      do { p++; } while (*p >= '0' && *p <= '9');
      partes++;
      if (!*p) break;
      if (*p++ != ':' || !*p || partes >= 2) return strlen(id);
    }
    if (!partes) return strlen(id);
  }
  return n;
}

// Exact persisted identity: reject unrepresentable values, never silently alias.
static inline int idbase_copiar_exato(const char *id, char *dst, size_t tam) {
  if (!dst || !tam) return 0;
  dst[0] = 0;
  size_t n = idbase_len(id);
  if (!n || n >= tam || strpbrk(id, " \t\r\n")) return 0;
  memcpy(dst, id, n); dst[n] = 0;
  return 1;
}

// Exact episode composition for fixed-width UI/playback identity buffers.
// A truncated or ambiguous composite is not a valid different episode.
static inline int idbase_compor_episodio(const char *base, int t, int e,
                                         char *dst, size_t tam) {
  if (!dst || !tam) return 0;
  dst[0] = 0;
  if (!base || !*base || t < 0 || e <= 0 || idbase_len(base) != strlen(base)) return 0;
  int n = snprintf(dst, tam, "%s:%d:%d", base, t, e);
  if (n < 0 || (size_t)n >= tam || idbase_len(dst) != strlen(base)) {
    dst[0] = 0;
    return 0;
  }
  return 1;
}

// Only the current IMDb history writers support a proven numeric IMDb id.
static inline int idbase_imdb_historico(const char *id) {
  size_t n = idbase_len(id);
  if (!idbase_e_imdb(id) || n < 3 || n >= 24) return 0;
  for (size_t i = 2; i < n; i++) if (id[i] < '0' || id[i] > '9') return 0;
  return 1;
}

// O id base copiado para `dst`.
static inline void idbase_copiar(const char *id, char *dst, size_t tam) {
  size_t n = idbase_len(id);
  if (!tam) return;
  if (n >= tam) n = tam - 1;
  if (n) memcpy(dst, id, n);
  dst[n] = 0;
}

// O episodio que o id carrega, se carrega. `*t`/`*e` ficam 0 sem episodio.
//   "tt1:2:5"       -> t=2 e=5
//   "kitsu:41370:5" -> t=-1 e=5  (absent season; explicit season 0 is Specials)
//   "kitsu:1:2:5"   -> t=2 e=5
static inline void idbase_episodio(const char *id, int *t, int *e) {
  const char *p;
  int a = -1, b = -1, n;
  if (t) *t = 0;
  if (e) *e = 0;
  if (!id) return;
  p = id + idbase_len(id);
  if (*p != ':') return;
  n = sscanf(p + 1, "%d:%d", &a, &b);
  if (n == 2) { if (t) *t = a; if (e) *e = b; }
  else if (n == 1) {
    if (idbase_e_imdb(id)) { if (t) *t = a; }     // como sempre foi para o IMDb
    else { if (t) *t = -1; if (e) *e = a; }
  }
}

// O id ja carrega episodio?
static inline int idbase_tem_episodio(const char *id) {
  return id && id[idbase_len(id)] == ':';
}

#endif
