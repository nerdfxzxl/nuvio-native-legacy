package space.nuvio.nativelegacy

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

/**
 * P4-C: fail-closed byte identity for the progressive VOD disk cache.
 * Catalog identity and torrent file proof are independent of a signed CDN URL.
 * Even this proof does NOT authorize reusing bytes: a strong HTTP representation
 * validator and a separately verified local span are also required.
 *
 * Pure JVM policy: no Android / Media3 dependency, exercised by host tests.
 */
internal object CacheProvaP4C {
    data class Identidade(
        val conta: String,
        val perfil: Int,
        val catalogo: String,
        val addon: String,
        val torrent: String,
        val arquivo: Int,
        val bytes: Long,
    ) {
        // No title/filename/size-only content identities. Unknown file index
        // or a non-hex torrent identifier cannot be considered immutable proof.
        fun forte(): Boolean =
            conta.isNotBlank() && perfil >= 0 && catalogo.isNotBlank() &&
            addon.isNotBlank() && arquivo >= 0 && bytes >= 0 &&
            (torrent.length == 40 || torrent.length == 64) &&
            torrent.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

        fun assinatura(): String? = if (!forte()) null else resumo(
            listOf("p4c-v1", conta, perfil.toString(), catalogo, addon,
                torrent.lowercase(Locale.ROOT), arquivo.toString(), bytes.toString()))
    }

    data class Representacao(
        val origem: String, // HTTPS origin + exact path, NEVER a signed query
        val etag: String,   // strong, quoted HTTP ETag
        val total: Long,
        val codificacao: String = "identity",
    ) {
        fun confiavel(): Boolean = origem.startsWith("https://") && !origem.contains('?') &&
            etag.length > 2 && etag.startsWith('"') && etag.endsWith('"') &&
            !etag.startsWith("W/", ignoreCase = true) &&
            total > 0 && codificacao == "identity"

        fun mesma(other: Representacao?): Boolean =
            other != null && confiavel() && other.confiavel() &&
            origem == other.origem && etag == other.etag && total == other.total &&
            codificacao == other.codificacao
    }

    // Every relevant HTTP request header, including auth, Referer, UA,
    // Accept-Encoding and cookies, participates in the representation scope.
    // Canonicalize names (case-insensitive) but never log header values.
    fun escopoCabecalhos(headers: Map<String, String>): String = resumo(
        listOf("p4c-h1") + headers.entries.map {
            it.key.trim().lowercase(Locale.ROOT) to it.value.trim()
        }.sortedWith(compareBy<Pair<String, String>>({ it.first }, { it.second }))
            .map { it.first + ":" + it.second })

    fun origem(url: String): String? = try {
        val u = URI(url)
        if (!u.scheme.equals("https", true) || u.host.isNullOrBlank() ||
            u.rawUserInfo != null || u.fragment != null) null
        else {
            val porta = if (u.port > 0 && u.port != 443) ":${u.port}" else ""
            "https://${u.host.lowercase(Locale.ROOT)}$porta${u.rawPath.ifEmpty { "/" }}"
        }
    } catch (_: Exception) { null }

    // A 206 alone is not proof. The response must describe the requested
    // byte interval exactly, including its real full-object length.
    fun resposta206(
        url: String,
        headers: Map<String, List<String>>,
        inicio: Long,
        pedido: Long,
        aberto: Long,
    ): Representacao? {
        if (inicio < 0 || pedido <= 0 || aberto <= 0 || aberto > pedido) return null
        fun h(nome: String) = headers.entries.firstOrNull { it.key.equals(nome, true) }
            ?.value?.firstOrNull()?.trim().orEmpty()
        val etag = h("ETag")
        val range = Regex("^bytes ([0-9]+)-([0-9]+)/([0-9]+)$", RegexOption.IGNORE_CASE)
            .matchEntire(h("Content-Range")) ?: return null
        val a = range.groupValues[1].toLongOrNull() ?: return null
        val b = range.groupValues[2].toLongOrNull() ?: return null
        val total = range.groupValues[3].toLongOrNull() ?: return null
        if (a != inicio || b < a || b >= total || (b - a + 1) != aberto) return null
        if (h("Content-Encoding").ifEmpty { "identity" }.lowercase(Locale.ROOT) != "identity") return null
        val o = origem(url) ?: return null
        return Representacao(o, etag, total).takeIf { it.confiavel() }
    }

    // Length-prefix fields: concatenation cannot alias after embedded colons
    // or newline, and SHA-256 never exposes account/header/token values in keys.
    private fun resumo(fields: List<String>): String {
        val d = MessageDigest.getInstance("SHA-256")
        for (f in fields) {
            val b = f.toByteArray(Charsets.UTF_8)
            d.update(byteArrayOf((b.size ushr 24).toByte(), (b.size ushr 16).toByte(),
                (b.size ushr 8).toByte(), b.size.toByte()))
            d.update(b)
        }
        return d.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
