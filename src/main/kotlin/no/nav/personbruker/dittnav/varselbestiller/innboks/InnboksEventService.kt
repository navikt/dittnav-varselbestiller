package no.nav.personbruker.dittnav.varselbestiller.innboks


import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.Producer
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
) : EventBatchProcessorService<NokkelIntern, InnboksIntern> {

    private val log = LoggerFactory.getLogger(InnboksEventService::class.java)

    override suspend fun processEvents(events: ConsumerRecords<NokkelIntern, InnboksIntern>) {
        val successfullyTransformedEvents = mutableMapOf<String, Doknotifikasjon>()
        val problematicEvents = mutableListOf<ConsumerRecord<NokkelIntern, InnboksIntern>>()
        val varselbestillinger = mutableListOf<Varselbestilling>()

        metricsCollector.recordMetrics(eventType = Eventtype.INNBOKS_INTERN) {
            events.forEach { event ->
                try {
                    val innboksKey = event.key()
                    val innboksEvent = event.value()
                    val producer = Producer(event.namespace, event.appnavn)
                    countAllEventsFromKafkaForProducer(producer)
                    if (innboksEvent.getEksternVarsling()) {
                        val doknotifikasjonKey = DoknotifikasjonCreator.createDoknotifikasjonKey(innboksKey, Eventtype.INNBOKS_INTERN)
                        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(innboksKey, innboksEvent)
                        successfullyTransformedEvents[doknotifikasjonKey] = doknotifikasjon
                        varselbestillinger.add(VarselbestillingTransformer.fromInnboks(innboksKey, innboksEvent, doknotifikasjon))
                        countSuccessfulEksternVarslingForProducer(producer)
                    }
                }  catch (e: Exception) {
                    countFailedEksternVarslingForProducer(Producer(event.namespace, event.appnavn))
                    problematicEvents.add(event)
                    log.warn("Validering av innboks-event fra Kafka fikk en uventet feil, fullfører batch-en.", e)
                }
            }
            if (successfullyTransformedEvents.isNotEmpty()) {
                produceDoknotifikasjonerAndPersistToDB(this, successfullyTransformedEvents, varselbestillinger)
            }
            if (problematicEvents.isNotEmpty()) {
                throwExceptionForProblematicEvents(problematicEvents)
            }
        }
    }

    private suspend fun produceDoknotifikasjonerAndPersistToDB(eventMetricsSession: EventMetricsSession,
                                                               successfullyTransformedEvents: Map<String, Doknotifikasjon>,
                                                               varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        val duplicateVarselbestillinger = varselbestillingRepository.fetchVarselbestillingerForBestillingIds(successfullyTransformedEvents.keys.toList())
        return if(duplicateVarselbestillinger.isEmpty()) {
            produce(successfullyTransformedEvents, varselbestillinger)
        } else {
            val duplicateBestillingIds = duplicateVarselbestillinger.map { it.bestillingsId }
            val remainingTransformedEvents = successfullyTransformedEvents.filterKeys { bestillingsId -> !duplicateBestillingIds.contains(bestillingsId) }
            val varselbestillingerToOrder = varselbestillinger.filter { !duplicateBestillingIds.contains(it.bestillingsId)}
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

    private fun throwExceptionForProblematicEvents(problematicEvents: MutableList<ConsumerRecord<NokkelIntern, InnboksIntern>>) {
        val message = "En eller flere innboks-eventer kunne ikke sendes til varselbestiller fordi transformering feilet."
        val exception = UntransformableRecordException(message)
        exception.addContext("antallMislykkedeTransformasjoner", problematicEvents.size)
        throw exception
    }
}
