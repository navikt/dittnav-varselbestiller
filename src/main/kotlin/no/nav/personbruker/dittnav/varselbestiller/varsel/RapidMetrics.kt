package no.nav.personbruker.dittnav.varselbestiller.varsel

import io.prometheus.client.Counter

object RapidMetrics {
    private const val METRIC_NAMESPACE = "tms_varselbestiller"

    private const val BESTILLING_OPPRETTET_NAME = "bestilling_opprettet"
    private const val BESTILLING_STOPPET_NAME = "bestilling_stoppet"
    private const val DUPLIKAT_BESTILLING_NAME = "duplikat_bestilling"

    private val BESTILLING_OPPRETTET: Counter = Counter.build()
        .name(BESTILLING_OPPRETTET_NAME)
        .namespace(METRIC_NAMESPACE)
        .help("Antall eksterne varsler bestilt")
        .labelNames("type", "kanaler")
        .register()

    private val BESTILLING_STOPPET: Counter = Counter.build()
        .name(BESTILLING_STOPPET_NAME)
        .namespace(METRIC_NAMESPACE)
        .help("Antall bestillinger av ekstern varsling stoppet")
        .labelNames("type")
        .register()

    private val DUPLIKAT_BESTILLING: Counter = Counter.build()
        .name(DUPLIKAT_BESTILLING_NAME)
        .namespace(METRIC_NAMESPACE)
        .help("Antall duplikate varsler mottatt")
        .labelNames("type")
        .register()

    fun eksternVarslingBestilt(varselType: VarselType, prefererteKanaler: List<String>) {
        BESTILLING_OPPRETTET
            .labels(varselType.lowercaseName(), prefererteKanaler.metricString())
            .inc()
    }

    fun eksternVarslingStoppet(varselType: String) {
        BESTILLING_STOPPET
            .labels(varselType)
            .inc()
    }

    fun duplikat(varselType: VarselType) {
        DUPLIKAT_BESTILLING
            .labels(varselType.lowercaseName())
            .inc()
    }
}

private fun List<String>.metricString(): String = when {
    this == listOf("SMS") -> "SMS"
    this == listOf("EPOST") -> "EPOST"
    else -> sorted().joinToString("_")
}
