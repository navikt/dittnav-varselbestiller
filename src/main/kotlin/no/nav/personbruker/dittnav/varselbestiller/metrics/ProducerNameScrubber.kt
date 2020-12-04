package no.nav.personbruker.dittnav.varselbestiller.metrics

class ProducerNameScrubber(private val producerNameResolver: ProducerNameResolver) {

    val UNKNOWN_USER = "unknown-user"
    val GENERIC_SYSTEM_USER = "unmapped-system-user"

    suspend fun getPublicAlias(systembruker: String): String {
        var alias = producerNameResolver.getProducerNameAlias(systembruker)

        if (alias.isNullOrBlank()) {
            alias = findFallBackAlias(systembruker)
        }
        return alias
    }

    private fun findFallBackAlias(systembruker: String): String {
        return if (isSystemUser(systembruker)) {
            GENERIC_SYSTEM_USER
        } else {
            UNKNOWN_USER
        }
    }

    private fun isSystemUser(producer: String): Boolean {
        return "^srv.{1,12}\$".toRegex().matches(producer)
    }
}
