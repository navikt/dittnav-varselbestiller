package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType.OPPGAVE
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory

class OppgaveEventService(
        private val doknotifikasjonProducer: DoknotifikasjonProducer,
        private val metricsProbe: EventMetricsProbe
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, Doknotifikasjon>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Oppgave>>()

        metricsProbe.runWithMetrics(eventType = OPPGAVE) {
            events.forEach { event ->
                try {
                    if(skalVarsleEksternt(event.value())) {
                        val doknotifikasjonKey = event.getNonNullKey().getEventId()
                        val doknotifikasjonEvent = DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(event.getNonNullKey(), event.value())
                        successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonKey, doknotifikasjonEvent))
                        countSuccessfulEventForProducer(event.getNonNullKey().getSystembruker())
                    }
                } catch (e: NokkelNullException) {
                    countFailedEventForProducer("NoProducerSpecified")
                    log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
                } catch (e: FieldValidationException) {
                    countFailedEventForProducer(event.getNonNullKey().getSystembruker())
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, oppgave-eventet vil bli forkastet. EventId: ${event.getNonNullKey().getEventId()}", e)
                } catch (e: Exception) {
                    countFailedEventForProducer(event.getNonNullKey().getSystembruker())
                    problematicEvents.add(event)
                    log.warn("Validering av oppgave-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            doknotifikasjonProducer.produceDoknotifikasjon(successfullyValidatedEvents)
        }
        kastExceptionHvisMislykkedValidering(problematicEvents)
    }

    private fun skalVarsleEksternt(event: Oppgave): Boolean {
        return event.getEksternVarsling()
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
