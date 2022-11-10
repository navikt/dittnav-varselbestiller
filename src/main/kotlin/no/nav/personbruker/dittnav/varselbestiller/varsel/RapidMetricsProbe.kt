package no.nav.personbruker.dittnav.varselbestiller.varsel

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter

private const val METRIC_NAMESPACE = "dittnav.varselbestiller.v1"

class RapidMetricsProbe(private val metricsReporter: MetricsReporter) {

    suspend fun countDoknotifikasjonProduced(varselType: VarselType) {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.doknotifikasjon.produced",
            fields = counterField(),
            tags = mapOf("varselType" to varselType.toString())
        )
    }

    suspend fun countDoknotifikasjonStoppProduced() {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.doknotifikasjonstopp.produced",
            fields = counterField(),
            tags = emptyMap()
        )
    }

    suspend fun countDuplicates(varselType: VarselType) {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.rapid.duplicates",
            fields = counterField(),
            tags = mapOf("varselType" to varselType.toString())
        )
    }
    suspend fun countReadVarselInaktivertEvents() {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.varselinaktivert.read",
            fields = counterField(),
            tags = emptyMap()
        )
    }

    suspend fun countProducedForVarslerInaktivert() {
        metricsReporter.registerDataPoint(
            measurementName = "$METRIC_NAMESPACE.varselinaktivert.produced",
            fields = counterField(),
            tags = emptyMap()
        )
    }


    private fun counterField(): Map<String, Int> = listOf("counter" to 1).toMap()

}
