package space.nuvio.nativelegacy

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

// DOWNLOAD EM VARIAS CONEXOES PARA ARQUIVO DE VIDEO PROGRESSIVO (MKV/MP4).
//
// POR QUE: o Android limita o buffer de recepcao de UMA conexao TCP (na TCL
// Smart TV Pro, tcp_rmem maximo de 2 MB; app comum nao passa de 1 MB por
// socket). A 250 ms de distancia isso da ~20 Mbps por conexao — exatamente os
// 18 Mbps que o teste de velocidade do app media contra o debrid na Europa,
// contra 60 na LG e ~300 num speedtest com servidor perto (medido em
// 30/09/2026: Hetzner EUA 27 Mbps na TV, 97 no Mac da mesma rede). Varias
// conexoes somam: o arquivo vem em pedacos de Range baixados ao mesmo tempo
// e entregues ao ExoPlayer EM ORDEM.
//
// So entra para http(s) que nao seja manifesto/segmento de HLS/DASH (esses ja
// sao pedacos pequenos) e so se o servidor honrar Range (206 com Content-Range).
// Senao, devolve o fluxo da conexao unica, como o DefaultHttpDataSource.
internal class FalhaDownloadParalelo(causa: IOException) : IOException(causa)

// DESTINO DO LINK DO ADDON, por sessao do player. O link do addon (AIOStreams,
// Torrentio, MediaFusion...) redireciona para o CDN do debrid, e cada abertura
// nova do Media3 (o indice no fim do arquivo, a volta ao inicio, cada seek)
// pagava esse redirecionamento de novo: 1-2 s pelo Torrentio, ate 10 s pelo
// MediaFusion no Shield (16/10/2026). Guardado o destino, a abertura seguinte
// vai direto ao CDN; se ele recusar (link vencido, 403/410) ou cair, volta UMA
// vez ao link do addon, que pede um novo. Mesma ideia do cdnlink.c do .tpk
// (PR #567), aqui dentro do proprio DataSource. Nunca loga o destino: o link
// do CDN carrega token.
class DestinosAddon(private val agora: () -> Long = { android.os.SystemClock.elapsedRealtime() }) {
    companion object {
        // O TorBox da ~3 h ao link do CDN; o resto do caminho cobre a sobra.
        const val VALIDADE_MS = 2L * 60 * 60 * 1000
    }
    private var original: String? = null
    private var destino: URL? = null
    private var tamanho = -1L
    private var desde = 0L

    @Synchronized fun buscar(orig: String): URL? =
        if (orig == original && agora() - desde < VALIDADE_MS) destino else null

    // Tamanho do arquivo que o destino guardado serviu (-1 = desconhecido).
    // Uma resposta "OK" do destino com OUTRO tamanho nao e este arquivo: um
    // CDN vencido pode responder 200 com um clipe de erro em vez de 403.
    @Synchronized fun tamanhoDe(orig: String): Long = if (orig == original) tamanho else -1L

    // So guarda um redirecionamento de verdade (host diferente).
    @Synchronized fun lembrar(orig: String, fim: URL, total: Long = -1L) {
        val o = try { URL(orig) } catch (_: java.net.MalformedURLException) { return }
        if (fim.host.isNullOrEmpty() || fim.host.equals(o.host, true)) return
        if (orig == original && fim == destino && (total <= 0 || total == tamanho)) return
        original = orig; destino = fim; tamanho = if (total > 0) total else -1L; desde = agora()
    }

    @Synchronized fun esquecer(orig: String) {
        if (orig == original) { original = null; destino = null }
    }
}

/** Nao repete no Loader o ciclo de tentativas que este DataSource ja esgotou. */
internal class PoliticaErroParalelo : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        var causa: Throwable? = loadErrorInfo.exception
        repeat(16) {
            if (causa is FalhaDownloadParalelo) return C.TIME_UNSET
            causa = causa?.cause ?: return super.getRetryDelayMsFor(loadErrorInfo)
        }
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}

