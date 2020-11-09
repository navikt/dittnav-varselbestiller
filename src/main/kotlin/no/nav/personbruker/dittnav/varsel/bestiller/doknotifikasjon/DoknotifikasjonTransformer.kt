package no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateFodselsnummer
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType

object DoknotifikasjonTransformer {

    fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): Doknotifikasjon {
        val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, EventType.BESKJED))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setFodselsnummer(validateFodselsnummer(beskjed.getFodselsnummer()))
                .setTittel("Du har fått en beskjed fra NAV")
                .setEpostTekst("Beskjed fra NAV på e-post")
                .setSmsTekst("Beskjed fra NAV på SMS")
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKey(nokkel: Nokkel, eventType: EventType): String {
        val eventId = validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50)
        val systembruker = validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100)
        return when(eventType) {
            EventType.BESKJED -> "B-$systembruker-$eventId"
            EventType.OPPGAVE -> "O-$systembruker-$eventId"
            EventType.DONE -> "D-$systembruker-$eventId"
            else -> throw FieldValidationException("$eventType er ugyldig type for å generere Doknotifikasjon-key")
        }
    }

    fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): Doknotifikasjon {
        val doknotifikasjonBuilder = Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, EventType.OPPGAVE))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setFodselsnummer(validateFodselsnummer(oppgave.getFodselsnummer()))
                .setTittel("Du har fått en oppgave fra NAV")
                .setEpostTekst("Oppgave fra NAV på e-post")
                .setSmsTekst("Oppgave fra NAV på SMS")
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonStopp(nokkel: Nokkel): DoknotifikasjonStopp {
        val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
                .setBestillingsId(validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
        return doknotifikasjonStoppBuilder.build()

    }

}
