package no.nav.personbruker.dittnav.varselbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
        private val varselbestillingRepository: VarselbestillingRepository
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Done>>()

        events.forEach { event ->
            try {
                val varselbestilling: Varselbestilling? = fetchVarselbestilling(event)
                if(varselbestilling != null) {
                    val doneKey = event.getNonNullKey()
                    val doknotifikasjonStoppKey = DoknotifikasjonTransformer.createDoknotifikasjonKey(doneKey, Eventtype.DONE)
                    val doknotifikasjonStoppEvent = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(doneKey)
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

    private suspend fun fetchVarselbestilling(event: ConsumerRecord<Nokkel, Done>): Varselbestilling? {
        val doneKey = event.getNonNullKey()
        val doneValue = event.value()
        val varselbestilling = varselbestillingRepository.fetchVarselbestilling(
                eventId = doneKey.getEventId(), systembruker = doneKey.getSystembruker(), fodselsnummer = doneValue.getFodselsnummer())
        return varselbestilling
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
