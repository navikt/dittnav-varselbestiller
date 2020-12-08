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
        handleAllEventsFromKafka(session)

        if (session.getEksternvarslingEventsSeen() > 0) {
            handleSeenEksternvarslingEvents(session)
            handleProcessedEksternvarslingEvents(session)
            handleFailedEksternvarslingEvents(session)
            handleDuplicateEksternvarslingEventKeys(session)
            handleEventsProcessingTime(session, processingTime)
        }
    }

    private suspend fun handleSeenEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberEksternvarslingSeen = session.getEksternvarslingEventsSeen(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_SEEN, numberEksternvarslingSeen, eventTypeName, printableAlias)
            PrometheusMetricsCollector.registerEventsSeen(numberEksternvarslingSeen, eventTypeName, printableAlias)
        }
    }

    private suspend fun handleAllEventsFromKafka(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberOfAllEvents = session.getAllEventsFromKafka(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberOfAllEvents > 0) {
                reportMetrics(KAFKA_ALL_EVENTS, numberOfAllEvents, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerAllEventsFromKafka(numberOfAllEvents, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleProcessedEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberEksternvarslingProcessed = session.getEksternvarslingEventsProcessed(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberEksternvarslingProcessed > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_PROCESSED, numberEksternvarslingProcessed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsProcessed(numberEksternvarslingProcessed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleFailedEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberEksternvarslingFailed = session.getEksternvarslingEventsFailed(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberEksternvarslingFailed > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_FAILED, numberEksternvarslingFailed, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsFailed(numberEksternvarslingFailed, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleDuplicateEksternvarslingEventKeys(session: EventMetricsSession) {
        session.getUniqueSystemUser().forEach { systemUser ->
            val numberEksternvarslingDuplicateKeys = session.getEksternvarslingDuplicateKeys(systemUser)
            val eventTypeName = session.eventtype.toString()
            val printableAlias = nameScrubber.getPublicAlias(systemUser)

            if (numberEksternvarslingDuplicateKeys > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_DUPLICATE_KEY, numberEksternvarslingDuplicateKeys, eventTypeName, printableAlias)
                PrometheusMetricsCollector.registerEventsDuplicateKey(numberEksternvarslingDuplicateKeys, eventTypeName, printableAlias)
            }
        }
    }

    private suspend fun handleEventsProcessingTime(session: EventMetricsSession, processingTime: Long) {
        val metricsOverHead = session.timeElapsedSinceSessionStartNanos() - processingTime
        val fieldMap = listOf(
                "seen" to session.getEksternvarslingEventsSeen(),
                "processed" to session.getEksternvarslingEventsProcessed(),
                "failed" to session.getEksternvarslingEventsFailed(),
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