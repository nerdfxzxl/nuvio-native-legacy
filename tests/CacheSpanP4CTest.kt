import space.nuvio.nativelegacy.CacheSpanP4C
import java.io.File
import java.nio.file.Files

fun main() {
    val root = Files.createTempDirectory("nv-p4c-span").toFile()
    val outside = Files.createTempDirectory("nv-p4c-outside").toFile()
    try {
        val registry = CacheSpanP4C(root)
        val file = File(root, "123.exo")
        val payload = ByteArray(128 * 1024) { (it * 29).toByte() }
        file.writeBytes(payload)
        val span = CacheSpanP4C.Trecho("source-signed-url-hash", 8192L, payload.size.toLong(), file)
        check(registry.registrar(span))
        check(registry.conhecido(span) && registry.valido(span))
        check(registry.quantidade() == 1)

        // Same file length, changed byte: not silently normalized by a second
        // registrar() call after a later completed write for the same key.
        file.writeBytes(payload.clone().also { it[8191] = (it[8191] + 1).toByte() })
        check(!registry.valido(span))
        // A trusted old hash must NOT be replaced by calling registrar again.
        check(!registry.valido(span))

        // Truncated interrupted write cannot be accepted.
        file.writeBytes(payload.copyOf(100))
        check(!registry.valido(span))
        check(!registry.registrar(CacheSpanP4C.Trecho("new", 0L, payload.size.toLong(), file)))

        // Invalid range coordinates or an escaped path are never trusted.
        file.writeBytes(payload)
        check(!registry.registrar(span.copy(inicio = -1L)))
        val otherFile = File(outside, "escape.exo")
        otherFile.writeBytes(payload)
        check(!registry.registrar(span.copy(arquivo = otherFile)))
        val link = File(root, "link.exo")
        Files.createSymbolicLink(link.toPath(), otherFile.toPath())
        check(!registry.registrar(span.copy(arquivo = link)))

        val different = span.copy(chave = "another")
        check(registry.registrar(different))
        check(registry.valido(different))
        check(!registry.conhecido(different.copy(inicio = different.inicio + 1)))
        registry.reterApenas(listOf(different))
        check(!registry.conhecido(span) && registry.conhecido(different))
        registry.esquecer(different)
        check(registry.quantidade() == 0)
        file.delete() // LRU eviction/removal while registry still exists
        check(!registry.valido(different))
        println("P4-C real span file hash/corruption/truncation/eviction checks PASS")
    } finally {
        root.deleteRecursively()
        outside.deleteRecursively()
    }
}
