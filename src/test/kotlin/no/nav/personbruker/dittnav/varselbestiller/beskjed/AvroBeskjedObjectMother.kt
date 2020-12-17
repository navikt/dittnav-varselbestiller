package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(lopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(defaultLopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithSikkerhetsnivaa(nivaa: Int): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultTekst, nivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithLink(link: String): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid)
    }

    fun createBeskjedWithGrupperingsid(grupperingsid: String): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid)
    }

    fun createBeskjedWithEksternVarsling(lopenummer: Int, eksternVarsling: Boolean): Beskjed {
        return createBeskjedWithFodselsnummerOgEksternVarsling(lopenummer, defaultFodselsnr, eksternVarsling)
    }

    fun createBeskjedWithFodselsnummerOgEksternVarsling(lopenummer: Int, fodselsnummer: String, eksternVarsling: Boolean): Beskjed {
        return createBeskjed(lopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjed(lopenummer: Int, fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String): Beskjed {
        return Beskjed(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                fodselsnummer,
                grupperingsid,
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling
        )
    }

}
