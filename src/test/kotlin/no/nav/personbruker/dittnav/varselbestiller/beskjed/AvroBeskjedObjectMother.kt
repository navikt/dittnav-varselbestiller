package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createBeskjed(
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
    ): Beskjed {
        return Beskjed(
            Instant.now().toEpochMilli(),
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

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(
            fodselsnummer = fodselsnummer,
        )
    }

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(
            text = text,
        )
    }

    fun createBeskjedWithSikkerhetsnivaa(nivaa: Int): Beskjed {
        return createBeskjed(
            sikkerhetsnivaa = nivaa,
        )
    }

    fun createBeskjedWithLink(link: String): Beskjed {
        return createBeskjed(
            link = link,
        )
    }

    fun createBeskjedWithGrupperingsid(grupperingsid: String): Beskjed {
        return createBeskjed(
            grupperingsid = grupperingsid,
        )
    }

    fun createBeskjedWithEksternVarsling(eksternVarsling: Boolean): Beskjed {
        return createBeskjed(
            eksternVarsling = eksternVarsling,
        )
    }

    fun createBeskjedWithEksternVarslingOgPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): Beskjed {
        return createBeskjed(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }

    fun createBeskjedWithFodselsnummerOgEksternVarsling(fodselsnummer: String, eksternVarsling: Boolean): Beskjed {
        return createBeskjed(
            fodselsnummer = fodselsnummer,
            eksternVarsling = eksternVarsling,
        )
    }

    fun createBeskjedWithEpostVarslingstekst(epostVarslingstekst: String): Beskjed {
        return createBeskjed(
            epostVarslingstekst = epostVarslingstekst
        )
    }

    fun createBeskjedWithSmsVarslingstekst(smsVarslingstekst: String): Beskjed {
        return createBeskjed(
            smsVarslingstekst = smsVarslingstekst
        )
    }
}
