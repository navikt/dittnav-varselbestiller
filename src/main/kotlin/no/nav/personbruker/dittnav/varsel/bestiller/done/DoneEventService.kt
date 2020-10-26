package no.nav.personbruker.dittnav.varsel.bestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType.DONE
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
        private val eventMetricsProbe: EventMetricsProbe
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Done>>()

        eventMetricsProbe.runWithMetrics(eventType = DONE) {
            events.forEach { event ->
                try {
                    if(harBestiltEksternVarsling(event.value())) {
                        val doknotifikasjonStoppKey = event.eventId
                        val doknotifikasjonStoppEvent = DoknotifikasjonTransformer.createDoknotifikasjonStopp(event.getNonNullKey())
                        successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonStoppKey, doknotifikasjonStoppEvent))
                        countSuccessfulEventForProducer(event.systembruker)
                    }
                } catch (e: NokkelNullException) {
                    countFailedEventForProducer("NoProducerSpecified")
                    log.warn("Done-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
                } catch (e: FieldValidationException) {
                    countFailedEventForProducer(event.systembruker)
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, done-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.systembruker)
                    problematicEvents.add(event)
                    log.warn("Validering av done-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            doknotifikasjonStoppProducer.produceDoknotifikasjonStop(successfullyValidatedEvents)
        }
        kastExceptionHvisMislykkedValidering(problematicEvents)
    }

    private fun harBestiltEksternVarsling(value: Done): Boolean {
        return false
        TODO("Skal legge til sjekk på om brukernotifikasjonen tilhørende Done-eventet faktisk har bestilt eksternt varsel")
    }

    private fun kastExceptionHvisMislykkedValidering(problematicEvents: MutableList<ConsumerRecord<Nokkel, Done>>) {
        if (problematicEvents.isNotEmpty()) {
            val message = "En eller flere done-eventer kunne ikke sendes til varsel-bestiller fordi validering feilet."
            val exception = UnvalidatableRecordException(message)
            exception.addContext("antallMislykkedValidering", problematicEvents.size)
            throw exception
        }
    }
}
