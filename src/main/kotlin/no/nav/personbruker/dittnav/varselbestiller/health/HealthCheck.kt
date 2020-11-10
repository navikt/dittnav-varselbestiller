package no.nav.personbruker.dittnav.varselbestiller.health

interface HealthCheck {

    suspend fun status(): HealthStatus

}
