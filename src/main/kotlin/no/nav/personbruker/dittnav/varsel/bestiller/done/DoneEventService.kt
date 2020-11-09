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
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonTransformer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Done>>()

        events.forEach { event ->
            try {
                if(harBestiltEksternVarsling(event.value())) {
                    val doneKey = event.getNonNullKey()
                    val doknotifikasjonStoppKey = DoknotifikasjonTransformer.createDoknotifikasjonKey(doneKey, EventType.DONE)
                    val doknotifikasjonStoppEvent = DoknotifikasjonTransformer.createDoknotifikasjonStopp(doneKey)
                    successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonStoppKey, doknotifikasjonStoppEvent))
                }
            } catch (e: NokkelNullException) {
                log.warn("Done-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
            } catch (e: FieldValidationException) {
                log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, done-eventet vil bli forkastet. EventId: ${event.eventId}", e)
            } catch (e: Exception) {
                problematicEvents.add(event)
                log.warn("Validering av done-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
            }
        }
        doknotifikasjonStoppProducer.produceDoknotifikasjonStop(successfullyValidatedEvents)
        kastExceptionHvisMislykkedValidering(problematicEvents)
    }

    private fun harBestiltEksternVarsling(value: Done): Boolean {
        // Legge til en sjekk på om brukernotifikasjonen tilhørende Done-eventet faktisk har bestilt eksternt varsel
        return false
    }

    private fun kastExceptionHvisMislykkedValidering(problematicEvents: MutableList<ConsumerRecord<Nokkel, Done>>) {
        if (problematicEvents.isNotEmpty()) {
            val message = "En eller flere done-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
            val exception = UnvalidatableRecordException(message)
            exception.addContext("antallMislykkedValidering", problematicEvents.size)
            throw exception
        }
    }
}
