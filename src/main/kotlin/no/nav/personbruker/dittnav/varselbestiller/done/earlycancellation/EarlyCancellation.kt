package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
                LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
            )
        }
    }

    override fun toString(): String {
        return """EarlyCancellation(
            |eventId=$eventId, 
            |appnavn=$appnavn, 
            |namespace=$namespace, 
            |fodselsnummer=****, 
            |systembruker=$systembruker, 
            |tidspunkt=$tidspunkt
            |)""".trimMargin()
    }
}
