package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.builders.exception.UnknownEventtypeException
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.common.validation.throwExceptionIfBeskjedOrNokkelIsInvalid
import no.nav.personbruker.dittnav.varselbestiller.common.validation.throwExceptionIfInnboksOrNokkelIsInvalid
import no.nav.personbruker.dittnav.varselbestiller.common.validation.throwExceptionIfOppgaveOrNokkelIsInvalid
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype

object DoknotifikasjonCreator {

    fun createDoknotifikasjonFromBeskjed(nokkel: Nokkel, beskjed: Beskjed): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        throwExceptionIfBeskjedOrNokkelIsInvalid(nokkel, beskjed)
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.BESKJED))
                .setBestillerId(nokkel.getSystembruker())
                .setSikkerhetsnivaa(beskjed.getSikkerhetsnivaa())
                .setFodselsnummer(beskjed.getFodselsnummer())
                .setTittel(getDoknotifikasjonEmailTitle(beskjed))
                .setEpostTekst(getDoknotifikasjonEmailText(beskjed))
                .setSmsTekst(getDoknotifikasjonSMSText(beskjed))
                .setAntallRenotifikasjoner(0)
                .setPrefererteKanaler(getPrefererteKanaler(beskjed.getEksternVarsling(), beskjed.getPrefererteKanaler()))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonFromOppgave(nokkel: Nokkel, oppgave: Oppgave): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        throwExceptionIfOppgaveOrNokkelIsInvalid(nokkel, oppgave)
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.OPPGAVE))
                .setBestillerId(nokkel.getSystembruker())
                .setSikkerhetsnivaa(oppgave.getSikkerhetsnivaa())
                .setFodselsnummer(oppgave.getFodselsnummer())
                .setTittel(getDoknotifikasjonEmailTitle(oppgave))
                .setEpostTekst(getDoknotifikasjonEmailText(oppgave))
                .setSmsTekst(getDoknotifikasjonSMSText(oppgave))
                .setAntallRenotifikasjoner(1)
                .setRenotifikasjonIntervall(7)
                .setPrefererteKanaler(getPrefererteKanaler(oppgave.getEksternVarsling(), oppgave.getPrefererteKanaler()))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonFromInnboks(nokkel: Nokkel, innboks: Innboks): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        throwExceptionIfInnboksOrNokkelIsInvalid(nokkel, innboks)
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
            .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.INNBOKS))
            .setBestillerId(nokkel.getSystembruker())
            .setSikkerhetsnivaa(innboks.getSikkerhetsnivaa())
            .setFodselsnummer(innboks.getFodselsnummer())
            .setTittel(getDoknotifikasjonEmailTitle(innboks))
            .setEpostTekst(getDoknotifikasjonEmailText(innboks))
            .setSmsTekst(getDoknotifikasjonSMSText(innboks))
            .setAntallRenotifikasjoner(1)
            .setRenotifikasjonIntervall(4)
            .setPrefererteKanaler(getPrefererteKanaler(innboks.getEksternVarsling(), innboks.getPrefererteKanaler()))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKey(nokkel: Nokkel, eventtype: Eventtype): String {
        val eventId = ValidationUtil.validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", ValidationUtil.MAX_LENGTH_EVENTID)
        val systembruker = ValidationUtil.validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", ValidationUtil.MAX_LENGTH_SYSTEMBRUKER)
        return when (eventtype) {
            Eventtype.BESKJED -> "B-$systembruker-$eventId"
            Eventtype.OPPGAVE -> "O-$systembruker-$eventId"
            Eventtype.INNBOKS -> "I-$systembruker-$eventId"
            Eventtype.DONE -> "D-$systembruker-$eventId"
            else -> throw UnknownEventtypeException("$eventtype er ugyldig type for å generere Doknotifikasjon-key")
        }
    }

    private fun getDoknotifikasjonEmailText(event: Beskjed): String {
        if (event.getEpostVarslingstekst() != null) {
            val title = getDoknotifikasjonEmailTitle(event)
            val body = event.getEpostVarslingstekst()
            return replaceInEmailTemplate(title, body)
        }
        return this::class.java.getResource("/texts/epost_beskjed.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonEmailTitle(event: Beskjed): String {
        return event.getEpostVarslingstittel() ?: "Beskjed fra NAV"
    }

    private fun getDoknotifikasjonEmailText(event: Oppgave): String {
        if (event.getEpostVarslingstekst() != null) {
            val title = getDoknotifikasjonEmailTitle(event)
            val body = event.getEpostVarslingstekst()
            return replaceInEmailTemplate(title, body)
        }
        return this::class.java.getResource("/texts/epost_oppgave.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonEmailTitle(event: Oppgave): String {
        return event.getEpostVarslingstittel() ?: "Du har fått en oppgave fra NAV"
    }

    private fun getDoknotifikasjonEmailText(event: Innboks): String {
        if (event.getEpostVarslingstekst() != null) {
            val title = getDoknotifikasjonEmailTitle(event)
            val body = event.getEpostVarslingstekst()
            return replaceInEmailTemplate(title, body)
        }
        return this::class.java.getResource("/texts/epost_innboks.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonEmailTitle(event: Innboks): String {
        return event.getEpostVarslingstittel() ?: "Du har fått en melding fra NAV"
    }

    private fun replaceInEmailTemplate(title: String, body: String): String {
        val emailTemplate = this::class.java.getResource("/texts/epost_mal.txt").readText(Charsets.UTF_8)

        return emailTemplate.replace("\${EPOST_VARSELTITTEL}", title).replace("\${EPOST_VARSELTEKST}", body)
    }

    private fun getDoknotifikasjonSMSText(event: Beskjed): String {
        return event.getSmsVarslingstekst() ?: this::class.java.getResource("/texts/sms_beskjed.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonSMSText(event: Oppgave): String {
        return event.getSmsVarslingstekst() ?: this::class.java.getResource("/texts/sms_oppgave.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonSMSText(event: Innboks): String {
        return event.getSmsVarslingstekst() ?: this::class.java.getResource("/texts/sms_innboks.txt").readText(Charsets.UTF_8)
    }

    private fun getPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>?): List<PrefererteKanal> {
        val valgteKanaler = mutableListOf<PrefererteKanal>()
        if(!eksternVarsling && !prefererteKanaler.isNullOrEmpty()) {
            throw FieldValidationException("Prefererte kanaler kan ikke settes så lenge ekstern varsling ikke er bestilt.")
        } else {
            val doknotifikasjonPrefererteKanalerAsString = PrefererteKanal.values().map { it.name }
            prefererteKanaler?.forEach { preferertKanal ->
                if(doknotifikasjonPrefererteKanalerAsString.contains(preferertKanal)) {
                    valgteKanaler.add(PrefererteKanal.valueOf(preferertKanal))
                } else {
                    throw FieldValidationException("Ukjent kanal, kanalen $preferertKanal er ikke definert som mulig preferert kanal i Doknotifikasjon.")
                }
            }
        }
        return valgteKanaler
    }
}
