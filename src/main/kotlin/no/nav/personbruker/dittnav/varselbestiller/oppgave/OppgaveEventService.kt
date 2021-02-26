package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.builders.exception.UnknownEventtypeException
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory

class OppgaveEventService(
        private val doknotifikasjonProducer: Producer<String, Doknotifikasjon>,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableMapOf<String, Doknotifikasjon>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Oppgave>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.OPPGAVE) {
            events.forEach { event ->
                try {
                    val oppgaveKey = event.getNonNullKey()
                    val oppgaveEvent = event.value()
                    countAllEventsFromKafkaForSystemUser(oppgaveKey.getSystembruker())
                    if (oppgaveEvent.getEksternVarsling()) {
                        val doknotifikasjonKey = DoknotifikasjonCreator.createDoknotifikasjonKey(oppgaveKey, Eventtype.OPPGAVE)
                        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(oppgaveKey, oppgaveEvent)
                        successfullyValidatedEvents[doknotifikasjonKey] = doknotifikasjon
                        varselbestillinger.add(VarselbestillingTransformer.fromOppgave(oppgaveKey, oppgaveEvent, doknotifikasjon))
                        countSuccessfulEksternvarslingForSystemUser(oppgaveKey.getSystembruker())
                    }
                } catch (e: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
                } catch (e: FieldValidationException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, oppgave-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (e: UnknownEventtypeException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder ukjent eventtype, oppgave-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (e: Exception) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    problematicEvents.add(event)
                    log.warn("Validering av oppgave-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            if (successfullyValidatedEvents.isNotEmpty()) {
                produceDoknotifikasjonerAndPersistToDB(this, successfullyValidatedEvents, varselbestillinger)
            }
            if (problematicEvents.isNotEmpty()) {
                throwExceptionIfValidationFailed(problematicEvents)
            }
        }
    }

    private suspend fun produceDoknotifikasjonerAndPersistToDB(eventMetricsSession: EventMetricsSession,
                                                               successfullyValidatedEvents: Map<String, Doknotifikasjon>,
                                                               varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        val duplicateVarselbestillinger = varselbestillingRepository.fetchVarselbestillingerForBestillingIds(successfullyValidatedEvents.keys.toList())
        return if(duplicateVarselbestillinger.isEmpty()) {
            produce(successfullyValidatedEvents, varselbestillinger)
        } else {
            val duplicateBestillingIds = duplicateVarselbestillinger.map { it.bestillingsId }
            val remainingValidatedEvents = successfullyValidatedEvents.filterKeys { bestillingsId -> !duplicateBestillingIds.contains(bestillingsId) }
            val varselbestillingerToOrder = varselbestillinger.filter { !duplicateBestillingIds.contains(it.bestillingsId)}
            logDuplicateVarselbestillinger(eventMetricsSession, duplicateVarselbestillinger)
            produce(remainingValidatedEvents, varselbestillingerToOrder)
        }
    }

    private suspend fun produce(successfullyValidatedEvents: Map<String, Doknotifikasjon>, varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        val events = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
        doknotifikasjonProducer.produceEvents(events)
        return varselbestillingRepository.persistInOneBatch(varselbestillinger)
    }

    private fun logDuplicateVarselbestillinger(eventMetricsSession: EventMetricsSession, duplicateVarselbestillinger: List<Varselbestilling>) {
        duplicateVarselbestillinger.forEach{
            log.info("Varsel med bestillingsid ${it.bestillingsId} er allerede bestilt, bestiller ikke på nytt.")
            eventMetricsSession.countDuplicateVarselbestillingForSystemUser(it.systembruker)
        }
    }

    private fun throwExceptionIfValidationFailed(problematicEvents: MutableList<ConsumerRecord<Nokkel, Oppgave>>) {
        val message = "En eller flere oppgave-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }
}
