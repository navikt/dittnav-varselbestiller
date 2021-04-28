package no.nav.personbruker.dittnav.varselbestiller.common.kafka

import kotlinx.coroutines.*
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.UnretriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.UnretriableKafkaException
import no.nav.personbruker.dittnav.varselbestiller.health.HealthCheck
import no.nav.personbruker.dittnav.varselbestiller.health.HealthStatus
import no.nav.personbruker.dittnav.varselbestiller.health.Status
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

class Consumer<K, V>(
        val topic: String,
        val kafkaConsumer: KafkaConsumer<K, V>,
        val eventBatchProcessorService: EventBatchProcessorService<K, V>,
        val job: Job = Job(),
        val timeToPauseInCaseOfPeriodicErrorsInMs : Long = ONE_MINUTE_IN_MS
) : CoroutineScope, HealthCheck {

    private val log: Logger = LoggerFactory.getLogger(Consumer::class.java)

    companion object {
        private const val ONE_MINUTE_IN_MS = 60000L
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    suspend fun stopPolling() {
        job.cancelAndJoin()
    }

    fun isCompleted(): Boolean {
        return job.isCompleted
    }

    fun isStopped(): Boolean {
        return !job.isActive
    }

    override suspend fun status(): HealthStatus {
        val serviceName = topic + "consumer"
        return if (job.isActive) {
            HealthStatus(serviceName, Status.OK, "Consumer is running", includeInReadiness = false)
        } else {
            log.error("Selftest mot Kafka-consumere , consumer kjører ikke.")
            HealthStatus(serviceName, Status.ERROR, "Consumer is not running", includeInReadiness = false)
        }
    }

    fun startPolling() {
        launch {
            kafkaConsumer.use { consumer ->
                consumer.subscribe(listOf(topic))

                while (job.isActive) {
                    pollForAndRelayBatchOfEvents()
                }
            }
        }
    }

    private suspend fun pollForAndRelayBatchOfEvents() = withContext(Dispatchers.IO) {
        try {
            val records = kafkaConsumer.poll(Duration.of(100, ChronoUnit.MILLIS))
            if (records.containsEvents()) {
                eventBatchProcessorService.processEvents(records)
                kafkaConsumer.commitSync()
            }
        } catch (re: RetriableKafkaException) {
            log.warn("Post mot Kafka feilet, prøver igjen senere. Topic: $topic", re)
            rollbackOffset()

        } catch (ure: UnretriableKafkaException) {
            log.warn("Alvorlig feil ved post mot kafka. Stopper polling. Topic: $topic", ure)
            stopPolling()

        } catch (rde: RetriableDatabaseException) {
            log.warn("Klarte ikke å skrive til databasen, prøver igjen senrere. Topic: $topic", rde)

        } catch (ude: UnretriableDatabaseException) {
            log.error("Det skjedde en alvorlig feil mot databasen, stopper videre polling. Topic: $topic", ude)
            stopPolling()

        } catch (re: RetriableException) {
            log.warn("Polling mot Kafka feilet, prøver igjen senere. Topic: $topic", re)

        } catch (ce: CancellationException) {
            log.info("Denne coroutine-en ble stoppet. ${ce.message}", ce)

        } catch (tae: TopicAuthorizationException) {
            log.warn("Pauser polling i ${timeToPauseInCaseOfPeriodicErrorsInMs}ms, er ikke autorisert for å lese: ${tae.unauthorizedTopics()}", tae)
            delay(timeToPauseInCaseOfPeriodicErrorsInMs)

        } catch (e: Exception) {
            log.error("Noe uventet feilet, stopper polling. Topic: $topic", e)
            stopPolling()
        }
    }

    fun ConsumerRecords<K, V>.containsEvents() = count() > 0

    private suspend fun rollbackOffset() {
        withContext(Dispatchers.IO) {
            kafkaConsumer.rollbackToLastCommitted()
        }
    }

    fun <K, V> KafkaConsumer<K, V>.rollbackToLastCommitted() {
        committed(assignment()).forEach { (partition, metadata) ->
            seek(partition, metadata.offset())
        }
    }

}
