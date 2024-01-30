package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.tms.ekstern.varselbestiller.config.FieldValidationException

object DoknotifikasjonCreator {
    private val log = KotlinLogging.logger { }

    fun createDoknotifikasjonFromVarsel(varsel: Varsel): Doknotifikasjon = try {

        Doknotifikasjon.newBuilder()
            .setBestillingsId(varsel.varselId)
            .setBestillerId(varsel.produsent.appnavn)
            .setSikkerhetsnivaa(varsel.sensitivitet.loginLevel)
            .setFodselsnummer(varsel.ident)
            .setTittel(varsel.eksternVarslingBestilling.epostVarslingstittel ?: varsel.type.emailTitle)
            .setEpostTekst(getDoknotifikasjonEmailText(varsel))
            .setSmsTekst(varsel.eksternVarslingBestilling.smsVarslingstekst ?:varsel.type.smsText)
            .setPrefererteKanaler(getPrefererteKanaler(varsel.eksternVarslingBestilling.prefererteKanaler))
            .setRenotifikasjoner(varsel)
            .build()
    } catch (fe: FieldValidationException) {
        log.warn { fe.toString() }
        throw fe
    }

    private fun getDoknotifikasjonEmailText(varsel: Varsel): String =
        varsel.eksternVarslingBestilling.epostVarslingstekst?.let {
            replaceInEmailTemplate(
                varsel.eksternVarslingBestilling.epostVarslingstittel ?: varsel.type.emailTitle,
                varsel.eksternVarslingBestilling.epostVarslingstekst
            )
        } ?: varsel.type.emailText

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
            if (doknotifikasjonPrefererteKanalerAsString.contains(preferertKanal)) {
                valgteKanaler.add(PrefererteKanal.valueOf(preferertKanal))
            } else {
                throw FieldValidationException("Ukjent kanal, kanalen $preferertKanal er ikke definert som mulig preferert kanal i Doknotifikasjon.")
            }
        }

        return valgteKanaler
    }
}
