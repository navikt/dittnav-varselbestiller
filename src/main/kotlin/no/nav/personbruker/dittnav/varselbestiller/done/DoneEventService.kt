package no.nav.personbruker.dittnav.varselbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.builders.exception.UnknownEventtypeException
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: Producer<String, DoknotifikasjonStopp>,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val doknotifikasjonStopp = mutableMapOf<String, DoknotifikasjonStopp>()
        val problematicEvents = mutableMapOf<Nokkel, Done>()
        metricsCollector.recordMetrics(eventType = Eventtype.DONE) {
            val doneEvents = getDoneEventsMap(this, events)
            val varselbestillingerForEventIds = varselbestillingRepository.fetchVarselbestillingerForEventIds(doneEvents.keys.map { it.getEventId() })
            if (varselbestillingerForEventIds.isNotEmpty()) {
                doneEvents.forEach { (nokkel, event) ->
                    try {
                        val varselbestilling = varselbestillingerForEventIds.firstOrNull{ it.eventId == nokkel.getEventId() && it.systembruker == nokkel.getSystembruker() && it.fodselsnummer == event.getFodselsnummer() }
                        if(varselbestilling != null) {
                            if(varselbestilling.avbestilt) {
                                log.info("Varsel med bestillingsid ${varselbestilling.bestillingsId} allerede avbestilt, avbestiller ikke på nytt.")
                                countDuplicateVarselbestillingForSystemUser(varselbestilling.systembruker)
                            } else {
                                doknotifikasjonStopp[varselbestilling.bestillingsId] = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
                                countSuccessfulEksternvarslingForSystemUser(varselbestilling.systembruker)
                            }
                        }
                    } catch (e: FieldValidationException) {
                        countFailedEksternvarslingForSystemUser(nokkel.getSystembruker() ?: "NoProducerSpecified")
                        log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, done-eventet vil bli forkastet. EventId: ${nokkel.getEventId()}", e)
                    } catch (e: UnknownEventtypeException) {
                        countFailedEksternvarslingForSystemUser(nokkel.getSystembruker() ?: "NoProducerSpecified")
                        log.warn("Eventet kan ikke brukes fordi det inneholder ukjent eventtype, done-eventet vil bli forkastet. EventId: ${nokkel.getEventId()}", e)
                    } catch (e: Exception) {
                        countFailedEksternvarslingForSystemUser(nokkel.getSystembruker() ?: "NoProducerSpecified")
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

    private fun getDoneEventsMap(eventMetricsSession: EventMetricsSession, events: ConsumerRecords<Nokkel, Done>): Map<Nokkel, Done> {
        val doneEvents = mutableMapOf<Nokkel, Done>()
        events.forEach { event ->
            try {
                val doneKey = event.getNonNullKey()
                val doneEvent = event.value()
                eventMetricsSession.countAllEventsFromKafkaForSystemUser(doneKey.getSystembruker())
                doneEvents[doneKey] = doneEvent
            } catch (e: NokkelNullException) {
                eventMetricsSession.countNokkelWasNull()
                log.warn("Done-eventet manglet nøkkel, blir forkastet. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
            } catch (e: Exception) {
                eventMetricsSession.countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                log.warn("Fikk en uventet feil ved prosessering av Done-event, fullfører batch-en.", e)
            }
        }
        return doneEvents
    }

    private suspend fun produceDoknotifikasjonStoppAndPersistToDB(successfullyValidatedEvents: Map<String, DoknotifikasjonStopp>) {
        val events = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
        doknotifikasjonStoppProducer.produceEvents(events)
        varselbestillingRepository.cancelVarselbestilling(successfullyValidatedEvents.keys.toList())
    }

    private fun throwExceptionIfFailedValidation(problematicEvents: MutableMap<Nokkel, Done>) {
        val message = "En eller flere done-eventer kunne ikke sendes til varselbestiller."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }
}
