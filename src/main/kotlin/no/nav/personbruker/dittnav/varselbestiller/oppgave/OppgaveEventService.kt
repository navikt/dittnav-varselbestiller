package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnvalidatableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer.getNonNullKey
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
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
        private val doknotifikasjonProducer: DoknotifikasjonProducer,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Oppgave> {

    private val log = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Oppgave>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, Doknotifikasjon>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Oppgave>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.OPPGAVE) {
            events.forEach { event ->
                try {
                    val oppgaveKey = event.getNonNullKey()
                    countAllEventsFromKafkaForSystemUser(oppgaveKey.getSystembruker())
                    if (shouldCreateDoknotifikasjon(event)) {
                        val oppgave = event.value()
                        val doknotifikasjonKey = DoknotifikasjonCreator.createDoknotifikasjonKey(oppgaveKey, Eventtype.OPPGAVE)
                        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(oppgaveKey, oppgave)
                        successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonKey, doknotifikasjon))
                        varselbestillinger.add(VarselbestillingTransformer.fromOppgave(oppgaveKey, oppgave, doknotifikasjon))
                        countSuccessfulEksternvarslingForSystemUser(oppgaveKey.getSystembruker())
                    }
                } catch (e: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Oppgave-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
                } catch (e: FieldValidationException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, oppgave-eventet vil bli forkastet. EventId: ${event.eventId}, context: ${e.context}", e)
                } catch (e: Exception) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    problematicEvents.add(event)
                    log.warn("Validering av oppgave-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }

            if (successfullyValidatedEvents.isNotEmpty()) {
                val result = produceDoknotifikasjonerAndPersistToDB(successfullyValidatedEvents, varselbestillinger)
                countDuplicateKeyEvents(result)
            }
            if (problematicEvents.isNotEmpty()) {
                throwExceptionIfValidationFailed(problematicEvents)
            }
        }
    }

    private suspend fun produceDoknotifikasjonerAndPersistToDB(successfullyValidatedEvents: MutableList<RecordKeyValueWrapper<String, Doknotifikasjon>>, varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        doknotifikasjonProducer.produceDoknotifikasjon(successfullyValidatedEvents)
        return varselbestillingRepository.persistInOneBatch(varselbestillinger)
    }

    private suspend fun shouldCreateDoknotifikasjon(event: ConsumerRecord<Nokkel, Oppgave>): Boolean {
        val oppgaveKey = event.getNonNullKey()
        val oppgave = event.value()
        val bestillingsid = DoknotifikasjonCreator.createDoknotifikasjonKey(oppgaveKey, Eventtype.OPPGAVE)
        var shouldCreate = false
        if(oppgave.getEksternVarsling()) {
            if(alreadyCreated(bestillingsid)) {
                log.info("Varsel med bestillingsid $bestillingsid er allerede bestilt, bestiller ikke på nytt.")
            } else {
                shouldCreate = true
            }
        }
        return shouldCreate
    }

    private suspend fun alreadyCreated(bestillingsId: String): Boolean {
        val varselbestilling = varselbestillingRepository.fetchVarselbestilling(bestillingsId = bestillingsId)
        return varselbestilling != null
    }

    private fun throwExceptionIfValidationFailed(problematicEvents: MutableList<ConsumerRecord<Nokkel, Oppgave>>) {
        val message = "En eller flere oppgave-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
        val exception = UnvalidatableRecordException(message)
        exception.addContext("antallMislykkedValidering", problematicEvents.size)
        throw exception
    }

    private fun EventMetricsSession.countDuplicateKeyEvents(result: ListPersistActionResult<Varselbestilling>) {
        if (result.foundConflictingKeys()) {
            val constraintErrors = result.getConflictingEntities().size
            val totalEntities = result.getAllEntities().size

            countDuplicateKeyEksternvarslingBySystemUser(result)

            val msg = """Traff $constraintErrors feil på duplikate eventId-er ved behandling av $totalEntities oppgave-eventer.
                           | Feilene ble produsert av: ${getEksternvarslingDuplicateKeys()}""".trimMargin()
            log.warn(msg)
        }
    }
}
