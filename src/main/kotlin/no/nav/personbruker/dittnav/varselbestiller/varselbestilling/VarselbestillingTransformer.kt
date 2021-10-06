package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import java.time.LocalDateTime
import java.time.ZoneId

object VarselbestillingTransformer {

    fun fromBeskjed(key: NokkelIntern, beskjed: BeskjedIntern, doknotifikasjon: Doknotifikasjon): Varselbestilling {
        return Varselbestilling(
                bestillingsId = doknotifikasjon.getBestillingsId(),
                eventId = key.getEventId(),
                fodselsnummer = key.getFodselsnummer(),
                appnavn = key.getAppnavn(),
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                prefererteKanaler = beskjed.getPrefererteKanaler()
        )
    }

    fun fromOppgave(key: NokkelIntern, oppgave: OppgaveIntern, doknotifikasjon: Doknotifikasjon): Varselbestilling {
        return Varselbestilling(
                bestillingsId = doknotifikasjon.getBestillingsId(),
                eventId = key.getEventId(),
                fodselsnummer = key.getFodselsnummer(),
                appnavn = key.getAppnavn(),
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                prefererteKanaler = oppgave.getPrefererteKanaler()
        )
    }
}
