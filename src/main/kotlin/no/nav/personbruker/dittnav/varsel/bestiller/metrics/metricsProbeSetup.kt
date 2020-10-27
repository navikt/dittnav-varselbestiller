package no.nav.personbruker.dittnav.varsel.bestiller.metrics

import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influx.InfluxMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influx.SensuConfig
import no.nav.personbruker.dittnav.common.metrics.masking.ProducerNameScrubber
import no.nav.personbruker.dittnav.common.metrics.masking.PublicAliasResolver
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.config.Environment
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.db.getProdusentnavn

fun buildEventMetricsProbe(environment: Environment, database: Database): EventMetricsProbe {
    val metricsReporter = resolveMetricsReporter(environment)
    val nameResolver = PublicAliasResolver({ database.queryWithExceptionTranslation { getProdusentnavn() } })
    val nameScrubber = ProducerNameScrubber(nameResolver)
    return EventMetricsProbe(metricsReporter, nameScrubber)
}

private fun resolveMetricsReporter(environment: Environment): MetricsReporter {
    return if (environment.sensuHost == "" || environment.sensuHost == "stub") {
        StubMetricsReporter()
    } else {
        val sensuConfig = SensuConfig(
                applicationName = environment.applicationName,
                hostName = environment.sensuHost,
                hostPort = environment.sensuPort.toInt(),
                clusterName = environment.clusterName,
                namespace = environment.namespace,
                eventsTopLevelName = "dittnav-varselbestiller",
                enableEventBatching = environment.sensuBatchingEnabled,
                eventBatchesPerSecond = environment.sensuBatchesPerSecond)
        InfluxMetricsReporter(sensuConfig)
    }
}
