package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus.PrometheusMetricsCollector

class MetricsCollector(private val metricsReporter: MetricsReporter) {

    suspend fun recordMetrics(eventType: Eventtype, block: suspend EventMetricsSession.() -> Unit) {
        val session = EventMetricsSession(eventType)
        block.invoke(session)
        processSession(session)
    }

    fun createSession(eventType: Eventtype): EventMetricsSession {
        return EventMetricsSession(eventType)
    }

    suspend fun processSession(session: EventMetricsSession) {
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
            val numberEksternvarslingSeen = session.getEksternvarslingEventsSeen(producer)
            val eventTypeName = session.eventtype.toString()

            reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_SEEN, numberEksternvarslingSeen, eventTypeName, producer.namespace, producer.appnavn)
            PrometheusMetricsCollector.registerSeenEksternvarslingEvents(numberEksternvarslingSeen, eventTypeName, producer.appnavn)
        }
    }

    private suspend fun handleAllEventsFromKafka(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberOfAllEvents = session.getAllEventsFromKafka(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberOfAllEvents > 0) {
                reportMetrics(KAFKA_ALL_EVENTS, numberOfAllEvents, eventTypeName, producer.namespace, producer.appnavn)
                PrometheusMetricsCollector.registerAllEventsFromKafka(numberOfAllEvents, eventTypeName, producer.appnavn)
            }
        }
    }

    private suspend fun handleProcessedEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingProcessed = session.getEksternvarslingEventsProcessed(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberEksternvarslingProcessed > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_PROCESSED, numberEksternvarslingProcessed, eventTypeName, producer.namespace, producer.appnavn)
                PrometheusMetricsCollector.registerProcessedEksternvarslingEvents(numberEksternvarslingProcessed, eventTypeName, producer.appnavn)
            }
        }
    }

    private suspend fun handleFailedEksternvarslingEvents(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingFailed = session.getEksternvarslingEventsFailed(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberEksternvarslingFailed > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_FAILED, numberEksternvarslingFailed, eventTypeName, producer.namespace, producer.appnavn)
                PrometheusMetricsCollector.registerFailedEksternvarslingEvents(numberEksternvarslingFailed, eventTypeName, producer.appnavn)
            }
        }
    }

    private suspend fun handleDuplicateEksternvarslingEventKeys(session: EventMetricsSession) {
        session.getUniqueProducers().forEach { producer ->
            val numberEksternvarslingDuplicateKeys = session.getEksternvarslingDuplicateKeys(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberEksternvarslingDuplicateKeys > 0) {
                reportMetrics(KAFKA_EKSTERNVARSLING_EVENTS_DUPLICATE_KEY, numberEksternvarslingDuplicateKeys, eventTypeName, producer.namespace, producer.appnavn)
                PrometheusMetricsCollector.registerDuplicateKeyEksternvarslingEvents(numberEksternvarslingDuplicateKeys, eventTypeName, producer.appnavn)
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

    private suspend fun reportMetrics(metricName: String, count: Int, eventType: String, producerNamespace: String, producerName: String) {
        metricsReporter.registerDataPoint(metricName, createCounterField(count), createTagMap(eventType, producerNamespace, producerName))
    }

    private fun createCounterField(events: Int): Map<String, Int> = listOf("counter" to events).toMap()

    private fun createTagMap(eventType: String, producerNamespace: String, producer: String): Map<String, String> =
            listOf("eventType" to eventType, "producerNamespace" to producerNamespace, "producer" to producer).toMap()
}
