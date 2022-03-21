package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import java.time.LocalDateTime

data class EarlyCancellation(
    val eventId: String,
    val appnavn: String,
    val namespace: String,
    val fodselsnummer: String,
    val systembruker: String,
    val tidspunkt: LocalDateTime,
) {
    companion object {
        fun fromEventEntryMap(entry: Map.Entry<NokkelIntern, DoneIntern>): EarlyCancellation {
            return EarlyCancellation(
                entry.key.getEventId(),
                entry.key.getAppnavn(),
                entry.key.getNamespace(),
                entry.key.getFodselsnummer(),
                entry.key.getSystembruker(),
                LocalDateTime.now()
            )
        }
    }
}
