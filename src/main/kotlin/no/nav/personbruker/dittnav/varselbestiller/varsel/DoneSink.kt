package no.nav.personbruker.dittnav.varselbestiller.varsel

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val rapidMetricsProbe: RapidMetricsProbe,
    private val writeToDb: Boolean
) :
    River.PacketListener {
    private val log: Logger = LoggerFactory.getLogger(DoneSink::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "done") }
            validate { it.requireKey("eventId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].textValue()

        runBlocking {
            val varselbestillinger = varselbestillingRepository.fetchVarselbestillingerForEventIds(listOf(eventId))

            if (varselbestillinger.isNotEmpty()) {
                val doknotifikasjonStopp = createDoknotifikasjonStopp(varselbestillinger.first())

                if (writeToDb) {
                    doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(listOf(doknotifikasjonStopp))
                } else {
                    log.info("Dryrun: done fra rapid med eventid $eventId")
                }
                log.info("Behandlet done fra rapid med eventid $eventId")
            }

            rapidMetricsProbe.countProcessed()
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
    }
}

