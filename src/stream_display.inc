// Included by the addon parser on Android and by its host regression test.
#include <pthread.h>
#include <unistd.h>
#include "stream_display.h"

static pthread_mutex_t displayLock = PTHREAD_MUTEX_INITIALIZER;
static FILE *displaySpill;

static FILE *openDisplaySpill(void) {
#ifdef NV_ANDROID
  const char *dir = SDL_AndroidGetInternalStoragePath();
  char path[PATH_MAX];
  if (!dir || snprintf(path, sizeof path, "%s/.stream-text-XXXXXX", dir) >= (int)sizeof path)
    return NULL;
  int fd = mkstemp(path);
  if (fd < 0) return NULL;
  unlink(path); // Private, process-lifetime backing for unusually long rows.
  FILE *file = fdopen(fd, "w+b");
  if (!file) close(fd);
  return file;
#else
  return tmpfile();
#endif
}

static void captureDisplay(const char *p, const char *end, StreamDisplay *out) {
  const char *fields[] = {"name", "title", "description"};
  const char *parts[3];
  size_t sizes[3], len = 4; // brackets and two commas
  for (int i = 0; i < 3; ++i) {
    const char *start = valorRaiz(p, end, fields[i]);
    const char *q = start;
    if (q && *q == '"') {
      for (++q; q < end && *q != '"'; ++q)
        if (*q == '\\' && q + 1 < end) ++q;
    }
    if (start && *start == '"' && q < end) {
      parts[i] = start;
      sizes[i] = (size_t)(q + 1 - start);
    } else {
      parts[i] = "null";
      sizes[i] = 4;
    }
    len += sizes[i];
  }
  char *data = len <= sizeof out->local ? out->local : malloc(len);
  if (!data) return;
  char *cursor = data;
  *cursor++ = '[';
  for (int i = 0; i < 3; ++i) {
    memcpy(cursor, parts[i], sizes[i]);
    cursor += sizes[i];
    *cursor++ = i == 2 ? ']' : ',';
  }
  if (data == out->local) {
    out->length = len;
    return;
  }
  pthread_mutex_lock(&displayLock);
  if (!displaySpill) displaySpill = openDisplaySpill();
  if (displaySpill && fseeko(displaySpill, 0, SEEK_END) == 0) {
    off_t offset = ftello(displaySpill);
    if (offset >= 0 && fwrite(data, 1, len, displaySpill) == len && fflush(displaySpill) == 0) {
      out->offset = (uint64_t)offset;
      out->length = len;
    }
  }
  pthread_mutex_unlock(&displayLock);
  free(data);
}

// Caller owns the result. It contains exactly the three JSON values in order.
char *stream_display_read(const StreamDisplay *d) {
  if (!d->length || d->length > INT_MAX) return NULL;
  char *bytes = malloc(d->length + 1);
  if (!bytes) return NULL;
  if (d->length <= sizeof d->local) memcpy(bytes, d->local, d->length);
  else {
    pthread_mutex_lock(&displayLock);
    int ok = displaySpill && fseeko(displaySpill, (off_t)d->offset, SEEK_SET) == 0 &&
             fread(bytes, 1, d->length, displaySpill) == d->length;
    pthread_mutex_unlock(&displayLock);
    if (!ok) { free(bytes); return NULL; }
  }
  bytes[d->length] = 0;
  return bytes;
}
