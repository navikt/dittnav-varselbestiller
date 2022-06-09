package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import java.time.LocalDateTime
import java.time.ZoneId

object VarselbestillingTransformer {

    fun fromBeskjed(nokkel: NokkelIntern, beskjed: BeskjedIntern): Varselbestilling {
        return Varselbestilling(
                bestillingsId = nokkel.getEventId(),
                eventId = nokkel.getEventId(),
                fodselsnummer = nokkel.getFodselsnummer(),
                systembruker = nokkel.getSystembruker(),
                namespace = nokkel.getNamespace(),
                appnavn = nokkel.getAppnavn(),
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                prefererteKanaler = beskjed.getPrefererteKanaler()
        )
    }

    fun fromOppgave(nokkel: NokkelIntern, oppgave: OppgaveIntern): Varselbestilling {
        return Varselbestilling(
                bestillingsId = nokkel.getEventId(),
                eventId = nokkel.getEventId(),
                fodselsnummer = nokkel.getFodselsnummer(),
                systembruker = nokkel.getSystembruker(),
                namespace = nokkel.getNamespace(),
                appnavn = nokkel.getAppnavn(),
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                prefererteKanaler = oppgave.getPrefererteKanaler()
        )
    }

    fun fromInnboks(nokkel: NokkelIntern, innboks: InnboksIntern): Varselbestilling {
        return Varselbestilling(
            bestillingsId = nokkel.getEventId(),
            eventId = nokkel.getEventId(),
            fodselsnummer = nokkel.getFodselsnummer(),
            systembruker = nokkel.getSystembruker(),
            namespace = nokkel.getNamespace(),
            appnavn = nokkel.getAppnavn(),
            bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
            prefererteKanaler = innboks.getPrefererteKanaler()
        )
    }
}
