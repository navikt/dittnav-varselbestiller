package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus.PrometheusMetricsCollector

class MetricsCollector(private val metricsReporter: MetricsReporter, private val nameScrubber: ProducerNameScrubber) {

    suspend fun recordMetrics(eventType: Eventtype, block: suspend EventMetricsSession.() -> Unit) {
        val session = EventMetricsSession(eventType)
        block.invoke(session)
        val processingTime = session.timeElapsedSinceSessionStartNanos()
        handleAllEvents(session)

        if (session.getEventsSeen() > 0) {
            handleEventsSeen(session)
            handleEventsProcessed(session)
            handleEventsFailed(session)
            handleDuplicateEventKeys(session)
            handleEventsProcessingTime(session, processingTime)
        }
    }

    private suspend fun handleEventsSeen(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberSeen = session.getEventsSeen(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            reportMetrics(KAFKA_EVENTS_SEEN, numberSeen, eventTypeName, printableAlias)
            PrometheusMetricsCollector.registerEventsSeen(numberSeen, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleAllEvents(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberOfAllEvents = session.getAllEvents(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberOfAllEvents > 0) {
                reportMetrics(KAFKA_ALL_EVENTS, numberOfAllEvents, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerAllEventsFromKafka(numberOfAllEvents, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleEventsProcessed(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberProcessed = session.getEventsProcessed(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberProcessed > 0) {
                reportMetrics(KAFKA_EVENTS_PROCESSED, numberProcessed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsProcessed(numberProcessed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleEventsFailed(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberFailed = session.getEventsFailed(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberFailed > 0) {
                reportMetrics(KAFKA_EVENTS_FAILED, numberFailed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsFailed(numberFailed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleDuplicateEventKeys(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberDuplicateKeyEvents = session.getDuplicateKeyEvents(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberDuplicateKeyEvents > 0) {
                reportMetrics(KAFKA_EVENTS_DUPLICATE_KEY, numberDuplicateKeyEvents, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsDuplicateKey(numberDuplicateKeyEvents, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleEventsProcessingTime(session: EventMetricsSession, processingTime: Long) {
        val metricsOverHead = session.timeElapsedSinceSessionStartNanos() - processingTime
        val fieldMap = listOf(
                "seen" to session.getEventsSeen(),
                "processed" to session.getEventsProcessed(),
                "failed" to session.getEventsFailed(),
                "processingTime" to processingTime,
                "metricsOverheadTime" to metricsOverHead
        ).toMap()

        val tagMap = listOf("eventType" to session.eventtype.toString()).toMap()

        metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, fieldMap, tagMap)
    }

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producerAlias: String) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producerAlias))
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producer: String): Map<String, String> =
            listOf("eventType" to eventType, "producer" to producer).toMap()
}