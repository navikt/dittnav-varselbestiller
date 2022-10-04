package no.nav.personbruker.dittnav.varselbestiller.varsel

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.metrics.influx.KAFKA_RAPID_EKSTERNVARSLING_EVENTS_PROCESSED

class RapidMetricsProbe(private val metricsReporter: MetricsReporter) {

    suspend fun countProcessed() {
        metricsReporter.registerDataPoint(KAFKA_RAPID_EKSTERNVARSLING_EVENTS_PROCESSED, counterField(), emptyMap())
    }

    private fun counterField(): Map<String, Int> = listOf("counter" to 1).toMap()
}
