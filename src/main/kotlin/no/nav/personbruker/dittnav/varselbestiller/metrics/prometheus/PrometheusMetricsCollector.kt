package no.nav.personbruker.dittnav.varselbestiller.metrics.prometheus

import io.prometheus.client.Counter
import io.prometheus.client.Gauge

object PrometheusMetricsCollector {

    const val NAMESPACE = "dittnav_varselbestiller_consumer"

    const val EVENTS_SEEN_NAME = "kafka_events_seen"
    const val EVENTS_PROCESSED_NAME = "kafka_events_processed"
    const val EVENT_LAST_SEEN_NAME = "kafka_event_type_last_seen"
    const val EVENTS_FAILED_NAME = "kafka_events_failed"
    const val EVENTS_DUPLICATE_KEY_NAME = "kafka_events_duplicate_key"
    const val ALL_EVENTS_NAME = "kafka_all_events"


    private val MESSAGES_SEEN: Counter = Counter.build()
            .name(EVENTS_SEEN_NAME)
            .namespace(NAMESPACE)
            .help("Events read since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGE_LAST_SEEN: Gauge = Gauge.build()
            .name(EVENT_LAST_SEEN_NAME)
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

    private val MESSAGES_PROCESSED: Counter = Counter.build()
            .name(EVENTS_PROCESSED_NAME)
            .namespace(NAMESPACE)
            .help("Events successfully processed since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_FAILED: Counter = Counter.build()
            .name(EVENTS_FAILED_NAME)
            .namespace(NAMESPACE)
            .help("Events failed since last startup")
            .labelNames("type", "producer")
            .register()

    private val MESSAGES_DUPLICATE_KEY: Counter = Counter.build()
            .name(EVENTS_DUPLICATE_KEY_NAME)
            .namespace(NAMESPACE)
            .help("Events skipped due to duplicate keys since last startup")
            .labelNames("type", "producer")
            .register()


    fun registerEventsProcessed(count: Int, topic: String, producer: String) {
        MESSAGES_PROCESSED.labels(topic, producer).inc(count.toDouble())
    }

    fun registerEventsSeen(count: Int, eventType: String, producer: String) {
        MESSAGES_SEEN.labels(eventType, producer).inc(count.toDouble())
        MESSAGE_LAST_SEEN.labels(eventType, producer).setToCurrentTime()
    }

    fun registerAllEventsFromKafka(count: Int, eventType: String, producer: String) {
        MESSAGES_ALL_EVENTS.labels(eventType, producer).inc(count.toDouble())
    }

    fun registerEventsFailed(count: Int, topic: String, producer: String) {
        MESSAGES_FAILED.labels(topic, producer).inc(count.toDouble())
    }

    fun registerEventsDuplicateKey(count: Int, topic: String, producer: String) {
        MESSAGES_DUPLICATE_KEY.labels(topic, producer).inc(count.toDouble())
    }

}