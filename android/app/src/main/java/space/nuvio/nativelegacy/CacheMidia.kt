package space.nuvio.nativelegacy

import android.net.Uri
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.CacheSpan
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

// F07 (1.8): SEEK CACHE ON DISK FOR ONE PLAYER SESSION (Media3 side).
//
// CacheDataSource sits ABOVE the existing chain (NvPlayer: DefaultDataSource ->
// here -> ParaleloDataSource.Factory with the auth headers, Range and the
// parallel downloader). Spans already downloaded are read back from disk;
// gaps go upstream with the same DataSpec (position/length = Range), so
// headers, redirects and the 4 connections are untouched. Cache hits never
// reach ParaleloDataSource, so the StreamFit passive meter keeps counting
// socket bytes only.
//
// One SimpleCache per session in its own folder (CacheSessao), no database
// (the index lives in the folder and dies with it), LRU-bounded to the
// effective limit. Writes go through CacheSessao.Escrita: ENOSPC or any other
// write error turns the cache off for the session, playback continues.
// P4-C: cross-open reuse requires independent exact-release proof AND a new
// one-byte strong-ETag/length/range check against the remote representation.
// Unknown or changed objects fail closed to a fresh network cache. All cached
// spans have SHA-256 verification before they are read.
@androidx.annotation.OptIn(UnstableApi::class)
class CacheMidia private constructor(
    private val pasta: File,
    private val cache: SimpleCache,
    private val sessao: String,
    private val escopo: String,
    private val prova: String?,
    private val tituloProva: String,
    private val urlBase: String,
    val pedidoMb: Int,
    val limiteMb: Int,
    val estado: CacheSessao.Estado,
) {
    companion object {
        private const val TAG = "nuvio-cache"
        private const val ESCRITA_BUF = 128 * 1024
        // Canceled loaders unwind (interrupt -> close) before the cache goes.
        private const val ESPERA_LIBERAR_MS = 1500L
        private val boot = "b" + java.lang.Long.toString(System.currentTimeMillis(), 36)
        private var contador = 0
        private val fio: ScheduledExecutorService by lazy {
            Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "nv-cache").apply { isDaemon = true } }
        }

        fun raiz(dirCache: File) = File(dirCache, "nv-seek")

        // WARM RESUME (#492): the kept cache of the last title (CacheSessao.Vaga).
        private const val VIGIA_MS = 5L * 60L * 1000L
        private var raizVaga: File? = null
        private val vaga = CacheSessao.Vaga<CacheMidia> { it.liberar() }

        // Only a completed old Media3 player may enter this one-slot pool.
        // Keep the SimpleCache OPEN: a closed SimpleCache is never reattached.
        // A reused cache remains quarantined until its next loader performs
        // a fresh, one-byte verified 206 against the new signed URL.
        @Synchronized fun guardar(c: CacheMidia) {
            val chave = c.chaveGuarda()
            if (chave == null || c.liberacaoAgendada || c.estado.desligado ||
                c.representacao?.confiavel() != true ||
                c.integridade.quantidade() == 0 ||
                !c.spansExistem(c.chaveBase())) {
                c.liberar()
                return
            }
            raizVaga = c.pasta.parentFile?.parentFile
            val livre = try { raizVaga?.usableSpace ?: 0L }
                        catch (_: SecurityException) { 0L }
            if (vaga.guardar(c, c.tituloProva, chave, c.pedidoMb,
                    android.os.SystemClock.elapsedRealtime(), livre)) {
                Log.i(TAG, "verified VOD cache kept in process (no URL or token logged)")
                vigiar()
            }
        }

        @Synchronized private fun pegar(titulo: String, chave: String, pedidoMb: Int,
                                        aoFalhar: (Boolean) -> Unit,
                                        url: String, livre: Long): CacheMidia? {
            val c = vaga.pegar(titulo, chave, pedidoMb,
                android.os.SystemClock.elapsedRealtime()) ?: return null
            val origem = CacheProvaP4C.origem(url)
            if (livre < CacheSessao.RESERVA_BYTES ||
                CacheSessao.limiteMb(pedidoMb, livre) < c.limiteMb ||
                c.liberacaoAgendada || c.estado.desligado ||
                origem == null) {
                // Remote redirect target is not known yet. Do NOT compare
                // input URL with the old CDN response: the loader probes the
                // actual redirected target before allowing any cache hit.
                c.liberar()
                return null
            }
            c.estado.aoFalhar = aoFalhar
            c.urlPendente = url // new signed URL is NOT trusted yet
            c.aliasAtiva = null
            Log.i(TAG, "warm seek cache candidate; awaiting remote 206 proof")
            return c
        }

        // Another title, setting off, profile switch.
        fun descartar(motivo: String) { if (vaga.largar()) Log.i(TAG, "kept cache dropped: $motivo") }

        private fun vigiar() {
            fio.schedule({
                val livre = try { raizVaga?.usableSpace ?: 0L } catch (e: SecurityException) { 0L }
                if (vaga.vencer(android.os.SystemClock.elapsedRealtime(), livre)) Log.i(TAG, "kept cache dropped: time or space")
                else if (vaga.titulo() != null) vigiar()
            }, VIGIA_MS, TimeUnit.MILLISECONDS)
        }

        // App start: crash leftovers of earlier processes, off the main thread.
        fun limparTudo(dirCache: File) {
            // Never recursively delete a live SimpleCache owned by a player.
            descartar("clear requested")
            limparSobras(dirCache)
        }

        fun limparSobras(dirCache: File) {
            fio.execute {
                val n = CacheSessao.limparSobras(raiz(dirCache), boot)
                if (n > 0) Log.i(TAG, "removed $n leftover cache folder(s)")
            }
        }

        // Session cache or the reason there is none: (cache, state).
        fun criar(dirCache: File, pedidoMb: Int, aoFalhar: (Boolean) -> Unit,
                  escopo: String = "", prova: String? = null,
                  titulo: String = "", url: String = ""): Pair<CacheMidia?, Int> {
            if (pedidoMb <= 0) return null to CacheSessao.DESLIGADO
            val r = raiz(dirCache)
            r.mkdirs()
            val livre = try { r.usableSpace } catch (e: SecurityException) { 0L }
            val limite = CacheSessao.limiteMb(pedidoMb, livre)
            if (limite <= 0) {
                Log.i(TAG, "off: ${livre shr 20} MB free for $pedidoMb MB + reserve")
                return null to CacheSessao.POUCO_ESPACO
            }
            // A fresh identity includes account/profile/catalog/addon/torrent
            // file and the entire relevant request-header fingerprint.
            if (prova != null && titulo.isNotEmpty() && url.isNotEmpty()) {
                val retida = pegar(titulo, prova + ":" + escopo, pedidoMb,
                    aoFalhar, url, livre)
                if (retida != null) return retida to CacheSessao.ATIVO
            }
            val sessao = "s" + (++contador)
            val pasta = CacheSessao.pastaSessao(r, boot, sessao)
            return try {
                CacheSessao.apagar(pasta)
                pasta.mkdirs()
                @Suppress("DEPRECATION")
                val c = SimpleCache(pasta, LeastRecentlyUsedCacheEvictor(limite.toLong() shl 20))
                Log.i(TAG, "on: $limite MB (asked $pedidoMb, ${livre shr 20} MB free)")
                CacheMidia(pasta, c, sessao, escopo, prova, titulo, url,
                    pedidoMb, limite, CacheSessao.Estado(aoFalhar)) to CacheSessao.ATIVO
            } catch (e: Exception) {
                Log.w(TAG, "cache init failed: $e")
                fio.execute { CacheSessao.apagar(pasta) }
                null to CacheSessao.FALHOU
            }
        }
    }


    private val integridade = CacheSpanP4C(pasta)
    private val guarda = Any()
    // A previously verified signed URL is only an immutable *key* in our
    // private SimpleCache. It is never used as an HTTP request on renewal.
    private fun chaveBase() = CacheSessao.chave(sessao, urlBase, escopo)
    private fun chaveGuarda(): String? =
        if (prova != null && tituloProva.isNotEmpty() && urlBase.isNotEmpty())
            prova + ":" + escopo else null

    @Volatile private var urlPendente: String? = null
    @Volatile private var aliasAtiva: String? = null
    private fun chaveUrl(url: String): String =
        if (aliasAtiva == url && urlBase.isNotEmpty()) chaveBase()
        else CacheSessao.chave(sessao, url, escopo)

    // Called from the NEW Media3 loader. The retired player has fully
    // stopped by the time it placed this cache in the warm slot. No network
    // request may occur on the main Android UI thread.
    private fun validarRetomada(upstream: DataSource.Factory, spec: DataSpec): Boolean? {
        synchronized(guarda) {
            val candidato = urlPendente ?: return null
            if (candidato != spec.uri.toString()) return null
            urlPendente = null // exactly one validation per retained ownership
            val old = chaveBase()
            val aprovado = !liberacaoAgendada && validar(old) &&
                spansExistem(old) && validarRemoto(upstream, spec)
            if (aprovado) {
                aliasAtiva = candidato
            } else {
                aliasAtiva = null
                descartarChave(old)
                representacao = null
            }
            return aprovado
        }
    }

    private fun trecho(span: CacheSpan): CacheSpanP4C.Trecho? =
        span.file?.let { CacheSpanP4C.Trecho(span.key, span.position, span.length, it) }

    private fun podarRegistro() {
        val todos = cache.keys.flatMap { k -> cache.getCachedSpans(k).mapNotNull { trecho(it) } }
        integridade.reterApenas(todos)
    }

    private fun registrar(spec: DataSpec?) {
        if (spec == null || liberacaoAgendada) return
        val chave = chaveUrl(spec.uri.toString())
        synchronized(guarda) {
            if (liberacaoAgendada) return
            try {
                for (span in cache.getCachedSpans(chave).toList()) {
                    val t = trecho(span)
                    // Never replace an existing digest with corrupted bytes.
                    if (t == null || (!integridade.conhecido(t) && !integridade.registrar(t))) {
                        cache.removeSpan(span)
                        if (t != null) integridade.esquecer(t)
                    }
                }
                podarRegistro()
            } catch (e: Exception) { estado.falhou(e) }
        }
    }

    private fun validar(chave: String): Boolean {
        synchronized(guarda) {
            if (liberacaoAgendada || estado.desligado) return false
            return try {
                var fim = -1L
                for (span in cache.getCachedSpans(chave).toList()) {
                    val t = trecho(span)
                    val valido = t != null && span.position >= fim &&
                        span.position >= 0 && span.length > 0 &&
                        span.position <= Long.MAX_VALUE - span.length &&
                        integridade.valido(t)
                    if (!valido) {
                        cache.removeSpan(span)
                        if (t != null) integridade.esquecer(t)
                    } else fim = span.position + span.length
                }
                podarRegistro()
                true
            } catch (e: Exception) {
                estado.falhou(e)
                false
            }
        }
    }

    private fun descartarChave(chave: String) {
        synchronized(guarda) {
            if (liberacaoAgendada) return
            try {
                for (span in cache.getCachedSpans(chave).toList()) {
                    cache.removeSpan(span)
                    trecho(span)?.let { integridade.esquecer(it) }
                }
                podarRegistro()
            } catch (e: Exception) { estado.falhou(e) }
        }
    }

    private class SinkProtegido(cache: SimpleCache, estado: CacheSessao.Estado, private val pasta: File,
                                private val registrar: (DataSpec?) -> Unit) : DataSink {
        // bufferSize 0: Escrita buffers (see CacheSessao.Escrita).
        private val interno = CacheDataSink(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE, 0)
        private var spec: DataSpec? = null
        private val escrita = CacheSessao.Escrita(estado, ESCRITA_BUF,
            { interno.open(spec!!) }, { b, o, n -> interno.write(b, o, n) }, { interno.close() })
        override fun open(dataSpec: DataSpec) { spec = dataSpec; escrita.abrir() }
        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            // Another app may consume storage after cache initialization.
            // Never write into the reserved 512 MiB of free disk space.
            if (length > 0 && !estado.desligado) {
                val livre = try { pasta.usableSpace } catch (_: SecurityException) { 0L }
                if (livre < CacheSessao.RESERVA_BYTES)
                    estado.falhou(java.io.IOException("ENOSPC: cache reserve depleted"))
            }
            escrita.escrever(buffer, offset, length)
        }
        override fun close() {
            val terminado = spec
            escrita.fechar()
            spec = null
            registrar(terminado)
        }
    }

    @Volatile private var representacao: CacheProvaP4C.Representacao? = null

    private fun spansExistem(chave: String): Boolean = try {
        cache.getCachedSpans(chave).isNotEmpty()
    } catch (e: Exception) {
        estado.falhou(e)
        false
    }

    // One bounded one-byte 206 probe only when a NEW DataSource open might
    // consume previously cached spans. Retain all signed URL/request headers.
    // Missing/weak validator, changed object, redirect origin, 200 response,
    // gzip, timeouts or storage error => do NOT consume cached bytes.
    private fun validarRemoto(upstream: DataSource.Factory, spec: DataSpec): Boolean {
        val esperado = representacao ?: return false
        if (spec.httpRequestHeaders.keys.any { it.equals("Range", true) }) return false
        val probe = try { spec.buildUpon().setPosition(0L).setLength(1L).build() }
                    catch (_: Exception) { return false }
        val rede = try { upstream.createDataSource() } catch (_: Exception) { return false }
        return try {
            if (rede.open(probe) <= 0) return false
            val headers = rede.responseHeaders
            val url = rede.uri?.toString() ?: spec.uri.toString()
            val recebida = CacheProvaP4C.resposta206(url, headers, 0, 1, 1)
            if (!esperado.mesma(recebida)) return false
            val byte = ByteArray(1)
            rede.read(byte, 0, 1) == 1
        } catch (_: Exception) {
            false
        } finally {
            try { rede.close() } catch (_: Exception) {}
        }
    }

    private fun observarResposta(d: DataSource, spec: DataSpec): Boolean {
        val headers = d.responseHeaders
        if (headers.isEmpty()) return true // cache-only hit; already probed
        val url = d.uri?.toString() ?: spec.uri.toString()
        val nova = CacheProvaP4C.observada206(url, headers, spec.position)
        val antiga = representacao
        if (antiga != null && (nova == null || !antiga.mesma(nova))) return false
        if (nova != null && antiga == null) representacao = nova
        return true
    }

    private class RepresentacaoMudou : IOException("P4-C: HTTP byte representation changed")

    // A CacheDataSource may transition from a previously verified disk span
    // into an uncached HTTP hole DURING read(), not during open(). Inspect the
    // actual current response before returning that read's bytes to Media3.
    private fun respostaAindaCompativel(d: DataSource): Boolean {
        val esperada = representacao ?: return true
        val headers = d.responseHeaders
        if (headers.isEmpty()) return true // disk-only read, no HTTP response
        val range = headers.entries.firstOrNull { it.key.equals("Content-Range", true) }
            ?.value?.firstOrNull()?.trim().orEmpty()
        val inicio = Regex("^bytes ([0-9]+)-").find(range)?.groupValues?.get(1)
            ?.toLongOrNull() ?: return false
        val url = d.uri?.toString() ?: return false
        return esperada.mesma(CacheProvaP4C.observada206(url, headers, inicio))
    }

    fun fabrica(upstream: DataSource.Factory): DataSource.Factory {
        val media3 = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory {
                SinkProtegido(cache, estado, pasta) { registrar(it) }
            }
            .setCacheKeyFactory(CacheKeyFactory { spec ->
                chaveUrl(spec.uri.toString())
            })
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Validate full completed spans before Media3 can select any cached
        // bytes. A failed cache read resumes the original HTTP DataSpec at
        // the precise delivered offset, never doubling network-byte telemetry.
        return DataSource.Factory {
            object : DataSource {
                private val listeners = ArrayList<TransferListener>()
                private var atual: DataSource? = null
                private var original: DataSpec? = null
                private var chave = ""
                private var consumidos = 0L
                private var noCache = false

                private fun criar(factory: DataSource.Factory): DataSource =
                    factory.createDataSource().also { d ->
                        for (listener in listeners) d.addTransferListener(listener)
                    }

                private fun usarRede(): Long {
                    val spec = original ?: throw IOException("DataSpec not opened")
                    try { atual?.close() } catch (_: Exception) {}
                    descartarChave(chave)
                    representacao = null
                    noCache = true
                    val d = criar(upstream)
                    return try {
                        val restante = d.open(spec.subrange(consumidos))
                        atual = d
                        restante
                    } catch (e: Exception) {
                        try { d.close() } catch (_: Exception) {}
                        throw e
                    }
                }

                override fun open(dataSpec: DataSpec): Long {
                    original = dataSpec
                    consumidos = 0L
                    val retomadaValidada = validarRetomada(upstream, dataSpec)
                    chave = chaveUrl(dataSpec.uri.toString())
                    val haviaSpans = spansExistem(chave)
                    noCache = !validar(chave) ||
                        (haviaSpans && retomadaValidada != true &&
                         !validarRemoto(upstream, dataSpec))
                    if (haviaSpans && noCache) {
                        descartarChave(chave)
                        representacao = null
                    }
                    val d = criar(if (noCache) upstream else media3)
                    atual = d
                    return try {
                        val n = d.open(dataSpec)
                        if (!noCache && !observarResposta(d, dataSpec)) {
                            // Changed upstream object after the initial proof.
                            // Never mix old spans with a new HTTP representation.
                            usarRede()
                        } else n
                    } catch (e: IOException) {
                        if (noCache) throw e
                        usarRede()
                    }
                }

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    val d = atual ?: throw IOException("DataSource closed")
                    return try {
                        val n = d.read(buffer, offset, length)
                        if (n > 0 && !noCache && !respostaAindaCompativel(d)) {
                            // Never combine the already delivered cached prefix
                            // with a newly changed upstream object. A new player
                            // generation must reopen the source from scratch.
                            descartarChave(chave)
                            representacao = null
                            throw RepresentacaoMudou()
                        }
                        if (n > 0) consumidos += n
                        n
                    } catch (e: IOException) {
                        if (noCache || e is RepresentacaoMudou) throw e
                        usarRede()
                        val n = atual!!.read(buffer, offset, length)
                        if (n > 0) consumidos += n
                        n
                    }
                }

                override fun getUri(): Uri? = atual?.uri
                override fun getResponseHeaders(): Map<String, List<String>> =
                    atual?.responseHeaders ?: emptyMap()
                override fun addTransferListener(transferListener: TransferListener) {
                    listeners.add(transferListener)
                    atual?.addTransferListener(transferListener)
                }
                override fun close() {
                    val d = atual
                    atual = null
                    original = null
                    d?.close()
                }
            }
        }
    }

    fun usadoMb(): Int = try { (cache.cacheSpace shr 20).toInt() } catch (e: Exception) { 0 }

    // Player closed / source changed: release and delete the folder, later and
    // off the main thread.
    @Volatile private var liberacaoAgendada = false

    @Synchronized fun liberar() {
        if (liberacaoAgendada) return
        liberacaoAgendada = true
        fio.schedule({
            try { cache.release() } catch (e: Exception) { Log.w(TAG, "release: $e") }
            CacheSessao.apagar(pasta)
        }, ESPERA_LIBERAR_MS, TimeUnit.MILLISECONDS)
    }
}
