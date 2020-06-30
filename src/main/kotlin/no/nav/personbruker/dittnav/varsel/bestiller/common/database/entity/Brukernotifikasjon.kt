package no.nav.personbruker.dittnav.varsel.bestiller.common.database.entity

import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType

data class Brukernotifikasjon(
        val eventId: String,
        val systembruker: String,
        val type: EventType,
        val fodselsnummer: String
) {

}
