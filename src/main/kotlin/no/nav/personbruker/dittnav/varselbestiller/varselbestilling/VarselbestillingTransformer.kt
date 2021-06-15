package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import java.time.LocalDateTime
import java.time.ZoneId

object VarselbestillingTransformer {

    fun fromBeskjed(key: Nokkel, beskjed: Beskjed, doknotifikasjon: Doknotifikasjon): Varselbestilling {
        return Varselbestilling(
                bestillingsId = doknotifikasjon.getBestillingsId(),
                eventId = key.getEventId(),
                fodselsnummer = beskjed.getFodselsnummer(),
                systembruker = key.getSystembruker(),
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                prefererteKanaler = beskjed.getPrefererteKanaler()
        )
    }

    fun fromOppgave(key: Nokkel, oppgave: Oppgave, doknotifikasjon: Doknotifikasjon): Varselbestilling {
        return Varselbestilling(
                bestillingsId = doknotifikasjon.getBestillingsId(),
                eventId = key.getEventId(),
                fodselsnummer = oppgave.getFodselsnummer(),
                systembruker = key.getSystembruker(),
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                prefererteKanaler = oppgave.getPrefererteKanaler()
        )
    }
}
