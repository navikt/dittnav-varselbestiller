package no.nav.personbruker.dittnav.varsel.bestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.varsel.bestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.BrukernotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType.DONE
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val producer: BrukernotifikasjonProducer<no.nav.personbruker.dittnav.varsel.bestiller.done.Done>,
        private val eventMetricsProbe: EventMetricsProbe
) : EventBatchProcessorService<Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyTransformedEvents = mutableListOf<no.nav.personbruker.dittnav.varsel.bestiller.done.Done>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Done>>()

        eventMetricsProbe.runWithMetrics(eventType = DONE) {

            events.forEach { event ->
                try {
                    val internalEvent = DoneTransformer.toInternal(event.getNonNullKey(), event.value())
                    successfullyTransformedEvents.add(internalEvent)
                    countSuccessfulEventForProducer(event.systembruker)

                } catch (e: NokkelNullException) {
                    countFailedEventForProducer("NoProducerSpecified")
                    log.warn("Eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)

                } catch (e: FieldValidationException) {
                    countFailedEventForProducer(event.systembruker)
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, eventet vil bli forkastet. EventId: ${event.getNonNullKey().getEventId()}", e)

                } catch (e: Exception) {
                    countFailedEventForProducer(event.systembruker)
                    problematicEvents.add(event)
                    log.warn("Transformasjon av done-event fra Kafka feilet.", e)
                }
            }

            producer.sendToKafka(successfullyTransformedEvents)
        }

        kastExceptionHvisMislykkedeTransformasjoner(problematicEvents)
    }

    private fun kastExceptionHvisMislykkedeTransformasjoner(problematicEvents: MutableList<ConsumerRecord<Nokkel, Done>>) {
        if (problematicEvents.isNotEmpty()) {
            val message = "En eller flere eventer kunne ikke transformeres"
            val exception = UntransformableRecordException(message)
            exception.addContext("antallMislykkedeTransformasjoner", problematicEvents.size)
            throw exception
        }
    }
}
