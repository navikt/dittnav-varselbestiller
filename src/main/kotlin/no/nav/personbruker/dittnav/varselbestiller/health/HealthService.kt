package no.nav.personbruker.dittnav.varselbestiller.health

import no.nav.personbruker.dittnav.varselbestiller.config.ApplicationContext

class HealthService(private val applicationContext: ApplicationContext) {

    suspend fun getHealthChecks(): List<HealthStatus> {
        return listOf(
                applicationContext.database.status(),
                applicationContext.beskjedConsumer.status(),
                applicationContext.oppgaveConsumer.status(),
                applicationContext.doneConsumer.status(),
                applicationContext.periodicConsumerPollingCheck.status()
        )
    }
}
