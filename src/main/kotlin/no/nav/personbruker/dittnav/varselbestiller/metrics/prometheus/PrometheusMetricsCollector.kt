package no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import no.nav.personbruker.dittnav.varselbestiller.metrics.Producer

object PrometheusMetricsCollector {

    const val NAMESPACE = "dittnav_varselbestiller_consumer"

    const val SEEN_EKSTERNVARSLING_EVENTS_NAME = "kafka_events_seen"
    const val PROCESSED_EKSTERNVARSLING_EVENTS_NAME = "kafka_events_processed"
    const val LAST_SEEN_EKSTERNVARSLING_EVENTS_NAME = "kafka_event_type_last_seen"
    const val FAILED_EKSTERNVARSLING_EVENTS_NAME = "kafka_events_failed"
    const val DUPLICATE_KEY_EKSTERNVARSLING_EVENTS_NAME = "kafka_events_duplicate_key"
    const val ALL_EVENTS_NAME = "kafka_all_events"


    private val MESSAGES_SEEN_EKSTERNVARSLING: Counter = Counter.build()
            .name(SEEN_EKSTERNVARSLING_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events read since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGE_LAST_SEEN_EKSTERNVARSLING: Gauge = Gauge.build()
            .name(LAST_SEEN_EKSTERNVARSLING_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Last time event type was seen")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_ALL_EVENTS: Gauge = Gauge.build()
            .name(ALL_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("All events read since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_PROCESSED_EKSTERNVARSLING: Counter = Counter.build()
            .name(PROCESSED_EKSTERNVARSLING_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events successfully processed since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_FAILED_EKSTERNVARSLING: Counter = Counter.build()
            .name(FAILED_EKSTERNVARSLING_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events failed since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_DUPLICATE_KEY_EKSTERNVARSLING: Counter = Counter.build()
            .name(DUPLICATE_KEY_EKSTERNVARSLING_EVENTS_NAME)
            .namespace(NAMESPACE)
            .help("Events skipped due to duplicate keys since last startup")
            .labelNames("type", "producer")
            .register()


    fun registerProcessedEksternvarslingEvents(count: Int, topic: String, producer: String) {
        MESSAGES_PROCESSED_EKSTERNVARSLING.labels(topic, producer).inc(count.toDouble())
    }

    fun registerSeenEksternvarslingEvents(count: Int, eventType: String, producer: String) {
        MESSAGES_SEEN_EKSTERNVARSLING.labels(eventType, producer).inc(count.toDouble())
        MESSAGE_LAST_SEEN_EKSTERNVARSLING.labels(eventType, producer).setToCurrentTime()
    }

    fun registerAllEventsFromKafka(count: Int, eventType: String, producer: String) {
        MESSAGES_ALL_EVENTS.labels(eventType, producer).inc(count.toDouble())
    }

    fun registerFailedEksternvarslingEvents(count: Int, topic: String, producer: String) {
        MESSAGES_FAILED_EKSTERNVARSLING.labels(topic, producer).inc(count.toDouble())
    }

    fun registerDuplicateKeyEksternvarslingEvents(count: Int, topic: String, producer: String) {
        MESSAGES_DUPLICATE_KEY_EKSTERNVARSLING.labels(topic, producer).inc(count.toDouble())
    }

}
