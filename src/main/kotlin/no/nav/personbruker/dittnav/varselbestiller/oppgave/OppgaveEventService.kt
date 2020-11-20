package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory

class OppgaveEventService(
        private val doknotifikasjonProducer: DoknotifikasjonProducer,
        private val varselbestillingRepository: VarselbestillingRepository
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, Doknotifikasjon>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Oppgave>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()
        events.forEach { event ->
            try {
                if(skalVarsleEksternt(event.value())) {
                    val oppgaveKey = event.getNonNullKey()
                    val oppgave = event.value()
                    val doknotifikasjonKey = DoknotifikasjonTransformer.createDoknotifikasjonKey(oppgaveKey, Eventtype.OPPGAVE)
                    val doknotifikasjon = DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(oppgaveKey, oppgave)
                    successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonKey, doknotifikasjon))
                    varselbestillinger.add(VarselbestillingTransformer.fromOppgave(oppgaveKey, oppgave, doknotifikasjon))
                }
            } catch (e: NokkelNullException) {
                log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
            } catch (e: FieldValidationException) {
                log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, oppgave-eventet vil bli forkastet. EventId: ${event.eventId}, context: ${e.context}", e)
            } catch (e: Exception) {
                problematicEvents.add(event)
                log.warn("Validering av oppgave-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
            }
        }
        if(successfullyValidatedEvents.isNotEmpty()) {
            produceDoknotifikasjonerAndPersistToDB(successfullyValidatedEvents, varselbestillinger)
        }
        if(problematicEvents.isNotEmpty()) {
            kastExceptionVedMislykkedValidering(problematicEvents)
        }
    }

    private suspend fun produceDoknotifikasjonerAndPersistToDB(successfullyValidatedEvents: MutableList<RecordKeyValueWrapper<String, Doknotifikasjon>>, varselbestillinger: List<Varselbestilling>) {
        doknotifikasjonProducer.produceDoknotifikasjon(successfullyValidatedEvents)
        varselbestillingRepository.persistInOneBatch(varselbestillinger)
    }

    private fun skalVarsleEksternt(event: Oppgave?): Boolean {
        return event != null && event.getEksternVarsling()
    }

    private fun kastExceptionVedMislykkedValidering(problematicEvents: MutableList<ConsumerRecord<Nokkel, Oppgave>>) {
        val message = "En eller flere oppgave-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }
}
