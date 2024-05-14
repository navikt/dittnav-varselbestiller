package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.tms.common.observability.traceVarsel
import no.nav.tms.ekstern.varselbestiller.config.MetricsCollector
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotifikasjonCreator.createDoknotifikasjonFromVarsel
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription

class VarselOpprettetSubscriber(
    private val doknotifikasjonProducer: DoknotEventProducer<Doknotifikasjon>
) : Subscriber() {
    private val log = KotlinLogging.logger { }
    override fun subscribe(): Subscription = Subscription
        .forEvent("opprettet")
        .withFields(
            "eksternVarslingBestilling",
            "produsent",
            "varselId",
            "ident",
            "sensitivitet"
        )
        .withAnyValue("type", "beskjed", "oppgave", "innboks")

    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
    override suspend fun receive(jsonMessage: JsonMessage) {

                val varsel: Varsel = objectMapper.treeToValue(jsonMessage.json)

                traceVarsel(varsel.varselId, mapOf("action" to "opprettet", "initiated_by" to varsel.produsent.namespace)) {
                    log.info { "Opprettet-event motatt" }
                    doknotifikasjonProducer.sendEvent(varsel.varselId, createDoknotifikasjonFromVarsel(varsel))
                    MetricsCollector.eksternVarslingBestilt(varsel.type, varsel.eksternVarslingBestilling.prefererteKanaler)
                }
    }
}

