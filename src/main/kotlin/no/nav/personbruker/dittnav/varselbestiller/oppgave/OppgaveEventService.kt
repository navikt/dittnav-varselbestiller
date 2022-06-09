package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.Producer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaveEventService(
    private val doknotifikasjonProducer: DoknotifikasjonProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val metricsCollector: MetricsCollector
) : EventBatchProcessorService<NokkelIntern, OppgaveIntern> {

    private val log: Logger = LoggerFactory.getLogger(OppgaveEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelIntern, OppgaveIntern>) {
        val viableEntries = mutableListOf<BestillingWrapper>()
        val problematicEvents = mutableListOf<ConsumerRecord<NokkelIntern, OppgaveIntern>>()

        metricsCollector.recordMetrics(eventType = Eventtype.OPPGAVE_INTERN) {
            events.forEach { event ->
                val producer = Producer(event.namespace, event.appnavn)

                try {
                    countAllEventsFromKafkaForProducer(producer)

                    if(event.isEksternVarsling()) {
                        val bestilling = BestillingTransformer.transformAndWrapEvent(event.key(), event.value())
                        viableEntries.add(bestilling)
                        countSuccessfulEksternVarslingForProducer(producer)
                    }
                } catch (e: Exception) {
                    countFailedEksternVarslingForProducer(producer)
                    problematicEvents.add(event)
                    log.warn("Transformasjon av oppgave-event fra Kafka feilet, fullfører batch-en før polling stoppes.", e)
                }
            }

            if (viableEntries.isNotEmpty()) {
                val (uniques, duplicates) = partitionUniquesAndDuplicates(viableEntries)

                produceBestilling(uniques)

                logDuplicates(duplicates)
            }
        }

        if (problematicEvents.isNotEmpty()) {
            throwExceptionForProblematicEvents(problematicEvents)
        }
    }

    private suspend fun partitionUniquesAndDuplicates(bestillingList: List<BestillingWrapper>): Pair<List<BestillingWrapper>, List<BestillingWrapper>> {
        val eventIds = bestillingList.map { (varselbestilling, _) -> varselbestilling.eventId }

        val duplicateVarselbestillinger = varselbestillingRepository.fetchVarselbestillingerForEventIds(eventIds)
        val duplicateEventIds = duplicateVarselbestillinger.map { it.eventId }

        return bestillingList.partition { !duplicateEventIds.contains(it.varselbestilling.eventId) }
    }

    private suspend fun produceBestilling(bestillingList: List<BestillingWrapper>) {
        if (bestillingList.isEmpty()) {
            return
        }

        val varselbestillingList = bestillingList.map { it.varselbestilling }
        val doknotifikasjonList = bestillingList.map { it.doknotifikasjon }
        doknotifikasjonProducer.sendAndPersistBestillingBatch(varselbestillingList, doknotifikasjonList)
    }

    private fun EventMetricsSession.logDuplicates(duplicateBestillinger: List<BestillingWrapper>) {
        duplicateBestillinger.forEach {
            val varselbestilling = it.varselbestilling

            log.info("Varsel med eventId ${varselbestilling.eventId} er allerede bestilt, bestiller ikke på nytt.")
            countDuplicateVarselbestillingForProducer(Producer(varselbestilling.namespace, varselbestilling.appnavn))
        }
    }

    private fun throwExceptionForProblematicEvents(problematicEvents: MutableList<ConsumerRecord<NokkelIntern, OppgaveIntern>>) {
        val message = "En eller flere oppgave-eventer kunne ikke sendes til varselbestiller fordi transformering feilet."
        val exception = UntransformableRecordException(message)
        exception.addContext("antallMislykkedeTransformasjoner", problematicEvents.size)
        throw exception
    }

    private fun ConsumerRecord<NokkelIntern, OppgaveIntern>.isEksternVarsling() = value().getEksternVarsling()
}
