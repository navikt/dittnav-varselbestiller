package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.varsel.Varsel
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselType

object DoknotifikasjonCreator {

    fun createDoknotifikasjonFromVarsel(varsel: Varsel): Doknotifikasjon =
        Doknotifikasjon.newBuilder()
            .setBestillingsId(varsel.eventId)
            .setBestillerId(varsel.appnavn)
            .setSikkerhetsnivaa(varsel.sikkerhetsnivaa)
            .setFodselsnummer(varsel.fodselsnummer)
            .setTittel(getDoknotifikasjonEmailTitle(varsel))
            .setEpostTekst(getDoknotifikasjonEmailText(varsel))
            .setSmsTekst(getDoknotifikasjonSMSText(varsel))
            .setPrefererteKanaler(getPrefererteKanaler(varsel.eksternVarsling, varsel.prefererteKanaler))
            .setRenotifikasjoner(varsel)
            .build()

    private fun getDoknotifikasjonEmailText(varsel: Varsel): String =
        varsel.epostVarslingstekst?.let {
            replaceInEmailTemplate(getDoknotifikasjonEmailTitle(varsel), varsel.epostVarslingstekst)
        } ?: when (varsel.varselType) {
            VarselType.BESKJED -> this::class.java.getResource("/texts/epost_beskjed.txt")!!.readText(Charsets.UTF_8)
            VarselType.OPPGAVE -> this::class.java.getResource("/texts/epost_oppgave.txt")!!.readText(Charsets.UTF_8)
            VarselType.INNBOKS -> this::class.java.getResource("/texts/epost_innboks.txt")!!.readText(Charsets.UTF_8)
        }

    private fun getDoknotifikasjonEmailTitle(varsel: Varsel): String =
        varsel.epostVarslingstittel ?: when(varsel.varselType) {
            VarselType.BESKJED -> "Beskjed fra NAV"
            VarselType.OPPGAVE -> "Du har fått en oppgave fra NAV"
            VarselType.INNBOKS -> "Du har fått en melding fra NAV"
        }

    private fun getDoknotifikasjonSMSText(varsel: Varsel): String =
        varsel.smsVarslingstekst ?: when(varsel.varselType) {
            VarselType.BESKJED -> this::class.java.getResource("/texts/sms_beskjed.txt")!!.readText(Charsets.UTF_8)
            VarselType.OPPGAVE -> this::class.java.getResource("/texts/sms_oppgave.txt")!!.readText(Charsets.UTF_8)
            VarselType.INNBOKS -> this::class.java.getResource("/texts/sms_innboks.txt")!!.readText(Charsets.UTF_8)
        }

    private fun Doknotifikasjon.Builder.setRenotifikasjoner(varsel: Varsel): Doknotifikasjon.Builder {
        when (varsel.varselType) {
            VarselType.BESKJED -> {
                antallRenotifikasjoner = 0
            }
            VarselType.OPPGAVE -> {
                antallRenotifikasjoner = 1
                renotifikasjonIntervall = 7
            }
            VarselType.INNBOKS -> {
                antallRenotifikasjoner = 1
                renotifikasjonIntervall = 4
            }
        }
        return this
    }

    private fun replaceInEmailTemplate(title: String, body: String): String {
        val emailTemplate = this::class.java.getResource("/texts/epost_mal.txt").readText(Charsets.UTF_8)

        return emailTemplate.replace("\${EPOST_VARSELTITTEL}", title).replace("\${EPOST_VARSELTEKST}", body)
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
