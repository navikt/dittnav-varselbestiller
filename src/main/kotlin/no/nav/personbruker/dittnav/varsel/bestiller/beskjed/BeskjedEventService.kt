package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType.BESKJED
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BeskjedEventService(
        private val doknotifikasjonProducer: DoknotifikasjonProducer,
        private val metricsProbe: EventMetricsProbe
) : EventBatchProcessorService<Nokkel, Beskjed> {

    private val log: Logger = LoggerFactory.getLogger(BeskjedEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, Doknotifikasjon>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Beskjed>>()

        metricsProbe.runWithMetrics(eventType = BESKJED) {
            events.forEach { event ->
                try {
                    if (skalVarsleEksternt(event.value())) {
                        val doknotifikasjonKey = event.eventId
                        val doknotifikasjonEvent = DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(event.getNonNullKey(), event.value())
                        successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonKey, doknotifikasjonEvent))
                        countSuccessfulEventForProducer(event.systembruker)
                    }
                } catch (nne: NokkelNullException) {
                    countFailedEventForProducer("NoProducerSpecified")
                    log.warn("Beskjed-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEventForProducer(event.systembruker)
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, beskjed-eventet vil bli forkastet. EventId: ${event.eventId}, context: ${fve.context}", fve)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.systembruker)
                    problematicEvents.add(event)
                    log.warn("Validering av beskjed-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            doknotifikasjonProducer.produceDoknotifikasjon(successfullyValidatedEvents)
        }
        kastExceptionHvisMislykkedValidering(problematicEvents)
    }

    private fun skalVarsleEksternt(event: Beskjed): Boolean {
        return event.getEksternVarsling()
    }

    private fun kastExceptionHvisMislykkedValidering(problematicEvents: MutableList<ConsumerRecord<Nokkel, Beskjed>>) {
        if (problematicEvents.isNotEmpty()) {
            val message = "En eller flere beskjed-eventer kunne ikke sendes til varsel-bestiller fordi validering feilet."
            val exception = UnvalidatableRecordException(message)
            exception.addContext("antallMislykkedValidering", problematicEvents.size)
            throw exception
        }
    }
}
