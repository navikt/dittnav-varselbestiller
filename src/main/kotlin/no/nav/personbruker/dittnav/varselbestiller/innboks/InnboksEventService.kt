package no.nav.personbruker.dittnav.varselbestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.builders.exception.UnknownEventtypeException
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory

class InnboksEventService(
    private val doknotifikasjonProducer: DoknotifikasjonProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Innboks> {

    private val log = LoggerFactory.getLogger(InnboksEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Innboks>) {
        val successfullyValidatedEvents = mutableMapOf<String, Doknotifikasjon>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Innboks>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.INNBOKS) {
            events.forEach { event ->
                try {
                    val innboksKey = event.getNonNullKey()
                    val innboksEvent = event.value()
                    countAllEventsFromKafkaForSystemUser(innboksKey.getSystembruker())
                    if (innboksEvent.getEksternVarsling()) {
                        val doknotifikasjonKey = DoknotifikasjonCreator.createDoknotifikasjonKey(innboksKey, Eventtype.INNBOKS)
                        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(innboksKey, innboksEvent)
                        successfullyValidatedEvents[doknotifikasjonKey] = doknotifikasjon
                        varselbestillinger.add(VarselbestillingTransformer.fromInnboks(innboksKey, innboksEvent, doknotifikasjon))
                        countSuccessfulEksternvarslingForSystemUser(innboksKey.getSystembruker())
                    }
                } catch (e: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Innboks-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
                } catch (e: FieldValidationException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, innboks-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (e: UnknownEventtypeException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder ukjent eventtype, innboks-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (cce: ClassCastException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    val funnetType = event.javaClass.name
                    val eventId = event.eventId
                    val systembruker = event.systembruker
                    log.warn("Feil eventtype funnet på innboks-topic. Fant et event av typen $funnetType. Eventet blir forkastet. EventId: $eventId, systembruker: $systembruker", cce)
                }  catch (e: Exception) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    problematicEvents.add(event)
                    log.warn("Validering av innboks-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
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
        return doknotifikasjonProducer.sendAndPersistEvents(successfullyValidatedEvents, varselbestillinger)
    }

    private fun logDuplicateVarselbestillinger(eventMetricsSession: EventMetricsSession, duplicateVarselbestillinger: List<Varselbestilling>) {
        duplicateVarselbestillinger.forEach{
            log.info("Varsel med bestillingsid ${it.bestillingsId} er allerede bestilt, bestiller ikke på nytt.")
            eventMetricsSession.countDuplicateVarselbestillingForSystemUser(it.systembruker)
        }
    }

    private fun throwExceptionIfValidationFailed(problematicEvents: MutableList<ConsumerRecord<Nokkel, Innboks>>) {
        val message = "En eller flere innboks-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }
}
