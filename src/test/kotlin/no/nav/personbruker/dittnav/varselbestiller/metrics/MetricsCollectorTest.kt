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

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
        mockkObject(PrometheusMetricsCollector)
    }

    @Test
    fun `should use producer name for events processed`() {
        val producerName = "appnavn"

        val metricsCollector = MetricsCollector(metricsReporter)

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

        producerNameForPrometheus.captured `should be equal to` producerName
        capturedTags.captured["producer"] `should be equal to` producerName
    }

    @Test
    fun `should use producer name for events failed`() {
        val producerName = "appnavn"

        val metricsCollector = MetricsCollector(metricsReporter)

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

        producerNameForPrometheus.captured `should be equal to` producerName
        capturedTags.captured["producer"] `should be equal to` producerName
    }

    @Test
    fun `should report correct number of events`() {
        val metricsCollector = MetricsCollector(metricsReporter)

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
