package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

data class EarlyCancellation(val eventId: String, val appnavn: String) {
    companion object {
        fun fromEventEntryMap(entry: Map.Entry<NokkelIntern, DoneIntern>): EarlyCancellation {
            return EarlyCancellation(entry.key.getEventId(), entry.key.getAppnavn())
        }
    }
}
