// SDL render thread only. Tiles bound GPU usage even for very tall filenames.
#include <jni.h>
#include "perftv.h"
enum { CARD_TILE_H = 256, CARD_TILE_COUNT = 24, CARD_UPLOADS_PER_FRAME = 2 };
static int cardUploadsThisFrame;
typedef struct {
  GLuint texture;
  unsigned generation;
  int index, width, top, rows;
  uint64_t used;
} CardTile;
static CardTile cardTiles[CARD_TILE_COUNT];
static uint64_t cardTileUse;

void stream_card_gl_clear(void) {
  for (int i = 0; i < CARD_TILE_COUNT; ++i) {
    GLuint texture = cardTiles[i].texture;
    if (texture) {
      gfx_tex_esquecer(texture);
      glDeleteTextures(1, &texture);
    }
  }
  memset(cardTiles, 0, sizeof cardTiles);
}

static int cardException(JNIEnv *env) {
  if (!(*env)->ExceptionCheck(env)) return 0;
  (*env)->ExceptionClear(env);
  return 1;
}

// All local references below belong to the caller's PushLocalFrame.
static jobject cardLayout(JNIEnv *env, int index, int width, int sendPayload) {
  jobject activity = SDL_AndroidGetActivity();
  if (!activity) return NULL;
  jclass cls = (*env)->GetObjectClass(env, activity);
  if (!cls) return NULL;
  jmethodID layout = (*env)->GetMethodID(env, cls, "layoutStreamCardAsync",
      "(III[B)Lspace/nuvio/nativelegacy/StreamCardParagraphs;");
  if (!layout) return NULL;
  jint generation = (jint)listaGeracao;
  jobject row = (*env)->CallObjectMethod(env, activity, layout, generation, index, width, NULL);
  if (cardException(env) || row || !sendPayload) return row;
  const StreamDisplay *source = &lista[index].display;
  char *json = stream_display_read(source);
  if (!json) return NULL;
  jbyteArray payload = (*env)->NewByteArray(env, (jsize)source->length);
  if (payload) (*env)->SetByteArrayRegion(env, payload, 0, (jsize)source->length, (const jbyte *)json);
  free(json);
  if (!payload || cardException(env)) return NULL;
  return (*env)->CallObjectMethod(env, activity, layout, generation, index, width, payload);
}

static int cardMeasure(int index, int width, int sendPayload) {
  JNIEnv *env = SDL_AndroidGetJNIEnv();
  if (!env || (*env)->PushLocalFrame(env, 12) < 0) { if (env) cardException(env); return 0; }
  jobject row = cardLayout(env, index, width, sendPayload);
  int height = 0;
  if (!cardException(env) && row) {
    jclass cls = (*env)->GetObjectClass(env, row);
    jmethodID method = cls ? (*env)->GetMethodID(env, cls, "getHeight", "()I") : NULL;
    if (method) height = (*env)->CallIntMethod(env, row, method);
  }
  if (cardException(env)) height = 0;
  (*env)->PopLocalFrame(env, NULL);
  return height;
}

static GLuint cardTile(int index, int width, int top, int rows) {
  CardTile *slot = cardTiles;
  for (int i = 0; i < CARD_TILE_COUNT; ++i) {
    CardTile *t = &cardTiles[i];
    if (t->texture && t->generation == listaGeracao && t->index == index &&
        t->width == width && t->top == top) {
      t->used = ++cardTileUse;
      return t->texture;
    }
    if (t->used < slot->used) slot = t;
  }
  if (cardUploadsThisFrame >= CARD_UPLOADS_PER_FRAME) return 0;
  JNIEnv *env = SDL_AndroidGetJNIEnv();
  if (!env || (*env)->PushLocalFrame(env, 16) < 0) { if (env) cardException(env); return 0; }
  jobject row = cardLayout(env, index, width, 0);
  GLuint result = 0;
  if (!cardException(env) && row) {
    jclass cls = (*env)->GetObjectClass(env, row);
    jmethodID method = cls ? (*env)->GetMethodID(env, cls, "tileAsync", "(II)[I") : NULL;
    jintArray rgba = method ? (*env)->CallObjectMethod(env, row, method, top, rows) : NULL;
    if (!cardException(env) && rgba && (*env)->GetArrayLength(env, rgba) == width * rows) {
      jint *pixels = (*env)->GetIntArrayElements(env, rgba, NULL);
      if (pixels) {
        GLuint texture = slot->texture;
        if (!texture) glGenTextures(1, &texture);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        // Same-size replacements only change pixels; avoid reallocating GPU
        // storage on every LRU cache miss while scrolling long stream lists.
        if (slot->texture && slot->width == width && slot->rows == rows)
          glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, rows, GL_RGBA,
                          GL_UNSIGNED_BYTE, pixels);
        else
          glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, rows, 0, GL_RGBA,
                       GL_UNSIGNED_BYTE, pixels);
        (*env)->ReleaseIntArrayElements(env, rgba, pixels, JNI_ABORT);
        gfx_tex_esquecer(0);
        *slot = (CardTile){texture, listaGeracao, index, width, top, rows, ++cardTileUse};
        cardUploadsThisFrame++;
        if (perftv_enabled) perftv_uploads++;
        result = texture;
      }
    }
  }
  cardException(env);
  (*env)->PopLocalFrame(env, NULL);
  return result;
}

static int drawCardText(int index, int width, int height, float x, float y, float alpha) {
  int first = (int)fmaxf(0, FOLHA_TOPO - 8.0f - y) / CARD_TILE_H * CARD_TILE_H;
  for (int top = first; top < height && y + top < folhaBase; top += CARD_TILE_H) {
    int rows = height - top < CARD_TILE_H ? height - top : CARD_TILE_H;
    GLuint texture = cardTile(index, width, top, rows);
    if (!texture) return 0;
    gfx_tex_aspect_atual = 0;
    gfx_rect((GfxRect){x, y + top, width, rows}, texture, GFX_TEXTO, 0, 0, 0, 0, 1, 1, 1, alpha);
  }
  return 1;
}
