package no.nav.personbruker.dittnav.varsel.bestiller.metrics

import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.config.Environment
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx.InfluxMetricsReporter
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx.SensuClient

fun buildEventMetricsProbe(environment: Environment, database: Database): EventMetricsProbe {
    val metricsReporter = resolveMetricsReporter(environment)
    val nameResolver = ProducerNameResolver(database)
    val nameScrubber = ProducerNameScrubber(nameResolver)
    return EventMetricsProbe(metricsReporter, nameScrubber)
}

private fun resolveMetricsReporter(environment: Environment): MetricsReporter {
    return if (environment.sensuHost == "" || environment.sensuHost == "stub") {
        StubMetricsReporter()
    } else {
        val sensuClient = SensuClient(environment.sensuHost, environment.sensuPort.toInt())
        InfluxMetricsReporter(sensuClient, environment)
    }
}
