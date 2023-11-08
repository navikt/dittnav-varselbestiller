package no.nav.personbruker.dittnav.varselbestiller.varsel

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.personbruker.dittnav.varselbestiller.common.eventMdc
import no.nav.personbruker.dittnav.varselbestiller.common.typeMDC
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator.createDoknotifikasjonFromVarsel
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import observability.traceVarsel

class VarselSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonProducer: DoknotifikasjonProducer,
    private val varselbestillingRepository: VarselbestillingRepository
) : River.PacketListener {
    private val log = KotlinLogging.logger { }

    private val objectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "aktivert") }
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

        traceVarsel(varsel.varselId, mapOf("aktivert".eventMdc, varsel.type.typeMDC)) {
            runBlocking {
                val isDuplicateVarselbestilling =
                    varselbestillingRepository.getVarselbestillingIfExists(varsel.varselId) != null

                if (!isDuplicateVarselbestilling) {
                    doknotifikasjonProducer.sendAndPersistBestilling(
                        varselbestilling = varsel.toVarselBestilling(),

                        doknotifikasjon = createDoknotifikasjonFromVarsel(varsel)
                    )
                    RapidMetrics.eksternVarslingBestilt(varsel.type, varsel.eksternVarslingBestilling.prefererteKanaler)
                } else {
                    RapidMetrics.duplikat(varsel.type)
                }
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.debug { problems.toString() }
    }

}

