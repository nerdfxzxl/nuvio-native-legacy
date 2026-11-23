package space.nuvio.nativelegacy

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Byte-for-byte integrity for completed Media3 disk spans in this process.
 * Media3's LRU/index tracks offsets, but does not cryptographically verify
 * cached file contents. Only completed, registered spans may be read.
 *
 * Pure JVM, no Media3 dependency; the CacheMidia adapter removes rejected
 * spans through SimpleCache.removeSpan before opening CacheDataSource.
 */
internal class CacheSpanP4C(private val raiz: File) {
    data class Trecho(val chave: String, val inicio: Long, val tamanho: Long, val arquivo: File)
    private data class Registro(val chave: String, val inicio: Long, val tamanho: Long, val caminho: String)
    private val digests = LinkedHashMap<Registro, String>()
    private val maxRegistros = 4096

    private fun registro(t: Trecho) = Registro(t.chave, t.inicio, t.tamanho, t.arquivo.absolutePath)

    // Never follow an unexpected symlink out of the per-session app cache dir.
    private fun digest(t: Trecho): String? {
        if (t.inicio < 0 || t.tamanho <= 0 || !t.arquivo.isFile ||
            t.arquivo.length() != t.tamanho) return null
        val base = try { raiz.canonicalPath + File.separator } catch (_: Exception) { return null }
        val dest = try { t.arquivo.canonicalPath } catch (_: Exception) { return null }
        if (!dest.startsWith(base) || dest != t.arquivo.absolutePath) return null
        return try {
            val hash = MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(64 * 1024)
            var total = 0L
            FileInputStream(t.arquivo).use { stream ->
                while (true) {
                    val n = stream.read(buf)
                    if (n == -1) break
                    if (n == 0) continue
                    total += n
                    if (total > t.tamanho) return null
                    hash.update(buf, 0, n)
                }
            }
            if (total != t.tamanho) null
            else hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        } catch (_: Exception) { null }
    }

    @Synchronized fun registrar(t: Trecho): Boolean {
        if (digests.size >= maxRegistros && !digests.containsKey(registro(t))) return false
        val h = digest(t) ?: return false
        digests[registro(t)] = h
        return true
    }

    @Synchronized fun valido(t: Trecho): Boolean {
        val anterior = digests[registro(t)] ?: return false
        return digest(t) == anterior
    }

    @Synchronized fun esquecer(t: Trecho) {
        digests.remove(registro(t))
    }

    // LRU eviction removes spans independently. Prune their digests too;
    // otherwise a long viewing session could grow the in-memory manifest.
    @Synchronized fun reterApenas(atual: Collection<Trecho>) {
        val keys = atual.mapTo(HashSet()) { registro(it) }
        digests.keys.retainAll(keys)
    }

    @Synchronized fun quantidade() = digests.size
}
