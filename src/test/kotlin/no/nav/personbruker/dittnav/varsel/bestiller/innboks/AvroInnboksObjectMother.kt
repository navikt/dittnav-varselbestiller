package no.nav.personbruker.dittnav.varsel.bestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import java.time.Instant

object AvroInnboksObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnummer = "12345"
    private val defaultText = "Dette er innboksnotifikasjon til brukeren"
    private val defaultSikkerhetsnivaa = 4

    fun createInnboks(lopenummer: Int): Innboks {
        return createInnboks(lopenummer, defaultFodselsnummer)
    }

    fun createInnboks(lopenummer: Int, fodselsnummer: String): Innboks {
        return createInnboks(lopenummer, fodselsnummer, defaultText, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithText(text: String): Innboks {
        return createInnboks(defaultLopenummer, defaultFodselsnummer, text, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithFodselsnummer(fodselsnummer: String): Innboks {
        return createInnboks(defaultLopenummer, fodselsnummer, defaultText, defaultSikkerhetsnivaa)
    }

    fun createInnboksWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Innboks {
        return createInnboks(defaultLopenummer, defaultFodselsnummer, defaultText, sikkerhetsnivaa)
    }

    fun createInnboks(lopenummer: Int, fodselsnummer: String, text: String, sikkerhetsnivaa: Int): Innboks {
        return Innboks(
                Instant.now().toEpochMilli(),
                fodselsnummer,
                "100$lopenummer",
                text,
                "https://nav.no/systemX/$lopenummer",
                sikkerhetsnivaa)
    }

}
