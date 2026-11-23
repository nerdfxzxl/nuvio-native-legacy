import space.nuvio.nativelegacy.CacheSessao

fun main() {
    val mb = 1L shl 20
    check(CacheSessao.limiteMb(256, 768 * mb) == 256)
    check(CacheSessao.limiteMb(512, 768 * mb) == 256)
    check(CacheSessao.limiteMb(CacheSessao.AUTOMATICO_MB, 4096 * mb) == 512)
    check(CacheSessao.limiteMb(CacheSessao.AUTOMATICO_MB, 8192 * mb) == 1024)
    check(CacheSessao.limiteMb(CacheSessao.AUTOMATICO_MB, 1200 * mb) == 0)
    check(CacheSessao.limiteMb(CacheSessao.AUTOMATICO_MB, 0) == 0)
    val first = "https://cdn.torbox.example/movie.mkv?token=one"
    val rotated = "https://cdn.torbox.example/movie.mkv?token=two"
    val key = CacheSessao.chave("s1", first)
    check(key == CacheSessao.chave("s1", first + "#fragment"))
    check(key != CacheSessao.chave("s1", rotated)) // no unproven signed URL alias
    check(key != CacheSessao.chave("s2", first)) // distinct sessions
    check(key != CacheSessao.chave("s1", first, "releaseA:headersA"))
    check(CacheSessao.chave("s1", first, "releaseA:headersA") !=
          CacheSessao.chave("s1", first, "releaseB:headersA"))
    check(CacheSessao.chave("s1", first, "releaseA:headersA") !=
          CacheSessao.chave("s1", first, "releaseA:headersB"))
    check(!key.contains("token") && !key.contains("cdn") && !key.contains("one"))
    check(key.startsWith("v2:s1:") && key.length == 70)
    try { CacheSessao.chave("../s", first); error("invalid session") }
    catch (_: IllegalArgumentException) {}
    println("P4-C key isolation, rotation, disk sizing and reserve PASS")
}