class ParaleloDataSource(
    private val ua: String?,
    private val props: Map<String, String>,
    private val conexoes: Int = CONEXOES,
    private val pedaco: Int = PEDACO,
    // StreamFit passivo (PassivoMedidor.kt): conta os bytes LIDOS DO SOCKET
    // por estas transferencias, nunca a entrega de memoria ao ExoPlayer.
    private val medidor: PassivoMedidor? = null,
    private val token: Long = 0L,
    private val destinos: DestinosAddon? = null,
    // Injetaveis para os testes de falha/cancelamento; producao usa a rede real.
    private val novaConexao: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val dormir: (Long) -> Unit = { Thread.sleep(it) },
    val stats: Estat = Estat(),
    private val serialOnly: java.util.concurrent.atomic.AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean(),
) : BaseDataSource(true) {

    // ESTATISTICAS AO VIVO (src/estatvivo.h, via NvPlayer.nativeStats): o que
    // ESTAS transferencias contam, por abertura do player (NvPlayer zera). So
    // contadores atomicos, nenhuma alocacao no caminho do byte.
    class Estat {
        val bytes = java.util.concurrent.atomic.AtomicLong()
        val conexoes = java.util.concurrent.atomic.AtomicInteger()
        @Volatile var usado = false
        @Volatile var primeiroByteMs = -1
        @Volatile var host = ""
        fun hostDe(u: URL) {
            val porta = if (u.port < 0 || u.port == u.defaultPort) "" else ":${u.port}"
            host = (u.host ?: "").lowercase() + porta
        }
    }

    companion object {
        private const val TAG = "nuvio-paralelo"
        const val CONEXOES = 4
        const val PEDACO = 4 shl 20           // 4 MB por pedido
        private val rangePattern = Regex("""bytes (\d+)-(\d+)/(\d+)""", RegexOption.IGNORE_CASE)
        private const val FULL_BODY = -2L
        private fun rangeTotal(c: HttpURLConnection, start: Long, requestedEnd: Long): Long {
            val match = rangePattern.matchEntire(c.getHeaderField("Content-Range")?.trim().orEmpty()) ?: return -1
            val first = match.groupValues[1].toLongOrNull() ?: return -1
            val last = match.groupValues[2].toLongOrNull() ?: return -1
            val size = match.groupValues[3].toLongOrNull() ?: return -1
            val expected = if (requestedEnd < 0) size - 1 else minOf(requestedEnd, size - 1)
            val length = c.contentLengthLong
            return if (size > 0 && first == start && last >= first && last == expected &&
                (length < 0 || length == last - first + 1)) size else -1
        }
        private const val TENTATIVAS = 3
        private val ESPERA_TENTATIVA_MS = longArrayOf(250L, 750L)
        // O primeiro pedido e menor: a imagem nao espera 4 MB chegarem.
        private const val PRIMEIRO = 1 shl 20
        // Menor pedaco do FIM DO ARQUIVO (ver `cauda` em abrir).
        private const val PEDACO_CAUDA_MIN = 128 shl 10

        // Manifesto e segmento de streaming adaptativo: conexao unica.
        fun serve(uri: Uri): Boolean {
            val s = uri.scheme ?: return false
            if (!s.equals("http", true) && !s.equals("https", true)) return false
            val p = (uri.path ?: "").lowercase()
            return !(p.endsWith(".m3u8") || p.endsWith(".mpd") || p.endsWith(".ts") ||
                p.endsWith(".m4s") || p.endsWith(".aac") || p.endsWith(".vtt") ||
                p.endsWith(".srt") || p.contains("/manifest"))
        }
    }

    class Factory(private val ua: String?, private val props: Map<String, String>,
                  private val unica: HttpDataSource.Factory,
                  private val medidor: PassivoMedidor? = null,
                  private val token: Long = 0L, private val stats: Estat = Estat(),
                  private val connections: Int = CONEXOES, private val chunk: Int = PEDACO) : DataSource.Factory {
        // Uma fabrica por sessao do player (NvPlayer): o destino vale para todas
        // as aberturas dela.
        private val destinos = DestinosAddon()
        private val serialOnly = java.util.concurrent.atomic.AtomicBoolean(connections <= 1)
        override fun createDataSource(): DataSource = Seletor(ua, props, unica.createDataSource(), medidor, token, destinos, stats, connections, chunk, serialOnly)
    }

    // Escolhe por pedido: paralelo para arquivo progressivo, unica para o resto.
    // Os ouvintes de transferencia (medidor de banda do ExoPlayer) vao para os dois.
    private class Seletor(private val ua: String?, private val props: Map<String, String>,
                          private val unica: DataSource,
                          private val medidor: PassivoMedidor?, private val token: Long,
                          private val destinos: DestinosAddon, private val stats: Estat,
                          private val connections: Int, private val chunk: Int,
                          private val serialOnly: java.util.concurrent.atomic.AtomicBoolean) : DataSource {
        private var atual: DataSource? = null
        private val ouvintes = ArrayList<androidx.media3.datasource.TransferListener>()
        override fun open(dataSpec: DataSpec): Long {
            val d = if (serve(dataSpec.uri) && dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET && dataSpec.httpBody == null)
                ParaleloDataSource(ua, props + dataSpec.httpRequestHeaders, connections, chunk,
                    medidor, token, destinos, stats = stats, serialOnly = serialOnly).also { p ->
                for (o in ouvintes) p.addTransferListener(o)
            } else unica
            atual = d
            return d.open(dataSpec)
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            atual?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
        override fun getUri(): Uri? = atual?.uri
        override fun getResponseHeaders(): Map<String, List<String>> = atual?.responseHeaders ?: emptyMap()
        override fun close() { try { atual?.close() } finally { atual = null } }
        override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
            ouvintes.add(transferListener)
            unica.addTransferListener(transferListener)
        }
    }

    private var uri: Uri? = null
    private var urlFinal: URL? = null
    private var response: Map<String, List<String>> = emptyMap()
    private var pos = 0L          // proximo byte a entregar
    private class UnreliableRange : IOException("Server did not honor HTTP Range")
    private var serialSpec: DataSpec? = null
    private var serialRemaining = -1L
    private var validator: String? = null
    private var fim = 0L          // exclusivo
    private var total = C.LENGTH_UNSET.toLong()
    private var exec: ExecutorService? = null
    private val pedidos = java.util.concurrent.ConcurrentHashMap<Long, Future<ByteArray>>()   // inicio do pedaco -> dados
    private var proximoPedido = 0L
    private var pedacoAtual = pedaco
    private var buf: ByteArray? = null
    private var bufIni = 0L
    // Conexao unica: servidor sem Range, ou arquivo pequeno.
    private var unicaCon: HttpURLConnection? = null
    private var unicaIn: InputStream? = null
    private var unicaOrigem: String? = null
    private var aberto = false
    @Volatile private var fechado = false
    // Future.cancel(true) nao interrompe connect/read de HttpURLConnection em
    // todos os Androids. Guardar as conexoes permite que close() as desconecte.
    private val ativas = Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<HttpURLConnection, Boolean>())

    private class Cancelamento(causa: Throwable? = null) : InterruptedIOException("cancelado") {
        init { if (causa != null) initCause(causa) }
    }

    private fun cancelado(causa: Throwable? = null): InterruptedIOException = Cancelamento(causa)

    private fun foiCancelado(e: IOException): Boolean =
        fechado || Thread.currentThread().isInterrupted || e is Cancelamento

    private fun conferirCancelamento() {
        if (fechado || Thread.currentThread().isInterrupted) throw cancelado()
    }

    private fun desconectar(c: HttpURLConnection?) {
        if (c == null) return
        ativas.remove(c)
        c.disconnect()
    }

    private fun esperarTentativa(indice: Int, atrasoMs: Long = -1L) {
        conferirCancelamento()
        val ms = if (atrasoMs >= 0) atrasoMs
                   else ESPERA_TENTATIVA_MS[indice.coerceAtMost(ESPERA_TENTATIVA_MS.lastIndex)]
        if (ms <= 0) return
        try { dormir(ms) }
        catch (e: InterruptedException) { Thread.currentThread().interrupt(); throw cancelado(e) }
        conferirCancelamento()
    }

    private fun conectar1(u: URL, ini: Long, ultimo: Long): HttpURLConnection {
        conferirCancelamento()
        val c = novaConexao(u)
        ativas.add(c)
        if (fechado) { desconectar(c); throw cancelado() }
        c.connectTimeout = 15000
        c.readTimeout = 20000
        // Redirecionamento seguido A MAO (conectar): o HttpURLConnection nao
        // segue troca de protocolo (https -> http), e o AIOStreams/ElfHosted faz
        // exatamente isso — a fonte ficava em "Opening source" com um 302.
        c.instanceFollowRedirects = false
        if (ua != null) c.setRequestProperty("User-Agent", ua)
        for ((k, v) in props) if (!k.equals("Range", true)) c.setRequestProperty(k, v)
        c.setRequestProperty("Accept-Encoding", "identity")
        if (ultimo != FULL_BODY) c.setRequestProperty("Range", if (ultimo >= 0) "bytes=$ini-$ultimo" else "bytes=$ini-")
        validator?.let { c.setRequestProperty("If-Match", it) }
        return c
    }

    private fun conectar(u0: URL, ini: Long, ultimo: Long): HttpURLConnection {
        var u = u0
        repeat(10) {
            val t0 = android.os.SystemClock.elapsedRealtime()
            val c = conectar1(u, ini, ultimo)
            val code = try { c.responseCode } catch (e: IOException) {
                desconectar(c)
                throw e
            }
            if (fechado) { desconectar(c); throw cancelado() }
            stats.primeiroByteMs = (android.os.SystemClock.elapsedRealtime() - t0).toInt()
            if (code in 300..399 && code != 304) {
                val loc = c.getHeaderField("Location")
                desconectar(c)
                if (loc.isNullOrEmpty()) throw IOException("HTTP $code sem Location")
                u = URL(u, loc)
                return@repeat
            }
            stats.hostDe(c.url)
            return c
        }
        throw IOException("redirecionamentos demais")
    }

    // A abertura inicial depois de um seek antes nao tinha retry algum. Um
    // ConnectException escapava daqui e encerrava a fonte, embora os pedacos
    // posteriores aparentassem ter TENTATIVAS. Mantem o retry no Loader (nao no
    // fio do player) e curto o bastante para nao esconder uma rota morta.
    internal fun conectarComRetry(u: URL, ini: Long, ultimo: Long, atrasoPrimeiraMs: Long = -1L): HttpURLConnection {
        var erro: IOException? = null
        repeat(TENTATIVAS) { tentativa ->
            try { return conectar(u, ini, ultimo) }
            catch (e: IOException) {
                erro = e
                // SocketTimeoutException tambem herda InterruptedIOException:
                // so a nossa sentinela (ou close/interrupcao real) cancela.
                if (foiCancelado(e)) throw cancelado(e)
                if (tentativa + 1 < TENTATIVAS) {
                    Log.w(TAG, "conexao ${tentativa + 1}/$TENTATIVAS falhou em ${u.host}; tentando de novo")
                    esperarTentativa(tentativa, if (tentativa == 0) atrasoPrimeiraMs else -1L)
                }
            }
        }
        throw FalhaDownloadParalelo(erro ?: IOException("conexao falhou"))
    }

    // Abertura pelo destino ja conhecido do link do addon; recusa ou queda
    // volta ao link do addon com o retry normal, e o destino novo fica guardado.
    private fun abrirSonda(original: URL, ini: Long, ultimo: Long): HttpURLConnection {
        val chave = original.toString()
        val guardado = destinos?.buscar(chave)
        if (guardado != null) {
            try {
                val c = conectar(guardado, ini, ultimo)
                val esperado = destinos?.tamanhoDe(chave) ?: -1L
                val visto = totalDe(c)
                if (c.responseCode in 200..299 && (esperado <= 0 || visto == esperado)) {
                    Log.i(TAG, "destino guardado: ${guardado.host} direto, sem passar por ${original.host}")
                    return c
                }
                if (c.responseCode in 200..299)
                    Log.i(TAG, "destino guardado serviu outro arquivo ($visto bytes, esperado $esperado); de volta a ${original.host}")
                else Log.i(TAG, "destino guardado recusou (HTTP ${c.responseCode}); de volta a ${original.host}")
                desconectar(c)
            } catch (e: IOException) {
                if (foiCancelado(e)) throw cancelado(e)
                Log.i(TAG, "destino guardado falhou (${e.javaClass.simpleName}); de volta a ${original.host}")
            }
            destinos?.esquecer(chave)
        }
        val c = conectarComRetry(original, ini, ultimo, 0L)
        if (c.responseCode in 200..299) destinos?.lembrar(chave, c.url, totalDe(c))
        return c
    }

    // Tamanho do arquivo inteiro segundo a resposta: o total do Content-Range
    // num 206, o corpo num 200 (o servidor ignorou o Range). -1 se nao se sabe.
    private fun totalDe(c: HttpURLConnection): Long = when (c.responseCode) {
        206 -> (c.getHeaderField("Content-Range") ?: "").substringAfter('/', "").trim().toLongOrNull() ?: -1L
        200 -> c.contentLengthLong.takeIf { it > 0 } ?: -1L
        else -> -1L
    }

    override fun open(dataSpec: DataSpec): Long {
        // Conectar, esperar o primeiro byte e baixar o primeiro pedaco e
        // transferencia ativa (o tempo entra na cobertura do segundo).
        medidor?.inicio(token)
        stats.usado = true
        stats.conexoes.incrementAndGet()
        try { return abrir(dataSpec) }
        catch (e: IOException) {
            val cancelled = foiCancelado(e)
            close()
            if (cancelled) throw cancelado(e) else throw e
        } finally { stats.conexoes.decrementAndGet(); medidor?.fim(token) }
    }

    private fun abrir(dataSpec: DataSpec): Long {
        fechado = false
        validator = null
        total = -1
        pedacoAtual = pedaco
        serialRemaining = -1
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        Log.i(TAG, "abrindo ${dataSpec.uri.host} a partir de ${dataSpec.position}")
        pos = dataSpec.position
        fim = pos
        urlFinal = null
        val pedido = dataSpec.length
        if (pedido == 0L) {
            // Janela vazia (DataSpec de 0 byte): abre vazio, sem pedir um Range
            // invalido (ultimo < primeiro) e sem gastar conexao.
            aberto = true
            transferStarted(dataSpec)
            return 0L
        }
        // A sonda cabe no que foi pedido, mas nunca passa de PRIMEIRO: um
        // buraco do cache maior que a janela (seek para tras sobre regiao
        // sem cache) entraria inteiro num ByteArray so, na thread de quem
        // chamou, antes do pool existir.
        if (pos < 0 || (pedido > 0 && pedido > Long.MAX_VALUE - pos)) throw IOException("Invalid HTTP byte window")
        val probeLength = if (pedido >= 0) minOf(pedido, PRIMEIRO.toLong()) else PRIMEIRO.toLong()
        serialSpec = dataSpec
        if (serialOnly.get()) {
            val last = if (pedido > 0) pos + pedido - 1 else -1
            val c = abrirSonda(URL(dataSpec.uri.toString()), pos, last)
            return validatedSerial(c, dataSpec, last)
        }
        val primeiroUltimo = pos + minOf(probeLength - 1, Long.MAX_VALUE - pos)
        // Primeiro pedaco pela URL original: segue os redirecionamentos do debrid
        // uma vez so; os pedacos seguintes vao direto ao endereco final. Uma
        // abertura seguinte da mesma sessao ja comeca no destino (DestinosAddon).
        var c = abrirSonda(URL(dataSpec.uri.toString()), pos, primeiroUltimo)
        if (c.responseCode == 206 && rangeTotal(c, pos, primeiroUltimo) < 0) {
            serialOnly.set(true)
            val url = c.url
            desconectar(c)
            c = conectarComRetry(url, pos, FULL_BODY)
            if (c.responseCode == 206) throw IOException("Invalid HTTP full-body fallback")
        }
        val code = c.responseCode
        total = totalDe(c)
        validator = c.getHeaderField("ETag")?.takeIf { it.length > 2 && it.first() == '"' && it.last() == '"' }
        if (code == 200 || validator == null) serialOnly.set(true)
        val canSplit = code == 206 && validator != null && conexoes > 1
        // Janela que ESTA abertura pode servir: o tamanho que o ExoPlayer
        // pediu ou, na abertura progressiva comum (length UNSET), o resto do
        // arquivo — nunca o pedaco de sonda, senao nenhuma abertura comum
        // caberia no paralelo e tudo viraria conexao unica.
        val janela = when {
            pedido >= 0 && total >= 0 -> minOf(pedido, total - pos)
            pedido >= 0 -> pedido
            total > 0 -> total - pos
            else -> C.LENGTH_UNSET.toLong()
        }
        val paralelo = canSplit && total > 0 && janela > pedaco.toLong() * 2
        // FIM DO ARQUIVO: o indice do MKV (Cues) e o moov do MP4 ficam nos
        // ultimos MB, e o Media3 vai ate la ANTES do primeiro quadro. Pela
        // conexao unica, os 4 MB finais de um remux de 50 GB levaram 24 s no
        // Shield (Ant-Man, 16/10/2026: 170 KB/s naquele trecho do TorBox) e a
        // imagem esperou tudo; os 612 KB finais de outro remux, 5,3 s. Uma
        // janela que vai ate o fim e tem mais de dois pedacos minimos vai pelo
        // pool: a sonda fica com a sua parte e o resto se divide entre as
        // outras conexoes, todas ao mesmo tempo.
        val cauda = !paralelo && canSplit && total > 0 &&
            janela > PEDACO_CAUDA_MIN.toLong() * 2 && pos + janela >= total
        if (!paralelo && !cauda) {
            Log.i(TAG, "conexao unica: HTTP $code, total=$total, pos=$pos (${dataSpec.uri.host})")
            if (code == 206 && janela > minOf(probeLength, total - pos)) {
                val url = c.url
                desconectar(c)
                val last = pos + janela - 1
                c = conectarComRetry(url, pos, last)
                if (c.responseCode == 206 && rangeTotal(c, pos, last) != total) {
                    desconectar(c)
                    c = conectarComRetry(url, pos, FULL_BODY)
                    if (c.responseCode == 206) throw IOException("Invalid HTTP full-body fallback")
                }
            }
            return startSerial(c, dataSpec)
        }
        requireIdentityEncoding(c)
        response = c.headerFields.filterKeys { it != null }
        uri = Uri.parse(c.url.toString())
        urlFinal = c.url
        fim = if (pedido != C.LENGTH_UNSET.toLong()) minOf(total, pos + pedido) else total
        // No fim do arquivo a sonda le so a sua parte (o pedido dela ja foi ate
        // PRIMEIRO; o resto daquela resposta e descartado ao desconectar).
        val parte = ((fim - pos + conexoes - 1) / conexoes.coerceAtLeast(1))
            .coerceIn(PEDACO_CAUDA_MIN.toLong(), PRIMEIRO.toLong())
        val sondaFim = minOf(primeiroUltimo + 1, fim, if (cauda) pos + parte else Long.MAX_VALUE)
        if (cauda) {
            val outras = (conexoes - 1).coerceAtLeast(1)
            val resto = fim - sondaFim
            pedacoAtual = ((resto + outras - 1) / outras)
                .coerceIn(PEDACO_CAUDA_MIN.toLong(), pedaco.toLong()).toInt()
            proximoPedido = sondaFim
            startWorkers()
        }
        val primeiro = try { lerTudo(c, sondaFim - pos) } finally { desconectar(c) }
        if (cauda && primeiro.size.toLong() != sondaFim - pos)
            throw IOException("sonda curta: ${primeiro.size} de ${sondaFim - pos}")
        bufIni = pos
        buf = primeiro
        if (!cauda) {
            proximoPedido = pos + primeiro.size
            startWorkers()
        }
        aberto = true
        transferStarted(dataSpec)
        if (cauda) Log.i(TAG, "paralelo no fim do arquivo: ${fim - pos} bytes, pedaco ${pedacoAtual shr 10} KB a partir de $pos")
        else Log.i(TAG, "paralelo: $conexoes conexoes, pedaco ${pedaco shr 20} MB, ${(fim - pos) shr 20} MB a partir de $pos")
        return fim - pos
    }

    private fun requireIdentityEncoding(c: HttpURLConnection) {
        val encoding = c.getHeaderField("Content-Encoding")
        if (!encoding.isNullOrBlank() && !encoding.equals("identity", true)) throw IOException("Encoded HTTP range")
    }

    private fun validatedSerial(first: HttpURLConnection, spec: DataSpec, last: Long): Long {
        var c = first
        if (c.responseCode == 206 && rangeTotal(c, pos, last) < 0) {
            val url = c.url
            desconectar(c)
            c = conectarComRetry(url, pos, FULL_BODY)
            if (c.responseCode == 206) throw IOException("Invalid HTTP full-body fallback")
        }
        return startSerial(c, spec)
    }

    private fun startSerial(c: HttpURLConnection, spec: DataSpec): Long {
        unicaCon = c
        if (c.responseCode != 200 && c.responseCode != 206) {
            throw HttpDataSource.InvalidResponseCodeException(c.responseCode, c.responseMessage,
                null, c.headerFields, spec, ByteArray(0))
        }
        requireIdentityEncoding(c)
        val size = totalDe(c)
        if ((total > 0 && size > 0 && total != size) ||
            (validator != null && (c.getHeaderField("ETag") != validator ||
                (urlFinal != null && urlFinal.toString() != c.url.toString()))))
            throw IOException("HTTP representation changed")
        if (size >= 0 && pos > size) throw java.io.EOFException("Seek past HTTP body")
        serialRemaining = when {
            size >= 0 && spec.length >= 0 -> minOf(size - pos, spec.length)
            size >= 0 -> size - pos
            else -> spec.length
        }
        response = c.headerFields.filterKeys { it != null }
        uri = Uri.parse(c.url.toString())
        val input = c.inputStream
        unicaIn = input
        stats.conexoes.incrementAndGet()
        unicaOrigem = PassivoMedidor.origemDe(c.url)
        if (c.responseCode == 200 && pos > 0) {
            val scratch = ByteArray(32 * 1024)
            var remaining = pos
            while (remaining > 0) {
                conferirCancelamento()
                val n = input.read(scratch, 0, minOf(remaining, scratch.size.toLong()).toInt())
                if (n <= 0) throw java.io.EOFException("Short HTTP seek prefix")
                recordBytes(unicaOrigem, n)
                remaining -= n
            }
        }
        conferirCancelamento()
        if (!aberto) transferStarted(spec)
        aberto = true
        return serialRemaining
    }

    private fun recordBytes(origin: String?, count: Int) {
        medidor?.bytes(token, origin, count)
        stats.bytes.addAndGet(count.toLong())
    }

    // Never allocate from a server-supplied length. Every reader gets one
    // bounded array for its requested span, including short-read retries.
    private fun lerTudo(c: HttpURLConnection, limite: Long): ByteArray {
        if (limite !in 1L..maxOf(PRIMEIRO, pedaco).toLong()) throw IOException("Invalid HTTP chunk size")
        val out = ByteArray(limite.toInt())
        val origem = PassivoMedidor.origemDe(c.url)
        c.inputStream.use { input ->
            var used = 0
            while (used < out.size) {
                conferirCancelamento()
                val n = input.read(out, used, minOf(out.size - used, 64 * 1024))
                conferirCancelamento()
                if (n <= 0) throw java.io.EOFException("Short HTTP byte range")
                recordBytes(origem, n)
                used += n
            }
        }
        return out
    }

    // Mantem ate conexoes * ADIANTE pedacos pedidos a frente da leitura.
    @Synchronized private fun startWorkers() {
        conferirCancelamento()
        exec = Executors.newFixedThreadPool(conexoes)
        encher()
    }

    @Synchronized private fun encher() {
        conferirCancelamento()
        val ex = exec ?: return
        val u = urlFinal ?: return
        while (pedidos.size < conexoes && proximoPedido < fim) {
            val ini = proximoPedido
            val ult = ini + minOf(fim - ini, pedacoAtual.toLong()) - 1
            pedidos[ini] = ex.submit<ByteArray> { baixar(u, ini, ult) }
            proximoPedido = ult + 1
        }
    }

    private fun baixar(u: URL, ini: Long, ult: Long): ByteArray {
        var ultimoErro: IOException? = null
        repeat(TENTATIVAS) {
            conferirCancelamento()
            // Uma conexao paralela ativa, do connect ao ultimo byte. As outras
            // somam no mesmo segundo do medidor; nada e contado duas vezes.
            medidor?.inicio(token)
            stats.conexoes.incrementAndGet()
            var c: HttpURLConnection? = null
            try {
                c = conectar(u, ini, ult)
                if (c.responseCode == 200) throw UnreliableRange()
                if (c.responseCode != 206) {
                    val spec = serialSpec ?: throw IOException("HTTP ${c.responseCode} in range request")
                    throw HttpDataSource.InvalidResponseCodeException(c.responseCode, c.responseMessage,
                        null, c.headerFields, spec, ByteArray(0))
                }
                requireIdentityEncoding(c)
                if (rangeTotal(c, ini, ult) != total) throw UnreliableRange()
                if (c.getHeaderField("ETag") != validator ||
                    c.url.toExternalForm() != u.toExternalForm()) throw IOException("HTTP byte range changed")
                return lerTudo(c, ult - ini + 1)
            } catch (e: IOException) {
                ultimoErro = e
                if (foiCancelado(e)) throw cancelado(e)
                if (e is UnreliableRange || (e is HttpDataSource.InvalidResponseCodeException &&
                    e.responseCode in 400..499)) throw e
            } finally {
                desconectar(c)
                stats.conexoes.decrementAndGet()
                medidor?.fim(token)
            }
            if (it + 1 < TENTATIVAS) esperarTentativa(it)
        }
        throw FalhaDownloadParalelo(ultimoErro ?: IOException("pedaco $ini-$ult falhou"))
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        conferirCancelamento()
        unicaIn?.let { inp ->
            // Conexao unica: so o tempo DENTRO do read e transferencia (um
            // stall bloqueia aqui); o intervalo entre reads e o ExoPlayer
            // parado com o buffer cheio, e nao conta.
            if (serialRemaining == 0L) return C.RESULT_END_OF_INPUT
            medidor?.inicio(token)
            val want = if (serialRemaining < 0) length else minOf(length.toLong(), serialRemaining).toInt()
            val n = try { inp.read(buffer, offset, want).also { conferirCancelamento() } }
                catch (e: IOException) { if (foiCancelado(e)) throw cancelado(e) else throw e }
                finally { medidor?.fim(token) }
            if (n > 0) {
                recordBytes(unicaOrigem, n)
                if (serialRemaining >= 0) serialRemaining -= n
            } else if (serialRemaining > 0) throw java.io.EOFException("Short serial HTTP body")
            if (n < 0) return C.RESULT_END_OF_INPUT
            bytesTransferred(n)
            return n
        }
        if (pos >= fim) return C.RESULT_END_OF_INPUT
        var b = buf
        if (b == null || pos >= bufIni + b.size) {
            val f = pedidos.remove(pos) ?: run { encher(); pedidos.remove(pos) }
                ?: throw IOException("pedaco $pos nao foi pedido")
            b = try { f.get(60, TimeUnit.SECONDS) }
                catch (e: java.util.concurrent.ExecutionException) {
                    if (e.cause !is UnreliableRange) throw (e.cause as? IOException) ?: IOException(e.cause)
                    conferirCancelamento()
                    serialOnly.set(true)
                    stopWorkers()
                    val url = urlFinal ?: throw IOException("Missing HTTP source")
                    val spec = serialSpec ?: throw IOException("Missing HTTP byte window")
                    val remaining = spec.subrange(pos - spec.position, fim - pos)
                    validatedSerial(conectarComRetry(url, pos, fim - 1), remaining, fim - 1)
                    return read(buffer, offset, length)
                }
                catch (e: java.util.concurrent.TimeoutException) { throw IOException("pedaco $pos demorou mais de 60 s") }
                catch (e: InterruptedException) { Thread.currentThread().interrupt(); throw cancelado(e) }
                catch (e: java.util.concurrent.CancellationException) { throw cancelado(e) }
            conferirCancelamento()
            buf = b; bufIni = pos
            encher()
        }
        val off = (pos - bufIni).toInt()
        val n = minOf(length, b!!.size - off)
        System.arraycopy(b, off, buffer, offset, n)
        pos += n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri
    override fun getResponseHeaders(): Map<String, List<String>> = response

    @Synchronized private fun stopWorkers() {
        exec?.shutdownNow()
        exec = null
        for (f in pedidos.values) f.cancel(true)
        pedidos.clear()
        // Interrompe connect/read antigos imediatamente ao buscar outra posicao.
        for (c in ativas.toList()) desconectar(c)
        buf = null
    }

    @Synchronized override fun close() {
        fechado = true
        stopWorkers()
        if (unicaIn != null) stats.conexoes.decrementAndGet()
        try { unicaIn?.close() } catch (_: IOException) {}
        unicaIn = null
        unicaOrigem = null
        desconectar(unicaCon)
        unicaCon = null
        if (aberto) { aberto = false; transferEnded() }
        uri = null
        response = emptyMap()
    }
}
