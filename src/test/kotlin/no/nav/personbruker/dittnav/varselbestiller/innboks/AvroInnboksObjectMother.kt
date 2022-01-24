package no.nav.personbruker.dittnav.varselbestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import java.time.Instant

object AvroInnboksObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Innboks til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createInnboks(
        fodselsnummer: String = defaultFodselsnr,
        text: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        grupperingsid: String = defaultGrupperingsid,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = null,
        epostVarslingstittel: String? = null,
        smsVarslingstekst: String? = null
    ): Innboks {
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
            epostVarslingstittel,
            smsVarslingstekst
        )
    }

    fun createInnboksWithFodselsnummer(fodselsnummer: String): Innboks {
        return createInnboks(
            fodselsnummer = fodselsnummer,
        )
    }

    fun createInnboksWithSikkerhetsnivaa(nivaa: Int): Innboks {
        return createInnboks(
            sikkerhetsnivaa = nivaa,
        )
    }

    fun createInnboksWithGrupperingsid(grupperingsid: String): Innboks {
        return createInnboks(
            grupperingsid = grupperingsid,
        )
    }

    fun createInnboksWithText(text: String): Innboks {
        return createInnboks(
            text = text,
        )
    }

    fun createInnboksWithLink(link: String): Innboks {
        return createInnboks(
            link = link,
        )
    }

    fun createInnboksWithEksternVarsling(eksternVarsling: Boolean): Innboks {
        return createInnboks(
            eksternVarsling = eksternVarsling
        )
    }

    fun createInnboksWithFodselsnummerOgEksternVarsling(fodselsnummer: String, eksternVarsling: Boolean): Innboks {
        return createInnboks(
            fodselsnummer = fodselsnummer,
            eksternVarsling = eksternVarsling,
        )
    }

    fun createInnboksWithEksternVarslingOgPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): Innboks {
        return createInnboks(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }

    fun createInnboksWithEpostVarslingstekst(epostVarslingstekst: String): Innboks {
        return createInnboks(
            epostVarslingstekst = epostVarslingstekst
        )
    }

    fun createInnboksWithSmsVarslingstekst(smsVarslingstekst: String): Innboks {
        return createInnboks(
            smsVarslingstekst = smsVarslingstekst
        )
    }
}
