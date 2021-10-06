package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import java.time.LocalDateTime

data class Varselbestilling(
        val bestillingsId: String,
        val eventId: String,
        val fodselsnummer: String,
        val appnavn: String,
        val bestillingstidspunkt: LocalDateTime,
        val prefererteKanaler: List<String>,
        val avbestilt: Boolean = false
)
