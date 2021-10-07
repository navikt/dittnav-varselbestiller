package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus.PrometheusMetricsCollector

class MetricsCollector(private val metricsReporter: MetricsReporter) {

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
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingSeen = session.getEksternvarslingEventsSeen(producer.namespace, producer.appnavn)
            val eventTypeName = session.eventtype.toString()

            reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_SEEN, numberEksternvarslingSeen, eventTypeName, producer)
            PrometheusMetricsCollector.registerSeenEksternvarslingEvents(numberEksternvarslingSeen, eventTypeName, producer)
        }
    }

    private suspend fun handleAllEventsFromKafka(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberOfAllEvents = session.getAllEventsFromKafka(producer.namespace, producer.appnavn)
            val eventTypeName = session.eventtype.toString()

            if (numberOfAllEvents > 0) {
                reportMetrics(KAFKA_ALL_EVENTS, numberOfAllEvents, eventTypeName, producer)
                PrometheusMetricsCollector.registerAllEventsFromKafka(numberOfAllEvents, eventTypeName, producer)
            }
        }
    }

    private suspend fun handleProcessedEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingProcessed = session.getEksternvarslingEventsProcessed(producer.namespace, producer.appnavn)
            val eventTypeName = session.eventtype.toString()

            if (numberEksternvarslingProcessed > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_PROCESSED, numberEksternvarslingProcessed, eventTypeName, producer)
                PrometheusMetricsCollector.registerProcessedEksternvarslingEvents(numberEksternvarslingProcessed, eventTypeName, producer)
            }
        }
    }

    private suspend fun handleFailedEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingFailed = session.getEksternvarslingEventsFailed(producer.namespace, producer.appnavn)
            val eventTypeName = session.eventtype.toString()

            if (numberEksternvarslingFailed > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_FAILED, numberEksternvarslingFailed, eventTypeName, producer)
                PrometheusMetricsCollector.registerFailedEksternvarslingEvents(numberEksternvarslingFailed, eventTypeName, producer)
            }
        }
    }

    private suspend fun handleDuplicateEksternvarslingEventKeys(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingDuplicateKeys = session.getEksternvarslingDuplicateKeys(producer.namespace, producer.appnavn)
            val eventTypeName = session.eventtype.toString()

            if (numberEksternvarslingDuplicateKeys > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_DUPLICATE_KEY, numberEksternvarslingDuplicateKeys, eventTypeName, producer)
                PrometheusMetricsCollector.registerDuplicateKeyEksternvarslingEvents(numberEksternvarslingDuplicateKeys, eventTypeName, producer)
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

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producer: Producer) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producer.getProducerKey()))
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producer: String): Map<String, String> =
            listOf("eventType" to eventType, "producer" to producer).toMap()
}
