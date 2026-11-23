"""P4-C source contracts for the committed JNI/Media3 disk-cache integration.

Source/ownership checks only. JVM proof/span suites exercise actual bytes; a
source contract is NOT a substitute for API35/Media3, TorBox or TV tests.
Run from the repository checkout with: python3 tests/playback_p4c_source_contract.py
"""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
native = (root / "src/player.c").read_text()
jni = (root / "src/video_android.c").read_text()
header = (root / "src/cacheboost.h").read_text()
kotlin = root / "android/app/src/main/java/space/nuvio/nativelegacy"
player = (kotlin / "NvPlayer.kt").read_text()
cache = (kotlin / "CacheMidia.kt").read_text()
policy = (kotlin / "CacheProvaP4C.kt").read_text()
span = (kotlin / "CacheSpanP4C.kt").read_text()
session = (kotlin / "CacheSessao.kt").read_text()
parallel = (kotlin / "ParaleloDataSource.kt").read_text()
checks = []


def require(name: str, condition: bool) -> None:
    assert condition, f"P4-C source contract failed: {name}"
    checks.append(name)


require("native account/profile and exact selected catalog title",
        "cacheboost_backend_identidade(sessao_usuario(), perfis_ativo(), alvo," in native)
require("selected addon and torrent/file index",
        all(part in native for part in
            ("st->infoHash", "st->hashTorrent", "st->addonChave", "st->fileIdx")))
require("C/JNI/Kotlin descriptor agreement",
        '"cacheIdentity"' in jni and
        '(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IJ)V' in jni and
        'fun cacheIdentity(conta: String, perfil: Int, catalogo: String' in player and
        "void cacheboost_backend_identidade(" in header)
require("stable proof and signed URL separation",
        'listOf("p4c-v1", conta, perfil.toString(), catalogo, addon,' in policy and
        "torrent.lowercase" in policy and "arquivo.toString()" in policy)
require("header fingerprint includes auth/UA",
        "escopoCabecalhos" in policy and
        'if (ua != null) props + ("User-Agent" to ua) else props' in player)
require("strict strong validator and byte range",
        all(part in policy for part in
            ('!etag.startsWith("W/"', '"Content-Range"', '"Content-Encoding"',
             '"Content-Length"', "a != inicio", "b >= total")))
require("HTTP redirect resolved URL and one-byte signed-URL renewal probe",
        "val url = rede.uri?.toString() ?: spec.uri.toString()" in cache and
        "setPosition(0L).setLength(1L)" in cache and
        "resposta206(url, headers, 0, 1, 1)" in cache)
require("only verified warm URLs may alias original Media3 key",
        all(part in cache for part in
            ("urlPendente", "val aprovado = !liberacaoAgendada && validar(old)",
             "aliasAtiva = candidato", "chaveUrl(spec.uri.toString())")))
require("no reattach after scheduled release and no stale ownership",
        all(part in cache for part in
            ("c.liberacaoAgendada", "epochConfig != invalidacoes.get()",
             "invalidacoes.incrementAndGet()", "vaga.guardar(c,")))
require("real span digest checked before reading",
        "MessageDigest.getInstance(\"SHA-256\")" in span and
        "integridade.valido(t)" in cache and "cache.removeSpan(span)" in cache)
require("attempted file-hash normalization disallowed",
        "if (digests.containsKey(key)) return valido(t)" in span)
require("disk failure and space reserves",
        "CacheSessao.RESERVA_BYTES" in cache and
        "LeastRecentlyUsedCacheEvictor" in cache and
        "CacheSessao.limiteMb" in cache)
require("cancellation halts retry and remote validation",
        "conferirCancelamento()" in cache and
        "if (noCache || e is RepresentacaoMudou || !podeVoltarRede(e)) throw e" in cache and
        "r.cache?.fabrica(rede) { r.carga.cancelada }" in player)
require("changed representation cannot be silently mixed with cached prefix",
        "respostaAindaCompativel(d)" in cache and
        "throw RepresentacaoMudou()" in cache)
require("player release precedes storage release or handoff",
        "CacheMidia.guardar(c)" in player and
        "cacheParaLiberar?.let { c ->" in player and
        "r.cache?.liberar()" in player)
require("source/seek generation and next-episode protection preserved",
        "pedido != pedidos.get()" in player and
        "consultaPreparo" in player and
        "identidadeCache = identidade" in player)
require("no network counter increment for disk reads",
        "if (isNetwork && source !is ParaleloDataSource && bytesTransferred > 0)" in player and
        "recordBytes(" in parallel)
require("global setting indices unchanged, Automatic remains bounded",
        "RESERVA_BYTES = 512L shl 20" in session and
        "AUTOMATICO_MB = 2048" in session and
        ".coerceAtMost(1024L)" in session)

print(f"P4-C production source contracts PASS ({len(checks)} assertions)")
