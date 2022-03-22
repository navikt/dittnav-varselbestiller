package no.nav.personbruker.dittnav.varselbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation.EarlyCancellation
import no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation.EarlyCancellationRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.Producer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractVarselbestillerForInternalEvent<V>(
    private val doknotifikasjonProducer: DoknotifikasjonProducer,
    private val varselbestillingRepository: VarselbestillingRepository,
    private val earlyCancellationRepository: EarlyCancellationRepository,
    private val metricsCollector: MetricsCollector,
    private val eventType: Eventtype
): EventBatchProcessorService<NokkelIntern, V>{

    private val log: Logger = LoggerFactory.getLogger(AbstractVarselbestillerForInternalEvent::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelIntern, V>) {
        val successfullyTransformedEvents = mutableMapOf<String, Doknotifikasjon>()
        val problematicEvents = mutableListOf<ConsumerRecord<NokkelIntern, V>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()
        val usedEarlyCancellations = mutableListOf<EarlyCancellation>()

        val metricsSession = metricsCollector.createSession(eventType)

        val eventIds = events.mapNotNull { it.eventId }
        val earlyCancellations = earlyCancellationRepository.findByEventIds(eventIds)
        events.forEach { record ->
            try {
                val key = record.key()
                val event = record.value()
                val producer = Producer(record.namespace, record.appnavn)
                metricsSession.countAllEventsFromKafkaForProducer(producer)
                if(hasEksternVarsling(event)) {
                    val earlyCancellation = earlyCancellations.firstOrNull { it.eventId == record.eventId }
                    if (earlyCancellation != null) {
                        log.info("${eventType.eventtype}-eventet var tidligere kansellert av ${earlyCancellation.appnavn} den ${earlyCancellation.tidspunkt}")
                        usedEarlyCancellations.add(earlyCancellation)
                        return@forEach
                    }

                    val doknotifikasjonKey = DoknotifikasjonCreator.createDoknotifikasjonKey(key, eventType)
                    val doknotifikasjon = createDoknotifikasjon(key, event)
                    successfullyTransformedEvents[doknotifikasjonKey] = doknotifikasjon
                    varselbestillinger.add(createVarselbestilling(key, event, doknotifikasjon))
                    metricsSession.countSuccessfulEksternVarslingForProducer(producer)
                }
            } catch (e: Exception) {
                metricsSession.countFailedEksternVarslingForProducer(Producer(record.namespace, record.appnavn))
                problematicEvents.add(record)
                log.warn("Transformasjon av ${eventType.eventtype}-event fra Kafka feilet, fullfører batch-en før polling stoppes.", e)
            }
        }
        if (successfullyTransformedEvents.isNotEmpty()) {
            produceDoknotifikasjonerAndPersistToDB(metricsSession, successfullyTransformedEvents, varselbestillinger)
        }
        if (usedEarlyCancellations.isNotEmpty()) {
            earlyCancellationRepository.deleteByEventIds(usedEarlyCancellations.map { it.eventId })
        }
        if (problematicEvents.isNotEmpty()) {
            throwExceptionForProblematicEvents(problematicEvents)
        }
        metricsCollector.processSession(metricsSession)
    }

    abstract fun createVarselbestilling(key: NokkelIntern, event: V, doknotifikasjon: Doknotifikasjon): Varselbestilling

    abstract fun createDoknotifikasjon(key: NokkelIntern, event: V): Doknotifikasjon

    abstract fun hasEksternVarsling(event: V): Boolean

    private suspend fun produceDoknotifikasjonerAndPersistToDB(eventMetricsSession: EventMetricsSession,
                                                               successfullyTransformedEvents: Map<String, Doknotifikasjon>,
                                                               varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        val duplicateVarselbestillinger = varselbestillingRepository.fetchVarselbestillingerForBestillingIds(successfullyTransformedEvents.keys.toList())
        return if(duplicateVarselbestillinger.isEmpty()) {
            produce(successfullyTransformedEvents, varselbestillinger)
        } else {
            val duplicateBestillingIds = duplicateVarselbestillinger.map { it.bestillingsId }
            val remainingTransformedEvents = successfullyTransformedEvents.filterKeys { !duplicateBestillingIds.contains(it) }
            val varselbestillingerToOrder = varselbestillinger.filter { !duplicateBestillingIds.contains(it.bestillingsId) }
            logDuplicateVarselbestillinger(eventMetricsSession, duplicateVarselbestillinger)
            produce(remainingTransformedEvents, varselbestillingerToOrder)
        }
    }

    private suspend fun produce(successfullyTransformedEvents: Map<String, Doknotifikasjon>, varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        return doknotifikasjonProducer.sendAndPersistEvents(successfullyTransformedEvents, varselbestillinger)
    }

    private fun logDuplicateVarselbestillinger(eventMetricsSession: EventMetricsSession, duplicateVarselbestillinger: List<Varselbestilling>) {
        duplicateVarselbestillinger.forEach{
            log.info("Varsel med bestillingsid ${it.bestillingsId} er allerede bestilt, bestiller ikke på nytt.")
            eventMetricsSession.countDuplicateVarselbestillingForProducer(Producer(it.namespace, it.appnavn))
        }
    }

    private fun throwExceptionForProblematicEvents(problematicEvents: MutableList<ConsumerRecord<NokkelIntern, V>>) {
        val message = "En eller flere ${eventType.eventtype}-eventer kunne ikke sendes til varselbestiller fordi transformering feilet."
        val exception = UntransformableRecordException(message)
        exception.addContext("antallMislykkedeTransformasjoner", problematicEvents.size)
        throw exception
    }
}
