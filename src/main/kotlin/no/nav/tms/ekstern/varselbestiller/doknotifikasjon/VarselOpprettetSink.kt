package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotifikasjonCreator.createDoknotifikasjonFromVarsel
import no.nav.tms.ekstern.varselbestiller.config.MetricsCollector
import observability.traceVarsel

class VarselOpprettetSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonProducer: DoknotEventProducer<Doknotifikasjon>
) : River.PacketListener {
    private val log = KotlinLogging.logger { }

    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "opprettet") }
            validate { it.demandAny("type", listOf("beskjed", "oppgave", "innboks")) }
            validate {
                it.requireKey(
                    "eksternVarslingBestilling",
                    "produsent",
                    "varselId",
                    "ident",
                    "sensitivitet"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val varsel: Varsel = objectMapper.readValue(packet.toJson())

        traceVarsel(varsel.varselId, mapOf("action" to "opprettet", "initiated_by" to varsel.produsent.namespace)) {
            log.info { "Opprettet-event motatt" }
            doknotifikasjonProducer.sendEvent(varsel.varselId, createDoknotifikasjonFromVarsel(varsel))
            MetricsCollector.eksternVarslingBestilt(varsel.type, varsel.eksternVarslingBestilling.prefererteKanaler)
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.debug { problems.toString() }
    }

}

