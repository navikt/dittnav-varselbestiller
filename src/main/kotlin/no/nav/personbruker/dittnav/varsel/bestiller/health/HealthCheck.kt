package no.nav.personbruker.dittnav.varsel.bestiller.health

interface HealthCheck {

    suspend fun status(): HealthStatus

}
