package no.nav.personbruker.dittnav.varselbestiller.done

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.Producer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneEventService(
        private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
        private val varselbestillingRepository: VarselbestillingRepository,
        private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<NokkelIntern, DoneIntern> {

    private val log: Logger = LoggerFactory.getLogger(DoneEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelIntern, DoneIntern>) {
        val doknotifikasjonStoppList = mutableListOf<DoknotifikasjonStopp>()
        val problematicEntries = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.DONE_INTERN) {
            val doneEventKeys = extractNokkelAndCountEvents(events)

            val eventIds = doneEventKeys.map { it.getEventId() }
            val existingVarselbestillingList = varselbestillingRepository.fetchVarselbestillingerForEventIds(eventIds)

            existingVarselbestillingList.forEach { varselbestilling ->
                try {
                    countWhetherBestillingsIdAndEventIdDiffer(varselbestilling)

                    if(varselbestilling.avbestilt) {
                        log.info("Varsel med bestillingsid ${varselbestilling.bestillingsId} allerede avbestilt, avbestiller ikke på nytt.")
                        countDuplicateVarselbestillingForProducer(Producer(varselbestilling.namespace, varselbestilling.appnavn))
                    } else {
                        val doknotifikasjonStopp = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
                        doknotifikasjonStoppList.add(doknotifikasjonStopp)
                        countSuccessfulEksternVarslingForProducer(Producer(varselbestilling.namespace, varselbestilling.appnavn))
                    }
                } catch (e: Exception) {
                    log.warn("Eventet kan ikke brukes pga en ukjent feil, done-eventet vil bli forkastet. EventId: ${varselbestilling.eventId}", e)
                    countFailedEksternVarslingForProducer(Producer(varselbestilling.namespace, varselbestilling.appnavn))
                    problematicEntries.add(varselbestilling)
                }
            }

            if (doknotifikasjonStoppList.isNotEmpty()) {
                produceDoknotifikasjonStoppAndPersistToDB(doknotifikasjonStoppList)
            }
        }

        if(problematicEntries.isNotEmpty()) {
            throwExceptionIfFailedValidation(problematicEntries)
        }
    }

    private fun EventMetricsSession.extractNokkelAndCountEvents(events: ConsumerRecords<NokkelIntern, DoneIntern>): List<NokkelIntern> {
        return events.map { event ->
            val producer = Producer(event.key().getNamespace(), event.key().getAppnavn())
            countAllEventsFromKafkaForProducer(producer)

            event.key()
        }
    }

    private suspend fun produceDoknotifikasjonStoppAndPersistToDB(successfullyValidatedEvents: List<DoknotifikasjonStopp>) {
        doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(successfullyValidatedEvents)
    }

    private fun throwExceptionIfFailedValidation(problematicEvents: List<Varselbestilling>) {
        val message = "En eller flere done-eventer kunne ikke sendes til varselbestiller."
        val exception = UntransformableRecordException(message)
        exception.addContext("antallMislykkedeTransformasjoner", problematicEvents.size)
        throw exception
    }

    private fun EventMetricsSession.countWhetherBestillingsIdAndEventIdDiffer(bestilling: Varselbestilling) {
        if (bestilling.bestillingsId != bestilling.eventId) {
            countBestillingsIdAndEventIdDiffered(Producer(bestilling.namespace, bestilling.appnavn))
        }
    }
}
