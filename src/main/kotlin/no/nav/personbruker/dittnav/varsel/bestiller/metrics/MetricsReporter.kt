package no.nav.personbruker.dittnav.varsel.bestiller.metrics

interface MetricsReporter {
    suspend fun registerDataPoint(measurement: String, fields: Map<String, Any>, tags: Map<String, String>)
}