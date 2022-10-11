package no.nav.personbruker.dittnav.varselbestiller.metrics.influx

private const val METRIC_NAMESPACE = "dittnav.varselbestiller.v1"

const val KAFKA_EKSTERNVARSLING_EVENTS_PROCESSED = "$METRIC_NAMESPACE.processed"
const val KAFKA_EKSTERNVARSLING_EVENTS_SEEN = "$METRIC_NAMESPACE.seen"
const val KAFKA_EKSTERNVARSLING_EVENTS_FAILED = "$METRIC_NAMESPACE.failed"
const val KAFKA_EKSTERNVARSLING_EVENTS_DUPLICATE_KEY = "$METRIC_NAMESPACE.duplicateKey"
const val KAFKA_EVENTS_PROCESSING_TIME = "$METRIC_NAMESPACE.processingTime"
const val KAFKA_ALL_EVENTS = "$METRIC_NAMESPACE.allEvents"
const val BESTILLINGSID_DISCREPANCY = "$METRIC_NAMESPACE.bestillingsIdDiscrepancy"