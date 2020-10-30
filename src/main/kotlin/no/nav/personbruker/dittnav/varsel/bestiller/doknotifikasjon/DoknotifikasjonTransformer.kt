package no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateFodselsnummer
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength

object DoknotifikasjonTransformer {

    fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): Doknotifikasjon {
        val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKeyForBeskjed(nokkel))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setFodselsnummer(validateFodselsnummer(beskjed.getFodselsnummer()))
                .setTittel("Du har fått en beskjed fra NAV")
                .setEpostTekst("Her er e-postteksten")
                .setSmsTekst("Her er SMS-teksten")
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKeyForBeskjed(nokkel: Nokkel): String {
        val eventId = validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50)
        val systembruker = validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100)
        return "B-$eventId-$systembruker"
    }

    fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): Doknotifikasjon {
        val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKeyForOppgave(nokkel))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setFodselsnummer(validateFodselsnummer(oppgave.getFodselsnummer()))
                .setTittel("Du har fått en oppgave fra NAV")
                .setEpostTekst("Her er e-postteksten")
                .setSmsTekst("Her er SMS-teksten")
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKeyForOppgave(nokkel: Nokkel): String {
        val eventId = validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50)
        val systembruker = validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100)
        return "O-$eventId-$systembruker"
    }

    fun createDoknotifikasjonStopp(nokkel: Nokkel): DoknotifikasjonStopp {
        val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
                .setBestillingsId(validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
        return doknotifikasjonStoppBuilder.build()

    }

}
