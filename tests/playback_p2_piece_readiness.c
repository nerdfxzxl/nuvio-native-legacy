// P2-A selected-file verified-piece snapshot contract (host C11, no Android).
// This tests the exact pure selected-file guard used by vigiaFio. It does not
// certify a playable-buffer percentage or device/torrent network operation.
#include "p2pmotor.h"
#include <assert.h>
#include <stdint.h>
#include <stdio.h>

int main(void) {
  P2pmArquivoMedicao sample = { 7, 512ULL * 1024 * 1024, 0, 0 };
  const uint64_t size = sample.tamanho_arquivo;

  // Unknown availability and a genuine known zero are different states.
  assert(!p2pm_medicao_corresponde(NULL, 7, size));
  assert(p2pm_medicao_corresponde(&sample, 7, size));

  // Genuine verified pieces may be noncontiguous after a missing first piece.
  sample.bytes_verificados = 80ULL * 1024 * 1024;
  assert(p2pm_medicao_corresponde(&sample, 7, size));
  sample.prefixo_verificado = 60ULL * 1024 * 1024;
  assert(p2pm_medicao_corresponde(&sample, 7, size));

  // Wrong selected file or different size must never reuse previous stats.
  assert(!p2pm_medicao_corresponde(&sample, 6, size));
  assert(!p2pm_medicao_corresponde(&sample, 7, size - 1));
  assert(!p2pm_medicao_corresponde(&sample, -1, size));
  assert(!p2pm_medicao_corresponde(&sample, 7, 0));

  // Impossible engine counts are unknown, never coerced to a percent.
  sample.prefixo_verificado = 81ULL * 1024 * 1024;
  assert(!p2pm_medicao_corresponde(&sample, 7, size));
  sample.prefixo_verificado = 0;
  sample.bytes_verificados = size + 1;
  assert(!p2pm_medicao_corresponde(&sample, 7, size));

  // Legitimate 100%-verified file is still NOT a current-seek buffer metric.
  sample.prefixo_verificado = sample.bytes_verificados = size;
  assert(p2pm_medicao_corresponde(&sample, 7, size));

  puts("P2-A selected-file piece snapshot identity: PASS");
  return 0;
}
