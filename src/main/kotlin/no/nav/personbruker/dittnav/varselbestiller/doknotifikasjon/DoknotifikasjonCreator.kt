package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UnknownEventtypeException
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype

object DoknotifikasjonCreator {

    fun createDoknotifikasjonFromBeskjed(nokkel: NokkelIntern, beskjed: BeskjedIntern): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.BESKJED_INTERN))
                .setBestillerId(nokkel.getAppnavn())
                .setSikkerhetsnivaa(beskjed.getSikkerhetsnivaa())
                .setFodselsnummer(nokkel.getFodselsnummer())
                .setTittel("Beskjed fra NAV")
                .setEpostTekst(getDoknotifikasjonEmailText(beskjed))
                .setSmsTekst(getDoknotifikasjonSMSText(beskjed))
                .setAntallRenotifikasjoner(0)
                .setPrefererteKanaler(getPrefererteKanaler(beskjed.getEksternVarsling(), beskjed.getPrefererteKanaler()))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonFromOppgave(nokkel: NokkelIntern, oppgave: OppgaveIntern): no.nav.doknotifikasjon.schemas.Doknotifikasjon {
        val doknotifikasjonBuilder = no.nav.doknotifikasjon.schemas.Doknotifikasjon.newBuilder()
                .setBestillingsId(createDoknotifikasjonKey(nokkel, Eventtype.OPPGAVE_INTERN))
                .setBestillerId(nokkel.getAppnavn())
                .setSikkerhetsnivaa(oppgave.getSikkerhetsnivaa())
                .setFodselsnummer(nokkel.getFodselsnummer())
                .setTittel("Du har fått en oppgave fra NAV")
                .setEpostTekst(getDoknotifikasjonEmailText(oppgave))
                .setSmsTekst(getDoknotifikasjonSMSText(oppgave))
                .setAntallRenotifikasjoner(1)
                .setRenotifikasjonIntervall(7)
                .setPrefererteKanaler(getPrefererteKanaler(oppgave.getEksternVarsling(), oppgave.getPrefererteKanaler()))
        return doknotifikasjonBuilder.build()
    }

    fun createDoknotifikasjonKey(nokkel: NokkelIntern, eventtype: Eventtype): String {
        val eventId = nokkel.getEventId()
        val appnavn = nokkel.getAppnavn()
        return when (eventtype) {
            Eventtype.BESKJED_INTERN -> "B-$appnavn-$eventId"
            Eventtype.OPPGAVE_INTERN -> "O-$appnavn-$eventId"
            Eventtype.DONE_INTERN -> "D-$appnavn-$eventId"
            else -> throw UnknownEventtypeException("$eventtype er ugyldig type for å generere Doknotifikasjon-key")
        }
    }

    private fun getDoknotifikasjonEmailText(event: BeskjedIntern): String {
        return event.getEpostVarslingstekst() ?: this::class.java.getResource("/texts/epost_beskjed.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonEmailText(event: OppgaveIntern): String {
        return event.getEpostVarslingstekst() ?: this::class.java.getResource("/texts/epost_oppgave.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonSMSText(event: BeskjedIntern): String {
        return event.getSmsVarslingstekst() ?: this::class.java.getResource("/texts/sms_beskjed.txt").readText(Charsets.UTF_8)
    }

    private fun getDoknotifikasjonSMSText(event: OppgaveIntern): String {
        return event.getSmsVarslingstekst() ?: this::class.java.getResource("/texts/sms_oppgave.txt").readText(Charsets.UTF_8)
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
