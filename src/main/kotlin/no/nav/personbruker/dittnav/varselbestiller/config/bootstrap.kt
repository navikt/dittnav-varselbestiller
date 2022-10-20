package no.nav.personbruker.dittnav.varselbestiller.config

import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStopPreparing
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.routing.routing
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.polling.pollingApi
import no.nav.personbruker.dittnav.varselbestiller.health.healthApi
import no.nav.personbruker.dittnav.varselbestiller.varsel.DoneSink
import no.nav.personbruker.dittnav.varselbestiller.varsel.RapidMetricsProbe
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselSink
import kotlin.concurrent.thread

fun Application.mainModule(appContext: ApplicationContext = ApplicationContext()) {
    DefaultExports.initialize()
    install(DefaultHeaders)
    routing {
        healthApi(appContext.healthService)
        pollingApi(appContext)

        configureStartupHook(appContext)
        configureShutdownHook(appContext)
    }
}

private fun Application.configureStartupHook(appContext: ApplicationContext) {
    environment.monitor.subscribe(ApplicationStarted) {
        Flyway.runFlywayMigrations(appContext.environment)
        KafkaConsumerSetup.startAllKafkaPollers(appContext)
        appContext.periodicConsumerPollingCheck.start()

        if (appContext.environment.rapidEnabled) {
            thread {
                startRapid(appContext)
            }
        }
    }
}

private fun startRapid(appContext: ApplicationContext) {
    val rapidMetricsProbe = RapidMetricsProbe(appContext.resolveMetricsReporter(appContext.environment))
    RapidApplication.create(appContext.environment.rapidConfig() + mapOf("HTTP_PORT" to "8090")).apply {
        VarselSink(
            rapidsConnection = this,
            doknotifikasjonProducer = appContext.doknotifikasjonProducer,
            varselbestillingRepository = appContext.doknotifikasjonRepository,
            rapidMetricsProbe = rapidMetricsProbe,
            writeToDb = appContext.environment.rapidWriteToDb
        )
        DoneSink(
            rapidsConnection = this,
            doknotifikasjonStoppProducer = appContext.doknotifikasjonStopProducer,
            varselbestillingRepository = appContext.doknotifikasjonRepository,
            rapidMetricsProbe = rapidMetricsProbe,
            writeToDb = appContext.environment.rapidWriteToDb
        )
    }.start()
}

private fun Application.configureShutdownHook(appContext: ApplicationContext) {
    environment.monitor.subscribe(ApplicationStopPreparing) {
        runBlocking {
            KafkaConsumerSetup.stopAllKafkaConsumers(appContext)
            appContext.periodicConsumerPollingCheck.stop()
            appContext.doknotifikasjonBeskjedProducer.flushAndClose()
            appContext.doknotifikasjonOppgaveProducer.flushAndClose()
            appContext.doknotifikasjonInnboksProducer.flushAndClose()
            appContext.doknotifikasjonStopProducer.flushAndClose()
        }
        appContext.database.dataSource.close()
    }
}
