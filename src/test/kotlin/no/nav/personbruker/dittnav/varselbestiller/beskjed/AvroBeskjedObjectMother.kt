package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"

    fun createBeskjed(): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithSikkerhetsnivaa(nivaa: Int): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, nivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjedWithLink(link: String): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid)
    }

    fun createBeskjedWithGrupperingsid(grupperingsid: String): Beskjed {
        return createBeskjed(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid)
    }

    fun createBeskjedWithEksternVarsling(eksternVarsling: Boolean): Beskjed {
        return createBeskjedWithFodselsnummerOgEksternVarsling(defaultFodselsnr, eksternVarsling)
    }

    fun createBeskjedWithFodselsnummerOgEksternVarsling(fodselsnummer: String, eksternVarsling: Boolean): Beskjed {
        return createBeskjed(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createBeskjed(fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String): Beskjed {
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
