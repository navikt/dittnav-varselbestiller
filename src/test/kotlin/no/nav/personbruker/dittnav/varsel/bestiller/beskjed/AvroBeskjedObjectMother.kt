package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "1234"
    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(lopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(defaultLopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithSikkerhetsnivaa(nivaa: Int): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultTekst, nivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithEksternVarsling(lopenummer: Int, eksternVarsling: Boolean): Beskjed {
        return createBeskjedWithFodselsnummerOgEksternVarsling(lopenummer, defaultFodselsnr, eksternVarsling)
    }

    fun createBeskjedWithFodselsnummerOgEksternVarsling(lopenummer: Int, fodselsnummer: String, eksternVarsling: Boolean): Beskjed {
        return createBeskjed(lopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling)
    }

    fun createBeskjed(lopenummer: Int, fodselsnummer: String, text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean): Beskjed {
        return Beskjed(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                fodselsnummer,
                "100$lopenummer",
                text,
                "https://nav.no/systemX/$lopenummer",
                sikkerhetsnivaa,
                eksternVarsling
        )
    }

}
