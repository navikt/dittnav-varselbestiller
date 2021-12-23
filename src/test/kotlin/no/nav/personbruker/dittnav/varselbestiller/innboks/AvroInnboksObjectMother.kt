package no.nav.personbruker.dittnav.varselbestiller.innboks

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import java.time.Instant

object AvroInnboksObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Innboks til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createInnboks(): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithFodselsnummer(fodselsnummer: String): Innboks {
        return createInnboks(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithSikkerhetsnivaa(nivaa: Int): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, nivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithGrupperingsid(grupperingsid: String): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithText(text: String): Innboks {
        return createInnboks(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithLink(link: String): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithEksternVarsling(eksternVarsling: Boolean): Innboks {
        return createInnboksWithFodselsnummerOgEksternVarsling(defaultFodselsnr, eksternVarsling)
    }

    fun createInnboksWithFodselsnummerOgEksternVarsling(fodselsnummer: String, eksternVarsling: Boolean): Innboks {
        return createInnboks(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createInnboksWithEksternVarslingOgPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, prefererteKanaler)
    }

    fun createInnboksWithEpostVarslingstekst(epostVarslingstekst: String): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler, epostVarslingstekst = epostVarslingstekst)
    }

    fun createInnboksWithSmsVarslingstekst(smsVarslingstekst: String): Innboks {
        return createInnboks(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler, smsVarslingstekst = smsVarslingstekst)
    }

    private fun createInnboks(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, prefererteKanaler: List<String>, epostVarslingstekst: String? = null, smsVarslingstekst: String? = null): Innboks {
        return Innboks(
            Instant.now().toEpochMilli(),
            fodselsnummer,
            grupperingsid,
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            smsVarslingstekst
        )
    }
}
