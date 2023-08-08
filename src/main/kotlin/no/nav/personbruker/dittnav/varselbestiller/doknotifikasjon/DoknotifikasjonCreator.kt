package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.common.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.varsel.Varsel
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselType

object DoknotifikasjonCreator {
    private val log = KotlinLogging.logger {  }

    fun createDoknotifikasjonFromVarsel(varsel: Varsel): Doknotifikasjon = try {

        Doknotifikasjon.newBuilder()
            .setBestillingsId(varsel.varselId)
            .setBestillerId(varsel.produsent.appnavn)
            .setSikkerhetsnivaa(varsel.sensitivitet.loginLevel)
            .setFodselsnummer(varsel.ident)
            .setTittel(getDoknotifikasjonEmailTitle(varsel))
            .setEpostTekst(getDoknotifikasjonEmailText(varsel))
            .setSmsTekst(getDoknotifikasjonSMSText(varsel))
            .setPrefererteKanaler(getPrefererteKanaler(varsel.eksternVarslingBestilling.prefererteKanaler))
            .setRenotifikasjoner(varsel)
            .build()
    } catch (fe: FieldValidationException){
        log.warn { fe.toString() }
        throw fe
    }

    private fun getDoknotifikasjonEmailText(varsel: Varsel): String =
        varsel.eksternVarslingBestilling.epostVarslingstekst?.let {
            replaceInEmailTemplate(getDoknotifikasjonEmailTitle(varsel), varsel.eksternVarslingBestilling.epostVarslingstekst)
        } ?: when (varsel.type) {
            VarselType.Beskjed -> this::class.java.getResource("/texts/epost_beskjed.txt")!!.readText(Charsets.UTF_8)
            VarselType.Oppgave -> this::class.java.getResource("/texts/epost_oppgave.txt")!!.readText(Charsets.UTF_8)
            VarselType.Innboks -> this::class.java.getResource("/texts/epost_innboks.txt")!!.readText(Charsets.UTF_8)
        }

    private fun getDoknotifikasjonEmailTitle(varsel: Varsel): String =
        varsel.eksternVarslingBestilling.epostVarslingstittel ?: when(varsel.type) {
            VarselType.Beskjed -> "Beskjed fra NAV"
            VarselType.Oppgave -> "Du har fått en oppgave fra NAV"
            VarselType.Innboks -> "Du har fått en melding fra NAV"
        }

    private fun getDoknotifikasjonSMSText(varsel: Varsel): String =
        varsel.eksternVarslingBestilling.smsVarslingstekst ?: when(varsel.type) {
            VarselType.Beskjed -> "Hei! Du har fått en ny beskjed fra NAV. Logg inn på nav.no for å se hva beskjeden gjelder. Vennlig hilsen NAV"
            VarselType.Oppgave -> "Hei! Du har fått en ny oppgave fra NAV. Logg inn på nav.no for å se hva oppgaven gjelder. Vennlig hilsen NAV"
            VarselType.Innboks -> "Hei! Du har fått en ny melding fra NAV. Logg inn på nav.no for å lese meldingen. Vennlig hilsen NAV"
        }

    private fun Doknotifikasjon.Builder.setRenotifikasjoner(varsel: Varsel): Doknotifikasjon.Builder {
        when (varsel.type) {
            VarselType.Beskjed -> {
                antallRenotifikasjoner = 0
            }
            VarselType.Oppgave -> {
                antallRenotifikasjoner = 1
                renotifikasjonIntervall = 7
            }
            VarselType.Innboks -> {
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
    private fun getPrefererteKanaler(prefererteKanaler: List<String>?): List<PrefererteKanal> {
        val valgteKanaler = mutableListOf<PrefererteKanal>()
        val doknotifikasjonPrefererteKanalerAsString = PrefererteKanal.values().map { it.name }
        prefererteKanaler?.forEach { preferertKanal ->
            if(doknotifikasjonPrefererteKanalerAsString.contains(preferertKanal)) {
                valgteKanaler.add(PrefererteKanal.valueOf(preferertKanal))
            } else {
                throw FieldValidationException("Ukjent kanal, kanalen $preferertKanal er ikke definert som mulig preferert kanal i Doknotifikasjon.")
            }
        }

        return valgteKanaler
    }
}
