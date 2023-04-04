package no.nav.personbruker.dittnav.varselbestiller.varsel

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator.createDoknotifikasjonFromVarsel
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class VarselSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonProducer: DoknotifikasjonProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val rapidMetricsProbe: RapidMetricsProbe
) :
    River.PacketListener {
    private val log = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "aktivert") }
            validate { it.demandAny("varselType", listOf("beskjed", "oppgave", "innboks")) }
            validate { it.demandValue("eksternVarsling", true) }
            validate {
                it.requireKey(
                    "namespace",
                    "appnavn",
                    "eventId",
                    "fodselsnummer",
                    "sikkerhetsnivaa",
                    "eksternVarsling"
                )
            }
            validate {
                it.interestedIn(
                    "prefererteKanaler",
                    "smsVarslingstekst",
                    "epostVarslingstekst",
                    "epostVarslingstittel"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val varsel = Varsel(
            varselType = VarselType.valueOf(packet["varselType"].textValue().uppercase()),
            namespace = packet["namespace"].textValue(),
            appnavn = packet["appnavn"].textValue(),
            eventId = packet["eventId"].textValue(),
            fodselsnummer = packet["fodselsnummer"].textValue(),
            sikkerhetsnivaa = packet["sikkerhetsnivaa"].intValue(),
            eksternVarsling = packet["eksternVarsling"].booleanValue(),
            prefererteKanaler = packet["prefererteKanaler"].map { it.textValue() },
            smsVarslingstekst = packet["smsVarslingstekst"].textValue(),
            epostVarslingstekst = packet["epostVarslingstekst"].textValue(),
            epostVarslingstittel = packet["epostVarslingstittel"].textValue()
        )

        runBlocking {
            val isDuplicateVarselbestilling =
                varselbestillingRepository.getVarselbestillingIfExists(varsel.eventId) != null

            if (!isDuplicateVarselbestilling) {
                doknotifikasjonProducer.sendAndPersistBestilling(
                    varselbestilling = varsel.toVarselBestilling(),
                    doknotifikasjon = createDoknotifikasjonFromVarsel(varsel)
                )
                rapidMetricsProbe.countDoknotifikasjonProduced(varsel.varselType, varsel.prefererteKanaler)
            } else {
                rapidMetricsProbe.countDuplicates(varsel.varselType)
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.debug(problems.toString())
    }
}

