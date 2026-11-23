import space.nuvio.nativelegacy.CacheProvaP4C

private fun falhar(m: String): Nothing = error(m)

fun main() {
    val hash = "a".repeat(40)
    val base = CacheProvaP4C.Identidade("acct", 1, "tt12:2:3", "addon-1", hash, 7, 100_000)
    val assinatura = base.assinatura() ?: falhar("strong identity rejected")
    check(assinatura.length == 64 && !assinatura.contains("acct"))
    check(base.copy(conta = "other").assinatura() != assinatura)
    check(base.copy(perfil = 2).assinatura() != assinatura)
    check(base.copy(catalogo = "tt12:2:4").assinatura() != assinatura)
    check(base.copy(addon = "addon-2").assinatura() != assinatura)
    check(base.copy(arquivo = 8).assinatura() != assinatura)
    check(base.copy(torrent = "b".repeat(40)).assinatura() != assinatura)
    check(base.copy(bytes = 100_001).assinatura() != assinatura)
    check(base.copy(torrent = hash.uppercase()).assinatura() == assinatura)
    check(base.copy(torrent = "").assinatura() == null)
    check(base.copy(torrent = "not-a-hash").assinatura() == null)
    check(base.copy(arquivo = -1).assinatura() == null)
    check(base.copy(conta = "").assinatura() == null)
    check(base.copy(catalogo = "").assinatura() == null)
    check(base.copy(addon = "").assinatura() == null)

    val h1 = mapOf("Origin" to "https://app.example", "Authorization" to "Bearer secret")
    check(CacheProvaP4C.escopoCabecalhos(h1) ==
        CacheProvaP4C.escopoCabecalhos(mapOf("authorization" to "Bearer secret", "origin" to "https://app.example")))
    check(CacheProvaP4C.escopoCabecalhos(h1) !=
        CacheProvaP4C.escopoCabecalhos(h1 + ("Authorization" to "Bearer other")))
    check(!CacheProvaP4C.escopoCabecalhos(h1).contains("Bearer"))
    val a = "https://cdn.torbox.example/file/movie.mkv?token=old"
    val b = "https://cdn.torbox.example/file/movie.mkv?token=renewed"
    check(CacheProvaP4C.origem(a) == CacheProvaP4C.origem(b))
    check(CacheProvaP4C.origem("https://other.example/file/movie.mkv") != CacheProvaP4C.origem(a))
    check(CacheProvaP4C.origem("http://cdn.torbox.example/file/movie.mkv") == null)

    fun meta(etag: String = "\"abc\"", total: Long = 5000L, first: Long = 0L,
             end: Long = 0L, encoding: String = "identity") =
        mapOf("ETag" to listOf(etag), "Content-Range" to listOf("bytes $first-$end/$total"),
              "Content-Encoding" to listOf(encoding))
    val one = CacheProvaP4C.resposta206(a, meta(), 0, 1, 1) ?: falhar("valid 206 rejected")
    val rotation = CacheProvaP4C.resposta206(b, meta(), 0, 1, 1) ?: falhar("valid rotation rejected")
    check(one.mesma(rotation))
    check(!one.mesma(CacheProvaP4C.resposta206(b, meta(etag = "\"changed\""), 0, 1, 1)))
    check(!one.mesma(CacheProvaP4C.resposta206(b, meta(total = 5001), 0, 1, 1)))
    check(CacheProvaP4C.resposta206(b, meta(etag = "W/\"abc\""), 0, 1, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(etag = ""), 0, 1, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(first = 1, end = 1), 0, 1, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(end = 9), 0, 1, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(encoding = "gzip"), 0, 1, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(), -1, 1, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(), 0, 0, 1) == null)
    check(CacheProvaP4C.resposta206(b, meta(), 0, 1, 2) == null)
    println("P4-C provenance and HTTP validator policy: 32 checks PASS")
}
