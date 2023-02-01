package no.nav.personbruker.dittnav.varselbestiller.varsel

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter

private const val METRIC_NAMESPACE = "dittnav.varselbestiller.v1"

class RapidMetricsProbe(private val metricsReporter: MetricsReporter) {

    suspend fun countDoknotifikasjonProduced(varselType: VarselType, kanaler: List<String>) {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.doknotifikasjon.produced",
            fields = counterField(),
            tags = mapOf("varselType" to varselType.toString(), "kanaler" to kanaler.metricString())
        )
    }

    suspend fun countDoknotifikasjonStoppProduced(eventName: String) {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.doknotifikasjonstopp.produced",
            fields = counterField(),
            tags = mapOf("eventName" to eventName)
        )
    }

    suspend fun countDuplicates(varselType: VarselType) {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.rapid.duplicates",
            fields = counterField(),
            tags = mapOf("varselType" to varselType.toString())
        )
    }

    private fun counterField(): Map<String, Int> = listOf("counter" to 1).toMap()

}

private fun List<String>.metricString(): String = when {
    toSet() == setOf("SMS", "EPOST") -> "BEGGE"
    this == listOf("SMS") -> "SMS"
    this == listOf("EPOST") -> "EPOST"
    else -> joinToString(",")
}
