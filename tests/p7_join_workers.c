#include "p7_join_workers.h"
#include <assert.h>
#include <stdio.h>

static P7Workers workers = P7_WORKERS_INIT;
static pthread_key_t key;
static atomic_int finished, destructed, shutting;
static pthread_mutex_t gate = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t changed = PTHREAD_COND_INITIALIZER;
static int entered, released;

static void tls_cleanup(void *p) {
  assert(p == &workers);
  atomic_fetch_add(&destructed, 1);
}
static void *work(void *arg) {
  assert(arg == &workers);
  assert(!pthread_setspecific(key, arg));
  pthread_mutex_lock(&gate);
  entered++;
  pthread_cond_broadcast(&changed);
  while (!released) pthread_cond_wait(&changed, &gate);
  pthread_mutex_unlock(&gate);
  atomic_fetch_add(&finished, 1);
  return NULL;
}
static void *shutdown_worker(void *unused) {
  (void)unused;
  atomic_fetch_add(&shutting, 1);
  p7_workers_shutdown(&workers);
  assert(atomic_load(&finished) == 8);
  assert(atomic_load(&destructed) == 8);
  return NULL;
}
int main(void) {
  pthread_t a, b;
  assert(!pthread_key_create(&key, tls_cleanup));
  for (int i = 0; i < 8; ++i) assert(p7_workers_start(&workers, work, &workers));
  pthread_mutex_lock(&gate);
  while (entered != 8) pthread_cond_wait(&changed, &gate);
  pthread_mutex_unlock(&gate);
  assert(!pthread_create(&a, NULL, shutdown_worker, NULL));
  assert(!pthread_create(&b, NULL, shutdown_worker, NULL));
  /* Observe the actual closed flag under its mutex, not a sleep or guess. */
  for (;;) {
    pthread_mutex_lock(&workers.lock);
    int closed = workers.closed;
    pthread_mutex_unlock(&workers.lock);
    if (closed) break;
  }
  assert(!p7_workers_start(&workers, work, &workers));
  assert(!atomic_load(&destructed));
  pthread_mutex_lock(&gate);
  released = 1;
  pthread_cond_broadcast(&changed);
  pthread_mutex_unlock(&gate);
  assert(!pthread_join(a, NULL) && !pthread_join(b, NULL));
  assert(atomic_load(&shutting) == 2);
  p7_workers_shutdown(&workers);
  assert(!p7_workers_start(&workers, work, &workers));
  assert(!pthread_key_delete(key));
  puts("PASS: final worker ownership, concurrent shutdown and TLS destructor joins");
  return 0;
}
