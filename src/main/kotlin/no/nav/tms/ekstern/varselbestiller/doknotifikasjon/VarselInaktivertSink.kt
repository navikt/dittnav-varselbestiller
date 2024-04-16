package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.ekstern.varselbestiller.config.MetricsCollector
import no.nav.tms.common.observability.traceVarsel

class InaktivertSink(
    rapidsConnection: RapidsConnection,
    private val stoppDoknotEventProducer: DoknotEventProducer<DoknotifikasjonStopp>
) :
    River.PacketListener {
    private val log = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "inaktivert")
                it.requireKey("varselId", "produsent")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val varselId = packet["varselId"].textValue()
        val appnavn = packet["produsent"]["appnavn"].textValue()
        val eventName = packet["@event_name"].textValue()

        traceVarsel(
            id = varselId,
            extra = mapOf("action" to "inaktivert", "initiated_by" to packet["produsent"]["namespace"].asText())
        ) {
            log.info { "Inaktivert-event motatt" }

            stoppDoknotEventProducer.sendEvent(varselId, createDoknotifikasjonStopp(varselId, appnavn))
            MetricsCollector.eksternVarslingStoppet(eventName)
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.debug { problems.toString() }
    }
}

fun createDoknotifikasjonStopp(varselId: String, appnavn: String): DoknotifikasjonStopp {
    val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
        .setBestillingsId(varselId)
        .setBestillerId(appnavn)
    return doknotifikasjonStoppBuilder.build()
}
