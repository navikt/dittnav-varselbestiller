package no.nav.personbruker.dittnav.varselbestiller.metrics

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.config.EventType
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_FAILED
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_PROCESSED
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_SEEN
import no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus.PrometheusMetricsCollector
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MetricsCollectorTest {

    private val metricsReporter = mockk<MetricsReporter>()
    private val producerNameResolver = mockk<ProducerNameResolver>()
    val producerName = "x-dittnav"
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

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_FAILED), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_FAILED, any(), any()) } returns Unit
        every { PrometheusMetricsCollector.registerEventsSeen(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(EventType.BESKJED) {
                countSuccessfulEventForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_FAILED), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsSeen(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsProcessed(any(), any(), any()) }

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

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSED), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSED, any(), any()) } returns Unit
        every { PrometheusMetricsCollector.registerEventsFailed(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(EventType.BESKJED) {
                countFailedEventForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSED), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsSeen(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsFailed(any(), any(), any()) }

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

        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_SEEN, capture(capturedFieldsForSeen), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSED, capture(capturedFieldsForProcessed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_FAILED, capture(capturedFieldsForFailed), any()) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(EventType.BESKJED) {
                countSuccessfulEventForProducer("producer")
                countSuccessfulEventForProducer("producer")
                countFailedEventForProducer("producer")
            }
        }

        coVerify(exactly = 3) { metricsReporter.registerDataPoint(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsSeen(3, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsProcessed(2, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsFailed(1, any(), any()) }

        capturedFieldsForSeen.captured["counter"] `should be equal to` 3
        capturedFieldsForProcessed.captured["counter"] `should be equal to` 2
        capturedFieldsForFailed.captured["counter"] `should be equal to` 1
    }

}