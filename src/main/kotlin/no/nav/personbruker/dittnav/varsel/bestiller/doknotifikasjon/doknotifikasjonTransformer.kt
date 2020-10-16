package no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateFodselsnummer

fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): Doknotifikasjon {
    val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
            .setBestillingsId(nokkel.getEventId())
            .setBestillerId(nokkel.getSystembruker())
            .setFodselsnummer(validateFodselsnummer(beskjed.getFodselsnummer()))
            .setTittel("Du har fått en beskjed fra NAV")
            .setEpostTekst("Her er e-postteksten")
            .setPrefererteKanaler("EPOST")
    return doknotifikasjonBuilder.build()
}

fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): Doknotifikasjon {
    val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
            .setBestillingsId(nokkel.getEventId())
            .setBestillerId(nokkel.getSystembruker())
            .setFodselsnummer(validateFodselsnummer(oppgave.getFodselsnummer()))
            .setTittel("Du har fått en oppgave fra NAV")
            .setEpostTekst("Her er e-postteksten")
            .setPrefererteKanaler("EPOST")
    return doknotifikasjonBuilder.build()
}

fun createDoknotifikasjonStoppFromDone(nokkel: Nokkel, done: Done): DoknotifikasjonStopp {
    val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
            .setBestillingsId(nokkel.getEventId())
            .setBestillerId(nokkel.getSystembruker())
    return doknotifikasjonStoppBuilder.build()

}
