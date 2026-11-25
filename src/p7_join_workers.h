#ifndef NV_P7_JOIN_WORKERS_H
#define NV_P7_JOIN_WORKERS_H
#include <pthread.h>
#include <stdatomic.h>
#include <stdlib.h>

typedef struct P7Worker {
  pthread_t thread;
  void *(*run)(void *);
  void *arg;
  atomic_int done;
  struct P7Worker *next;
} P7Worker;

typedef struct {
  pthread_mutex_t lock;
  pthread_cond_t idle;
  P7Worker *head;
  int closed, draining;
} P7Workers;

#define P7_WORKERS_INIT { PTHREAD_MUTEX_INITIALIZER, PTHREAD_COND_INITIALIZER, NULL, 0, 0 }

static inline void *p7_worker_run(void *arg) {
  P7Worker *w = arg;
  w->run(w->arg);
  atomic_store_explicit(&w->done, 1, memory_order_release);
  return NULL;
}

/* The caller keeps ownership of arg if creation fails. Reap only workers
 * whose functions have returned. Joining also waits for pthread TLS cleanup. */
static inline int p7_workers_start(P7Workers *g, void *(*run)(void *), void *arg) {
  pthread_mutex_lock(&g->lock);
  if (g->closed) { pthread_mutex_unlock(&g->lock); return 0; }
  P7Worker **p = &g->head;
  while (*p) {
    P7Worker *w = *p;
    if (!atomic_load_explicit(&w->done, memory_order_acquire)) {
      p = &w->next;
      continue;
    }
    pthread_join(w->thread, NULL);
    *p = w->next;
    free(w);
  }
  P7Worker *w = calloc(1, sizeof *w);
  if (!w) { pthread_mutex_unlock(&g->lock); return 0; }
  w->run = run;
  w->arg = arg;
  atomic_init(&w->done, 0);
  if (pthread_create(&w->thread, NULL, p7_worker_run, w)) {
    free(w);
    pthread_mutex_unlock(&g->lock);
    return 0;
  }
  w->next = g->head;
  g->head = w;
  pthread_mutex_unlock(&g->lock);
  return 1;
}

/* Final teardown only: revoke new work, then join without holding the group
 * mutex. Never call from a member worker. Repeated callers wait for the
 * original shutdown rather than mistaking an empty list for completed joins. */
static inline void p7_workers_shutdown(P7Workers *g) {
  pthread_mutex_lock(&g->lock);
  while (g->draining) pthread_cond_wait(&g->idle, &g->lock);
  if (g->closed) { pthread_mutex_unlock(&g->lock); return; }
  g->closed = g->draining = 1;
  P7Worker *w = g->head;
  g->head = NULL;
  pthread_mutex_unlock(&g->lock);
  while (w) {
    P7Worker *next = w->next;
    pthread_join(w->thread, NULL);
    free(w);
    w = next;
  }
  pthread_mutex_lock(&g->lock);
  g->draining = 0;
  pthread_cond_broadcast(&g->idle);
  pthread_mutex_unlock(&g->lock);
}
#endif
