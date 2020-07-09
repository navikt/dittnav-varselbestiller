package no.nav.personbruker.dittnav.varsel.bestiller.health

import no.nav.personbruker.dittnav.varsel.bestiller.config.ApplicationContext

class HealthService(private val applicationContext: ApplicationContext) {

    suspend fun getHealthChecks(): List<HealthStatus> {
        return listOf(
                applicationContext.database.status(),
                applicationContext.beskjedConsumer.status(),
                applicationContext.oppgaveConsumer.status(),
                applicationContext.doneConsumer.status()
        )
    }
}
