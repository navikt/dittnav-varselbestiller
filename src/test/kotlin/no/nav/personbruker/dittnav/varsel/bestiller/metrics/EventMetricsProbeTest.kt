package no.nav.personbruker.dittnav.varsel.bestiller.metrics

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx.EVENTS_BATCH
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx.EVENTS_FAILED
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx.EVENTS_PROCESSED
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx.EVENTS_SEEN
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventMetricsProbeTest {

    private val metricsReporter = mockk<MetricsReporter>()
    private val prometheusCollector = mockkObject(PrometheusMetricsCollector)
    private val producerNameResolver = mockk<ProducerNameResolver>()

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun shouldReplaceSystemNameWithAliasForEventProcessed() {
        val producerName = "sys-t-user"
        val producerAlias = "test-user"

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsProbe = EventMetricsProbe(metricsReporter, nameScrubber)

        val producerNameForPrometheus = slot<String>()
        val capturedTags = slot<Map<String, String>>()

        coEvery { metricsReporter.registerDataPoint(not(EVENTS_BATCH), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(EVENTS_BATCH, any(), any()) } returns Unit
        every { PrometheusMetricsCollector.registerEventsSeen(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsProbe.runWithMetrics(EventType.BESKJED) {
                countSuccessfulEventForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(EVENTS_BATCH), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsSeen(any() , any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsProcessed(any() , any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun shouldReplaceSystemNameWithAliasForEventFailed() {
        val producerName = "sys-t-user"
        val producerAlias = "test-user"

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsProbe = EventMetricsProbe(metricsReporter, nameScrubber)

        val capturedTags = slot<Map<String, String>>()
        val producerNameForPrometheus = slot<String>()

        coEvery { metricsReporter.registerDataPoint(not(EVENTS_BATCH), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(EVENTS_BATCH, any(), any())} returns Unit
        every { PrometheusMetricsCollector.registerEventsFailed(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
                metricsProbe.runWithMetrics(EventType.BESKJED) {
                countFailedEventForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(EVENTS_BATCH), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsSeen(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsFailed(any(), any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun shouldReportCorrectNumberOfEvents() {
        coEvery { producerNameResolver.getProducerNameAlias(any()) } returns "test-user"
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsProbe = EventMetricsProbe(metricsReporter, nameScrubber)

        val capturedFieldsForSeen = slot<Map<String, Any>>()
        val capturedFieldsForProcessed = slot<Map<String, Any>>()
        val capturedFieldsForFailed = slot<Map<String, Any>>()

        coEvery { metricsReporter.registerDataPoint(EVENTS_SEEN, capture(capturedFieldsForSeen), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(EVENTS_PROCESSED, capture(capturedFieldsForProcessed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(EVENTS_FAILED, capture(capturedFieldsForFailed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(EVENTS_BATCH, any(), any())} returns Unit

        runBlocking {
            metricsProbe.runWithMetrics(EventType.BESKJED) {
                countSuccessfulEventForProducer("producer")
                countSuccessfulEventForProducer("producer")
                countFailedEventForProducer("producer")
            }
        }

        coVerify(exactly = 4) { metricsReporter.registerDataPoint(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsSeen(3, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsProcessed(2, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerEventsFailed(1, any(), any()) }

        capturedFieldsForSeen.captured["counter"] `should be equal to` 3
        capturedFieldsForProcessed.captured["counter"] `should be equal to` 2
        capturedFieldsForFailed.captured["counter"] `should be equal to` 1
    }
}
