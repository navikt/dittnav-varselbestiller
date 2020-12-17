package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.UnknownEventtypeException
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.common.validation.throwExceptionIfBeskjedOrNokkelIsNotValid
import no.nav.personbruker.dittnav.varselbestiller.common.validation.throwExceptionIfOppgaveOrNokkelIsNotValid
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype

object DoknotifikasjonCreator {

    fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        throwExceptionIfBeskjedOrNokkelIsNotValid(nokkel, beskjed)
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.BESKJED))
                .setBestillerId(nokkel.getSystembruker())
                .setSikkerhetsnivaa(beskjed.getSikkerhetsnivaa())
                .setFodselsnummer(beskjed.getFodselsnummer())
                .setTittel("Beskjed fra NAV")
                .setEpostTekst(getDoknotifikasjonEmailText(Eventtype.BESKJED))
                .setSmsTekst(getDoknotifikasjonSMSText(Eventtype.BESKJED))
                .setAntallRenotifikasjoner(1)
                .setRenotifikasjonIntervall(1)
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKey(nokkel: Nokkel, eventtype: Eventtype): String {
        val eventId = ValidationUtil.validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", ValidationUtil.MAX_LENGTH_EVENTID)
        val systembruker = ValidationUtil.validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", ValidationUtil.MAX_LENGTH_SYSTEMBRUKER)
        return when (eventtype) {
            Eventtype.BESKJED -> "B-$systembruker-$eventId"
            Eventtype.OPPGAVE -> "O-$systembruker-$eventId"
            Eventtype.DONE -> "D-$systembruker-$eventId"
            else -> throw UnknownEventtypeException("$eventtype er ugyldig type for å generere Doknotifikasjon-key")
        }
    }

    fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        throwExceptionIfOppgaveOrNokkelIsNotValid(nokkel, oppgave)
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.OPPGAVE))
                .setBestillerId(nokkel.getSystembruker())
                .setSikkerhetsnivaa(oppgave.getSikkerhetsnivaa())
                .setFodselsnummer(oppgave.getFodselsnummer())
                .setTittel("Du har fått en oppgave fra NAV")
                .setEpostTekst(getDoknotifikasjonEmailText(Eventtype.OPPGAVE))
                .setSmsTekst(getDoknotifikasjonSMSText(Eventtype.OPPGAVE))
                .setAntallRenotifikasjoner(1)
                .setRenotifikasjonIntervall(1)
                .setPrefererteKanaler(listOf(PrefererteKanal.EPOST, PrefererteKanal.SMS))
        return doknotifikasjonBuilder.build()
    }

    private fun getDoknotifikasjonEmailText(eventtype: Eventtype): String {
        return when (eventtype) {
            Eventtype.BESKJED -> this::class.java.getResource("/texts/epost_beskjed.txt").readText(Charsets.UTF_8)
            Eventtype.OPPGAVE -> this::class.java.getResource("/texts/epost_oppgave.txt").readText(Charsets.UTF_8)
            else -> throw UnknownEventtypeException("Finnes ikke e-posttekst for $eventtype.")
        }
    }

    private fun getDoknotifikasjonSMSText(eventtype: Eventtype): String {
        return when (eventtype) {
            Eventtype.BESKJED -> this::class.java.getResource("/texts/sms_beskjed.txt").readText(Charsets.UTF_8)
            Eventtype.OPPGAVE -> this::class.java.getResource("/texts/sms_oppgave.txt").readText(Charsets.UTF_8)
            else -> throw UnknownEventtypeException("Finnes ikke SMS-tekst for $eventtype.")
        }
    }
}
