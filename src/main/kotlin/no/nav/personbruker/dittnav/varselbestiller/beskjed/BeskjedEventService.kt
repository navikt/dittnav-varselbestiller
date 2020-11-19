package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonRepository
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BeskjedEventService(
        private val doknotifikasjonProducer: DoknotifikasjonProducer,
        private val doknotifikasjonRepository: DoknotifikasjonRepository
) : EventBatchProcessorService<Nokkel, Beskjed> {

    private val log: Logger = LoggerFactory.getLogger(BeskjedEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, Doknotifikasjon>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Beskjed>>()
        events.forEach { event ->
            try {
                if (skalVarsleEksternt(event.value())) {
                    val beskjedKey = event.getNonNullKey()
                    val beskjed = event.value()
                    val doknotifikasjonKey = DoknotifikasjonTransformer.createDoknotifikasjonKey(beskjedKey, Eventtype.BESKJED)
                    val doknotifikasjon = DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(beskjedKey, beskjed)
                    successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonKey, doknotifikasjon))
                }
            } catch (nne: NokkelNullException) {
                log.warn("Beskjed-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
            } catch (fve: FieldValidationException) {
                log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, beskjed-eventet vil bli forkastet. EventId: ${event.eventId}, context: ${fve.context}", fve)
            } catch (e: Exception) {
                problematicEvents.add(event)
                log.warn("Validering av beskjed-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
            }
        }
        if(successfullyValidatedEvents.isNotEmpty()) {
            produceDoknotifikasjonerAndPersistToDB(successfullyValidatedEvents)
        }
        if(problematicEvents.isNotEmpty()) {
            kastExceptionHvisMislykkedValidering(problematicEvents)
        }
    }

    private suspend fun produceDoknotifikasjonerAndPersistToDB(successfullyValidatedEvents: MutableList<RecordKeyValueWrapper<String, Doknotifikasjon>>) {
        doknotifikasjonProducer.produceDoknotifikasjon(successfullyValidatedEvents)
        val doknotifikasjoner = successfullyValidatedEvents.map { DoknotifikasjonTransformer.toInternal(Eventtype.BESKJED, it.value) }
        doknotifikasjonRepository.createInOneBatch(doknotifikasjoner)
    }

    private fun skalVarsleEksternt(event: Beskjed?): Boolean {
        return event != null && event.getEksternVarsling()
    }

    private fun kastExceptionHvisMislykkedValidering(problematicEvents: MutableList<ConsumerRecord<Nokkel, Beskjed>>) {
        val message = "En eller flere beskjed-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }
}
