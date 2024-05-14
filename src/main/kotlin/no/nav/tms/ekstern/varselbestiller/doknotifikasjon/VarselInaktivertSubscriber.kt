package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.tms.ekstern.varselbestiller.config.MetricsCollector
import no.nav.tms.common.observability.traceVarsel
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription

class VarselInaktivertSink(
    private val stoppDoknotEventProducer: DoknotEventProducer<DoknotifikasjonStopp>
) : Subscriber() {
    private val log = KotlinLogging.logger { }
    override suspend fun receive(jsonMessage: JsonMessage) {
        val varselId = jsonMessage["varselId"].textValue()
        val appnavn = jsonMessage["produsent"]["appnavn"].textValue()
        val eventName = jsonMessage.eventName

        traceVarsel(
            id = varselId,
            extra = mapOf("action" to "inaktivert", "initiated_by" to jsonMessage["produsent"]["namespace"].asText())
        ) {
            log.info { "Inaktivert-event motatt" }

            stoppDoknotEventProducer.sendEvent(varselId, createDoknotifikasjonStopp(varselId, appnavn))
            MetricsCollector.eksternVarslingStoppet(eventName)
        }
    }

    override fun subscribe(): Subscription = Subscription
        .forEvent("inaktivert")
        .withFields("varselId", "produsent")

}

fun createDoknotifikasjonStopp(varselId: String, appnavn: String): DoknotifikasjonStopp {
    val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
        .setBestillingsId(varselId)
        .setBestillerId(appnavn)
    return doknotifikasjonStoppBuilder.build()
}
