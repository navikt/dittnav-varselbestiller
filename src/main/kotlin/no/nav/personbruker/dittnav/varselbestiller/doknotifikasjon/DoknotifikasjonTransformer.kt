package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.validation.validateFodselsnummer
import no.nav.personbruker.dittnav.varselbestiller.common.validation.validateNonNullFieldMaxLength
import no.nav.personbruker.dittnav.varselbestiller.common.validation.validateSikkerhetsnivaa
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import java.time.ZonedDateTime

object DoknotifikasjonTransformer {

    fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.BESKJED))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(beskjed.getSikkerhetsnivaa()))
                .setFodselsnummer(validateFodselsnummer(beskjed.getFodselsnummer()))
                .setTittel("Du har fått en beskjed fra NAV")
                .setEpostTekst("Beskjed fra NAV på e-post")
                .setSmsTekst("Beskjed fra NAV på SMS")
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKey(nokkel: Nokkel, eventtype: Eventtype): String {
        val eventId = validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50)
        val systembruker = validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100)
        return when(eventtype) {
            Eventtype.BESKJED -> "B-$systembruker-$eventId"
            Eventtype.OPPGAVE -> "O-$systembruker-$eventId"
            Eventtype.DONE -> "D-$systembruker-$eventId"
            else -> throw FieldValidationException("$eventtype er ugyldig type for å generere Doknotifikasjon-key")
        }
    }

    fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.OPPGAVE))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(oppgave.getSikkerhetsnivaa()))
                .setFodselsnummer(validateFodselsnummer(oppgave.getFodselsnummer()))
                .setTittel("Du har fått en oppgave fra NAV")
                .setEpostTekst("Oppgave fra NAV på e-post")
                .setSmsTekst("Oppgave fra NAV på SMS")
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun toInternal(eventtype: Eventtype, externalValue: no.nav.doknotifikasjon.schemas.Doknotifikasjon): Doknotifikasjon {
        return Doknotifikasjon(
                bestillingsid = externalValue.getBestillingsId(),
                eventtype = eventtype,
                systembruker = externalValue.getBestillerId(),
                eventtidspunkt = ZonedDateTime.now()
        )
    }
}
