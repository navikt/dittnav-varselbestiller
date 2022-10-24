package no.nav.personbruker.dittnav.varselbestiller.varsel

import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.slf4j.LoggerFactory

class VarselInaktivertSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(VarselInaktivertSink::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "varselInaktivert") }
            validate { it.requireKey("eventId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()!!
        log.info("Mottok varselInaktivert event for eventId $eventId")
        runBlocking {
            varselbestillingRepository.varselbestillingByEventId(eventId).also {
                log.info("Kanselerer bestillig av eksterne varsel for eventId $eventId")
                if (it != null && !it.avbestilt) {
                    doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(it.toDoktifikasjonList())
                } else {
                    log.warn("Fant ikke bestilling for eventId $eventId")
                }

            }
        }
    }
}

private fun Varselbestilling.toDoktifikasjonList(): List<DoknotifikasjonStopp> = listOf(
    DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(this)
)
