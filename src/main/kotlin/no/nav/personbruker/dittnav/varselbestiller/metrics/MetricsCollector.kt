package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_DUPLICATE_KEY
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_FAILED
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_PROCESSED
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_EVENTS_SEEN
import no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus.PrometheusMetricsCollector

class MetricsCollector(private val metricsReporter: MetricsReporter, private val nameScrubber: ProducerNameScrubber) {

    suspend fun recordMetrics(eventType: Eventtype, block: suspend EventMetricsSession.() -> Unit) {
        val session = EventMetricsSession(eventType)
        block.invoke(session)

        if (session.getEventsSeen() > 0) {
            handleEventsProcessed(session)
            handleEventsSeen(session)
            handleEventsFailed(session)
            handleDuplicateEventKeys(session)
        }
    }

    private suspend fun handleEventsProcessed(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemuser ->
            val numberProcessed = session.getEventsProcessed(systemuser)
            val eventTypeName = session.eventType.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemuser)

            reportMetrics(KAFKA_EVENTS_PROCESSED, numberProcessed, eventTypeName, printableAlias)
            PrometheusMetricsCollector.registerEventsProcessed(numberProcessed, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleEventsSeen(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemuser ->
            val numberSeen = session.getEventsSeen(systemuser)
            val eventTypeName = session.eventType.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemuser)

            reportMetrics(KAFKA_EVENTS_SEEN, numberSeen, eventTypeName, printableAlias)
            PrometheusMetricsCollector.registerEventsSeen(numberSeen, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleEventsFailed(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemuser ->
            val numberFailed = session.getEventsFailed(systemuser)
            val eventTypeName = session.eventType.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemuser)

            if (numberFailed > 0) {
                reportMetrics(KAFKA_EVENTS_FAILED, numberFailed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsFailed(numberFailed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleDuplicateEventKeys(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberDuplicateKeyEvents = session.getDuplicateKeyEvents(systemUser)
            val eventTypeName = session.eventType.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberDuplicateKeyEvents > 0) {
                reportMetrics(KAFKA_EVENTS_DUPLICATE_KEY, numberDuplicateKeyEvents, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsDuplicateKey(numberDuplicateKeyEvents, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producerAlias: String) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producerAlias))
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producer: String): Map<String, String> =
            listOf("eventType" to eventType, "producer" to producer).toMap()
}