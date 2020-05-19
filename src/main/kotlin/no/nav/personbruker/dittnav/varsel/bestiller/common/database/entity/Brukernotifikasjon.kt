package no.nav.personbruker.dittnav.varsel.bestiller.common.database.entity

import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType
import no.nav.personbruker.dittnav.varsel.bestiller.done.Done

data class Brukernotifikasjon(
        val eventId: String,
        val systembruker: String,
        val type: EventType,
        val fodselsnummer: String
) {

    fun isRepresentsSameEventAs(doneEvent: Done): Boolean {
        if (eventId != doneEvent.eventId) return false
        if (fodselsnummer != doneEvent.fodselsnummer) return false
        if (systembruker != doneEvent.systembruker) return false

        return true
    }

}
