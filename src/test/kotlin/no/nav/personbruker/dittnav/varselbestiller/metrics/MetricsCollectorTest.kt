package no.nav.personbruker.dittnav.varselbestiller.metrics

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus.PrometheusMetricsCollector
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MetricsCollectorTest {

    private val metricsReporter = mockk<MetricsReporter>()
    private val producerNameResolver = mockk<ProducerNameResolver>()
    val producerName = "dummySystembruker"
    val producerAlias = "dittnav"

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
        mockkObject(PrometheusMetricsCollector)
    }

    @Test
    fun `should replace system name with alias for event processed`() {

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val producerNameForPrometheus = slot<String>()
        val capturedTags = slot<Map<String, String>>()

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit
        every { PrometheusMetricsCollector.registerSeenEksternvarslingEvents(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED_INTERN) {
                countSuccessfulEksternVarslingForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerSeenEksternvarslingEvents(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerProcessedEksternvarslingEvents(any(), any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should replace system name with alias for event failed`() {

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val capturedTags = slot<Map<String, String>>()
        val producerNameForPrometheus = slot<String>()

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit
        every { PrometheusMetricsCollector.registerFailedEksternvarslingEvents(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED_INTERN) {
                countFailedEksternvarslingForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerSeenEksternvarslingEvents(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerFailedEksternvarslingEvents(any(), any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should replace system name with alias for all events from kafka`() {

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val producerNameForPrometheus = slot<String>()
        val capturedTags = slot<Map<String, String>>()

        coEvery { metricsReporter.registerDataPoint(KAFKA_ALL_EVENTS, any(), capture(capturedTags)) } returns Unit
        every { PrometheusMetricsCollector.registerAllEventsFromKafka(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED_INTERN) {
                countAllEventsFromKafkaForProducer(producerName)
            }
        }

        coVerify(exactly = 1) { metricsReporter.registerDataPoint(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerAllEventsFromKafka(any(), any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should report correct number of events`() {
        coEvery { producerNameResolver.getProducerNameAlias(any()) } returns "test-user"

        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val capturedFieldsForSeen = slot<Map<String, Any>>()
        val capturedFieldsForProcessed = slot<Map<String, Any>>()
        val capturedFieldsForFailed = slot<Map<String, Any>>()
        val capturedFieldsForAllEvents = slot<Map<String, Any>>()

        coEvery { metricsReporter.registerDataPoint(KAFKA_EKSTERNVARSLING_EVENTS_SEEN, capture(capturedFieldsForSeen), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EKSTERNVARSLING_EVENTS_PROCESSED, capture(capturedFieldsForProcessed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EKSTERNVARSLING_EVENTS_FAILED, capture(capturedFieldsForFailed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_ALL_EVENTS, capture(capturedFieldsForAllEvents), any()) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED_INTERN) {
                countSuccessfulEksternVarslingForProducer("producer")
                countSuccessfulEksternVarslingForProducer("producer")
                countFailedEksternvarslingForProducer("producer")
                countAllEventsFromKafkaForProducer("producer")
            }
        }

        coVerify(exactly = 5) { metricsReporter.registerDataPoint(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerSeenEksternvarslingEvents(3, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerProcessedEksternvarslingEvents(2, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerFailedEksternvarslingEvents(1, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerAllEventsFromKafka(1, any(), any()) }

        capturedFieldsForSeen.captured["counter"] `should be equal to` 3
        capturedFieldsForProcessed.captured["counter"] `should be equal to` 2
        capturedFieldsForFailed.captured["counter"] `should be equal to` 1
        capturedFieldsForAllEvents.captured["counter"] `should be equal to` 1
    }

}
