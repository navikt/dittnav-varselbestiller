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
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<Nokkel, Done> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, Done>) {
        val successfullyValidatedEvents = mutableListOf<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>()
        val problematicEvents = mutableListOf<ConsumerRecord<Nokkel, Done>>()
        val varselbestillingerToCancel = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.DONE) {
            events.forEach { event ->
                try {
                    val doneKey = event.getNonNullKey()
                    countAllEventsFromKafkaForSystemUser(doneKey.getSystembruker())

                    val varselbestilling: Varselbestilling? = fetchVarselbestilling(event)
                    if (shouldCreateDoknotifikasjonStopp(varselbestilling)) {
                        val doknotifikasjonStoppKey = varselbestilling!!.bestillingsId
                        val doknotifikasjonStoppEvent = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
                        successfullyValidatedEvents.add(RecordKeyValueWrapper(doknotifikasjonStoppKey, doknotifikasjonStoppEvent))
                        varselbestillingerToCancel.add(varselbestilling)
                        countSuccessfulEksternvarslingForSystemUser(varselbestilling.systembruker)
                    }
                } catch (e: NokkelNullException) {
                    countNokkelWasNull()
                    log.warn("Done-eventet manglet nøkkel. Topic: ${event.topic()}, Partition: ${event.partition()}, Offset: ${event.offset()}", e)
                } catch (e: FieldValidationException) {
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Eventet kan ikke brukes fordi det inneholder valideringsfeil, done-eventet vil bli forkastet. EventId: ${event.eventId}", e)
                } catch (e: Exception) {
                    problematicEvents.add(event)
                    countFailedEksternvarslingForSystemUser(event.systembruker ?: "NoProducerSpecified")
                    log.warn("Validering av done-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            if (successfullyValidatedEvents.isNotEmpty()) {
                produceDoknotifikasjonStoppAndPersistToDB(successfullyValidatedEvents, varselbestillingerToCancel)
            }
            if (problematicEvents.isNotEmpty()) {
                throwExceptionIfFailedValidation(problematicEvents)
            }
        }
    }

    private fun shouldCreateDoknotifikasjonStopp(varselbestilling: Varselbestilling?): Boolean {
        var shouldCancel = false
        if (varselbestilling != null) {
            if (varselbestilling.avbestilt) {
                log.info("Varsel med bestillingsid ${varselbestilling.bestillingsId} allerede avbestilt, avbestiller ikke på nytt.")
            } else {
                shouldCancel = true
            }
        }
        return shouldCancel
    }

    private suspend fun produceDoknotifikasjonStoppAndPersistToDB(successfullyValidatedEvents: List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>, varselbestillingerToCancel: List<Varselbestilling>) {
        doknotifikasjonStoppProducer.produceDoknotifikasjonStop(successfullyValidatedEvents)
        varselbestillingRepository.cancelVarselbestilling(varselbestillingerToCancel)
    }

    private suspend fun fetchVarselbestilling(event: ConsumerRecord<Nokkel, Done>): Varselbestilling? {
        val doneKey = event.getNonNullKey()
        val doneValue = event.value()
        return varselbestillingRepository.fetchVarselbestilling(
                eventId = doneKey.getEventId(), systembruker = doneKey.getSystembruker(), fodselsnummer = doneValue.getFodselsnummer())
    }

    private fun throwExceptionIfFailedValidation(problematicEvents: MutableList<ConsumerRecord<Nokkel, Done>>) {
        if (problematicEvents.isNotEmpty()) {
            val message = "En eller flere done-eventer kunne ikke sendes til varselbestiller fordi validering feilet."
            val exception = UnvalidatableRecordException(message)
            exception.addContext("antallMislykkedValidering", problematicEvents.size)
            throw exception
        }
    }
}
