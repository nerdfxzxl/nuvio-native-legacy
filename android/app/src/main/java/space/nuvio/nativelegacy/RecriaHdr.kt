package space.nuvio.nativelegacy

// QUEM NAO RECRIA A SUPERFICIE NO HDR. A recriacao do primeiro quadro HDR (e a
// segunda, 1,5 s depois) existe por causa do painel da TCL (MediaTek), que so
// liga o modo HDR quando a Surface nasce com o decoder ja em HDR. Em outras
// familias ela so custa:
// - MStar (OMX.MS.*, caixas Shinon): o hwcomposer refaz o overlay a cada
//   GONE/VISIBLE e a tela pisca/trava.
// - Amlogic (OMX.amlogic.*, c2.amlogic.*: Mecool, caixas de 2 GB): o HDR e da
//   saida HDMI, nao do painel. Na KM7 SE (Reddit, 2.0.3) as duas sessoes HDR10
//   recriaram a superficie duas vezes no inicio e cairam dentro da ART (GC e
//   AudioTrack.getTimestamp no fio do ExoPlayer) com memoria critica; a sessao
//   SDR, sem recriacao, nao caiu (registros D1 66008, 66017, 65445).
// Devolve o nome da familia dispensada, ou null para recriar.
object RecriaHdr {
    // Main-looper transaction: a background pause cancels capture-only
    // work or changes an already sent detach into a same-surface reattach.
    // Completion/expiry is one-shot; late renderer acknowledgments cannot
    // execute a second transition.
    enum class DetachAction { IGNORE, RETAIN, RECREATE }
    class DetachTicket {
        private enum class Phase { CAPTURE, SENT, FINISHED }
        private var phase = Phase.CAPTURE
        private var retain = false
        fun pause() { retain = true }
        fun send(): Boolean {
            if (phase != Phase.CAPTURE || retain) return false
            phase = Phase.SENT
            return true
        }
        fun acknowledge(): DetachAction {
            if (phase != Phase.SENT) return DetachAction.IGNORE
            phase = Phase.FINISHED
            return if (retain) DetachAction.RETAIN else DetachAction.RECREATE
        }
        fun expire(): Boolean {
            if (phase != Phase.SENT) return false
            phase = Phase.FINISHED
            return true
        }
        fun cancel() { phase = Phase.FINISHED }
    }

    // Media3 ExoTimeoutException.OPERATION_DETACH_SURFACE = 3.
    fun timeoutRecuperavel(codigo: Int, recriando: Boolean, operacao: Int): Boolean =
        codigo == 1003 && recriando && operacao == 3

    fun ownsTransition(session: Int, activeSession: Int, request: Int, activeRequest: Int,
                       samePlayer: Boolean, sameSurface: Boolean): Boolean =
        session == activeSession && request == activeRequest && samePlayer && sameSurface

    fun ownsRenderedFrame(sameOutput: Boolean, surfaceValid: Boolean,
                          renderTimeMs: Long, invalidatedAtMs: Long): Boolean =
        sameOutput && surfaceValid && renderTimeMs >= invalidatedAtMs

    fun mayRecoverDetach(alreadyRetried: Boolean, hasSurface: Boolean): Boolean =
        !alreadyRetried && hasSurface

    // Only the observed TCL/MediaTek panel behavior justifies proactive
    // destruction. Unknown devices retain their hardware plane; actual
    // detach failures still use the independent bounded recovery path.
    fun needsTclWorkaround(manufacturer: String, brand: String, decoders: List<String>): Boolean {
        val tcl = listOf(manufacturer, brand).any {
            it.trim().equals("TCL", ignoreCase = true)
        }
        if (!tcl || dispensa(decoders) != null) return false
        return decoders.any {
            it.startsWith("c2.mtk.", ignoreCase = true) ||
                it.startsWith("OMX.MTK.", ignoreCase = true)
        }
    }

    fun dispensa(decoders: List<String>): String? {
        if (decoders.any { it.startsWith("OMX.MS.") }) return "MStar"
        if (decoders.any { it.startsWith("OMX.amlogic.") || it.startsWith("c2.amlogic.") }) return "Amlogic"
        return null
    }
}
