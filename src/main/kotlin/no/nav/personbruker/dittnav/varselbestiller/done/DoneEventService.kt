package no.nav.personbruker.dittnav.varselbestiller.done

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<NokkelIntern, DoneIntern> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelIntern, DoneIntern>) {
        val doknotifikasjonStopp = mutableMapOf<String, DoknotifikasjonStopp>()
        val problematicEvents = mutableMapOf<NokkelIntern, DoneIntern>()
        metricsCollector.recordMetrics(eventType = Eventtype.DONE_INTERN) {
            val doneEvents = getDoneEventsMap(this, events)
            if(doneEvents.isNotEmpty()) {
                val varselbestillingerForEventIds = varselbestillingRepository.fetchVarselbestillingerForEventIds(doneEvents.keys.map { it.getEventId() })
                if (varselbestillingerForEventIds.isNotEmpty()) {
                    doneEvents.forEach { (nokkel, event) ->
                        try {
                            val varselbestilling = varselbestillingerForEventIds.firstOrNull{ it.eventId == nokkel.getEventId() && it.appnavn == nokkel.getAppnavn() && it.fodselsnummer == nokkel.getFodselsnummer() }
                            if(varselbestilling != null) {
                                if(varselbestilling.avbestilt) {
                                    log.info("Varsel med bestillingsid ${varselbestilling.bestillingsId} allerede avbestilt, avbestiller ikke på nytt.")
                                    countDuplicateVarselbestillingForProducer(varselbestilling.namespace, varselbestilling.appnavn)
                                } else {
                                    doknotifikasjonStopp[varselbestilling.bestillingsId] = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
                                    countSuccessfulEksternVarslingForProducer(varselbestilling.namespace, varselbestilling.appnavn)
                                }
                            }
                        } catch (e: Exception) {
                            countFailedEksternvarslingForProducer(nokkel.getNamespace(), nokkel.getAppnavn())
                            problematicEvents[nokkel] = event
                            log.warn("Eventet kan ikke brukes pga en ukjent feil, done-eventet vil bli forkastet. EventId: ${nokkel.getEventId()}", e)
                        }
                    }
                    if (doknotifikasjonStopp.isNotEmpty()) {
                        produceDoknotifikasjonStoppAndPersistToDB(doknotifikasjonStopp)
                    }
                    if(problematicEvents.isNotEmpty()) {
                        throwExceptionIfFailedValidation(problematicEvents)
                    }
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
                eventMetricsSession.countAllEventsFromKafkaForProducer(event.namespace, event.appnavn)
                doneEvents[doneKey] = doneEvent
            }  catch (e: Exception) {
                eventMetricsSession.countFailedEksternvarslingForProducer(event.namespace, event.appnavn)
                log.warn("Fikk en uventet feil ved prosessering av Done-event, fullfører batch-en.", e)
            }
        }
        return doneEvents
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
