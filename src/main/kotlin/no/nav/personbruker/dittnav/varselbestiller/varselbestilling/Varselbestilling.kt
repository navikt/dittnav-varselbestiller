package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import java.time.LocalDateTime

data class Varselbestilling(
        val bestillingsId: String,
        val eventId: String,
        val fodselsnummer: String,
        val systembruker: String,
        val bestillingstidspunkt: LocalDateTime,
        val avbestilt: Boolean = false
) {

    override fun toString(): String {
        return "Varselbestilling(" +
                "bestillingsId=$bestillingsId, " +
                "eventId=$eventId, " +
                "fodselsnummer=***, " +
                "systembruker=***, " +
                "bestillingstidspunkt=$bestillingstidspunkt, " +
                "avbestilt=$avbestilt"
    }
}


