package no.nav.personbruker.dittnav.varselbestiller.varsel

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class DoneSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
    private val includeVarselInaktivert: Boolean = false
) :
    River.PacketListener {
    private val log = KotlinLogging.logger {  }

    init {
        River(rapidsConnection).apply {
            if (includeVarselInaktivert) {
                validate { it.demandAny("@event_name", listOf("inaktivert")) }
            } else {
                validate { it.demandValue("@event_name", "done") }
            }
            validate { it.requireKey("eventId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].textValue()
        val eventName = packet["@event_name"].textValue()

        runBlocking {

            varselbestillingRepository.getVarselbestillingIfExists(eventId)?.let { existingVarselbestilling ->
                if (!existingVarselbestilling.avbestilt) {
                    doknotifikasjonStoppProducer.sendDoknotifikasjonStoppAndPersistCancellation(
                        createDoknotifikasjonStopp(existingVarselbestilling)
                    )
                    rapidMetricsProbe.countDoknotifikasjonStoppProduced(eventName)
                }
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

