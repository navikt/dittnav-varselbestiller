package no.nav.personbruker.dittnav.varselbestiller.done

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEvent
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEventRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.Producer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
    private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val earlyDoneEventRepository: EarlyDoneEventRepository,
    private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<NokkelIntern, DoneIntern> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelIntern, DoneIntern>) {
        val doknotifikasjonStopp = mutableMapOf<String, DoknotifikasjonStopp>()
        val problematicEvents = mutableMapOf<NokkelIntern, DoneIntern>()
        val unmatchedEvents = mutableMapOf<NokkelIntern, DoneIntern>()
        metricsCollector.recordMetrics(eventType = Eventtype.DONE_INTERN) {
            val doneEvents = getDoneEventsMap(this, events)
            if(doneEvents.isNotEmpty()) {
                val varselbestillingerForEventIds = varselbestillingRepository.fetchVarselbestillingerForEventIds(doneEvents.keys.map { it.getEventId() })

                // todo: q1: skal hele batch re-proseseres om det er en problematic event? double-cancellation
                doneEvents.forEach { (nokkel, event) ->
                    try {
                        // TODO: verifisere at vi trenger alle disse felter for å finne match
                        val varselbestilling = varselbestillingerForEventIds.firstOrNull{ it.eventId == nokkel.getEventId() && it.appnavn == nokkel.getAppnavn() && it.fodselsnummer == nokkel.getFodselsnummer() }
                        if(varselbestilling != null) {
                            if(varselbestilling.avbestilt) {
                                log.info("Varsel med bestillingsid ${varselbestilling.bestillingsId} allerede avbestilt, avbestiller ikke på nytt.")
                                countDuplicateVarselbestillingForProducer(Producer(varselbestilling.namespace, varselbestilling.appnavn))
                            } else {
                                doknotifikasjonStopp[varselbestilling.bestillingsId] = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
                                countSuccessfulEksternVarslingForProducer(Producer(varselbestilling.namespace, varselbestilling.appnavn))
                            }
                        } else {
                            unmatchedEvents[nokkel] = event
                            log.info("Ingen varsel fant for done-eventet. EventId: ${nokkel.getEventId()}")
                        }
                    } catch (e: Exception) {
                        countFailedEksternVarslingForProducer(Producer(nokkel.getNamespace(), nokkel.getAppnavn()))
                        problematicEvents[nokkel] = event
                        log.warn("Eventet kan ikke brukes pga en ukjent feil, done-eventet vil bli forkastet. EventId: ${nokkel.getEventId()}", e)
                    }
                }
                if (doknotifikasjonStopp.isNotEmpty()) {
                    produceDoknotifikasjonStoppAndPersistToDB(doknotifikasjonStopp)
                }
                if (unmatchedEvents.isNotEmpty()) {
                    persistEarlyCancellationsToDB(unmatchedEvents)
                }
                if(problematicEvents.isNotEmpty()) {
                    throwExceptionIfFailedValidation(problematicEvents)
                }
            }
        }
    }

    private fun getDoneEventsMap(eventMetricsSession: EventMetricsSession, events: ConsumerRecords<NokkelIntern, DoneIntern>): Map<NokkelIntern, DoneIntern> {
        val doneEvents = mutableMapOf<NokkelIntern, DoneIntern>()
        events.forEach { event ->
            try {
                val doneKey = event.key()
                val doneEvent = event.value()
                eventMetricsSession.countAllEventsFromKafkaForProducer(Producer(event.namespace, event.appnavn))
                doneEvents[doneKey] = doneEvent
            }  catch (e: Exception) {
                eventMetricsSession.countFailedEksternVarslingForProducer(Producer(event.namespace, event.appnavn))
                log.warn("Fikk en uventet feil ved prosessering av Done-event, fullfører batch-en.", e)
            }
        }
        return doneEvents
    }

    private suspend fun persistEarlyCancellationsToDB(unmatchedEvents: MutableMap<NokkelIntern, DoneIntern>) {
        val earlyDoneEvents = unmatchedEvents.map { EarlyDoneEvent.fromEventEntryMap(it) }

        earlyDoneEventRepository.persistInBatch(earlyDoneEvents)
    }

    private suspend fun produceDoknotifikasjonStoppAndPersistToDB(successfullyValidatedEvents: Map<String, DoknotifikasjonStopp>) {
        doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(successfullyValidatedEvents)
    }

    private fun throwExceptionIfFailedValidation(problematicEvents: MutableMap<NokkelIntern, DoneIntern>) {
        val message = "En eller flere done-eventer kunne ikke sendes til varselbestiller."
        val exception = UntransformableRecordException(message)
        exception.addContext("antallMislykkedeTransformasjoner", problematicEvents.size)
        throw exception
    }
}
