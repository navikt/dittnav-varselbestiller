package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.builders.exception.UnknownEventtypeException
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BeskjedEventService(
        private val doknotifikasjonProducer: Producer<String, Doknotifikasjon>,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Beskjed> {

    private val log: Logger = LoggerFactory.getLogger(BeskjedEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Beskjed>) {
        val successfullyValidatedEvents = mutableMapOf<String, Doknotifikasjon>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Beskjed>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.BESKJED) {
            events.forEach { event ->
                try {
                    val beskjedKey = event.getNonNullKey()
                    val beskjedEvent = event.value()
                    countAllEventsFromKafkaForSystemUser(beskjedKey.getSystembruker())
                    if(beskjedEvent.getEksternVarsling()) {
                        val doknotifikasjonKey = DoknotifikasjonCreator.createDoknotifikasjonKey(beskjedKey, Eventtype.BESKJED)
                        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(beskjedKey, beskjedEvent)
                        successfullyValidatedEvents[doknotifikasjonKey] = doknotifikasjon
                        varselbestillinger.add(VarselbestillingTransformer.fromBeskjed(beskjedKey, beskjedEvent, doknotifikasjon))
                        countSuccessfulEksternvarslingForSystemUser(beskjedKey.getSystembruker())
                    }
                } catch (nne: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Beskjed-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", nne)
                } catch (fve: FieldValidationException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, beskjed-eventet vil bli forkastet. EventId: ${event.eventId}", fve)
                } catch (e: UnknownEventtypeException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder ukjent eventtype, beskjed-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (e: Exception) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    problematicEvents.add(event)
                    log.warn("Validering av beskjed-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            if (successfullyValidatedEvents.isNotEmpty()) {
                produceDoknotifikasjonerAndPersistToDB(this, successfullyValidatedEvents, varselbestillinger)
            }
            if (problematicEvents.isNotEmpty()) {
                throwExceptionIfFailedValidation(problematicEvents)
            }
        }
    }

    private suspend fun produceDoknotifikasjonerAndPersistToDB(eventMetricsSession: EventMetricsSession,
                                                               successfullyValidatedEvents: Map<String, Doknotifikasjon>,
                                                               varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        val duplicateVarselbestillinger = varselbestillingRepository.fetchVarselbestillingerForBestillingIds(successfullyValidatedEvents.keys.toList())
        return if(duplicateVarselbestillinger.isEmpty()) {
            order(successfullyValidatedEvents, varselbestillinger)
        } else {
            val duplicateBestillingIds = duplicateVarselbestillinger.map { it.bestillingsId }
            val remainingValidatedEvents = successfullyValidatedEvents.filterKeys { bestillingsId -> !duplicateBestillingIds.contains(bestillingsId) }
            val varselbestillingerToOrder = varselbestillinger.filter { !duplicateBestillingIds.contains(it.bestillingsId) }
            logDuplicateVarselbestillinger(eventMetricsSession, duplicateVarselbestillinger)
            order(remainingValidatedEvents, varselbestillingerToOrder)
        }
    }

    private suspend fun order(successfullyValidatedEvents: Map<String, Doknotifikasjon>, varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
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

    private fun throwExceptionIfFailedValidation(problematicEvents: MutableList<ConsumerRecord<Nokkel, Beskjed>>) {
        val message = "En eller flere beskjed-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }
}
