package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.dittnav.varsel.bestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType.OPPGAVE
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory

class OppgaveEventService(
        private val kafkaProducer: KafkaProducerWrapper<Oppgave>,
        private val metricsProbe: EventMetricsProbe
) : EventBatchProcessorService<Oppgave> {

    private val log = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<Oppgave>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Oppgave>>()

        metricsProbe.runWithMetrics(eventType = OPPGAVE) {
            events.forEach { event ->
                try {
                    OppgaveValidation.validateEvent(event.getNonNullKey(), event.value())
                    successfullyValidatedEvents.add(RecordKeyValueWrapper(event.getNonNullKey(), event.value()))
                    countSuccessfulEventForProducer(event.systembruker)

                } catch (e: NokkelNullException) {
                    countFailedEventForProducer("NoProducerSpecified")
                    log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)

                } catch (e: FieldValidationException) {
                    countFailedEventForProducer(event.systembruker)
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, oppgave-eventet vil bli forkastet. EventId: ${event.getNonNullKey().getEventId()}", e)

                } catch (e: Exception) {
                    countFailedEventForProducer(event.systembruker)
                    problematicEvents.add(event)
                    log.warn("Validering av oppgave-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }

            kafkaProducer.sendEvents(successfullyValidatedEvents)
        }

        kastExceptionHvisMislykkedValidering(problematicEvents)
    }

    private fun kastExceptionHvisMislykkedValidering(problematicEvents: MutableList<ConsumerRecord<Nokkel, Oppgave>>) {
        if (problematicEvents.isNotEmpty()) {
            val message = "En eller flere oppgave-eventer kunne ikke sendes til varsel-bestiller fordi validering feilet."
            val exception = UnvalidatableRecordException(message)
            exception.addContext("antallMislykkedValidering", problematicEvents.size)
            throw exception
        }
    }

}
