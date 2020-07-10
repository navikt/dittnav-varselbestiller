package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import java.time.Instant

object AvroBeskjedObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "1234"
    private val defaultText = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4

    fun createBeskjedWithText(text: String): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, text, defaultSikkerhetsnivaa)
    }

    fun createBeskjedWithSikkerhetsnivaa(nivaa: Int): Beskjed {
        return createBeskjed(defaultLopenummer, defaultFodselsnr, defaultText, nivaa)
    }

    fun createBeskjed(lopenummer: Int): Beskjed {
        return createBeskjed(lopenummer, defaultFodselsnr, defaultText, defaultSikkerhetsnivaa)
    }

    fun createBeskjedWithFodselsnummer(fodselsnummer: String): Beskjed {
        return createBeskjed(defaultLopenummer, fodselsnummer, defaultText, defaultSikkerhetsnivaa)
    }

    fun createBeskjed(lopenummer: Int, fodselsnummer: String, text: String, sikkerhetsnivaa: Int): Beskjed {
        return Beskjed(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                fodselsnummer,
                "100$lopenummer",
                text,
                "https://nav.no/systemX/$lopenummer",
                sikkerhetsnivaa)
    }

}
