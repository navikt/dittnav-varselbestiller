package no.nav.personbruker.dittnav.varsel.bestiller.config

import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStopPreparing
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.routing.routing
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.health.healthApi
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.kafka.pollingApi

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
        KafkaConsumerSetup.startAllKafkaPollers(appContext)
    }
}

private fun Application.configureShutdownHook(appContext: ApplicationContext) {
    environment.monitor.subscribe(ApplicationStopPreparing) {
        runBlocking {
            KafkaConsumerSetup.stopAllKafkaConsumers(appContext)
        }
        appContext.database.dataSource.close()
    }
}
