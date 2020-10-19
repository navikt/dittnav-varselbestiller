package no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateFodselsnummer
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength

object DoknotifikasjonTransformer {

    fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): Doknotifikasjon {
        val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
                .setBestillingsId(validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setFodselsnummer(validateFodselsnummer(beskjed.getFodselsnummer()))
                .setTittel("Du har fått en beskjed fra NAV")
                .setEpostTekst("Her er e-postteksten")
                .setSmsTekst("Her er SMS-teksten")
                .setPrefererteKanaler("EPOST")
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): Doknotifikasjon {
        val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
                .setBestillingsId(validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50))
                .setBestillerId(nokkel.getSystembruker())
                .setFodselsnummer(validateFodselsnummer(oppgave.getFodselsnummer()))
                .setTittel("Du har fått en oppgave fra NAV")
                .setEpostTekst("Her er e-postteksten")
                .setSmsTekst("Her er SMS-teksten")
                .setPrefererteKanaler("EPOST")
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonStopp(nokkel: Nokkel): DoknotifikasjonStopp {
        val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
                .setBestillingsId(validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
        return doknotifikasjonStoppBuilder.build()

    }

}
