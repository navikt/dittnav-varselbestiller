package no.nav.personbruker.dittnav.varsel.bestiller.config

object ConfigUtil {

    fun isCurrentlyRunningOnNais(): Boolean {
        return System.getenv("NAIS_APP_NAME") != null
    }

}