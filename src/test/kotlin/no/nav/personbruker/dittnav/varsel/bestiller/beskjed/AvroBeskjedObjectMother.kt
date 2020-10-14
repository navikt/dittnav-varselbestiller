package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "1234"
    private val defaultText = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(lopenummer, defaultFodselsnr, defaultText, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(defaultLopenummer, fodselsnummer, defaultText, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }


    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, text, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithSikkerhetsnivaa(nivaa: Int): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultText, nivaa, defaultEksternVarsling)
    }

    fun createBeskjedWithEksternVarsling(): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultText, defaultSikkerhetsnivaa, true)
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
