package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Oppgave
import java.time.Instant

object AvroOppgaveObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "12345"
    private val defaultTekst = "Dette er oppgave til brukeren"
    private val defaultSikkerhetsnivaa = 4

    fun createOppgave(lopenummer: Int): Oppgave {
        return createOppgave(lopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa)
    }

    fun createOppgaveWithFodselsnummer(lopenummer: Int, fodselsnummer: String): Oppgave {
        return createOppgave(lopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa)
    }

    fun createOppgaveWithText(tekst: String): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, tekst, defaultSikkerhetsnivaa)
    }

    fun createOppgaveWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, defaultTekst, sikkerhetsnivaa)
    }

    fun createOppgave(lopenummer: Int, fodselsnummer: String, tekst: String, sikkerhetsnivaa: Int): Oppgave {
        return Oppgave(
                Instant.now().toEpochMilli(),
                fodselsnummer,
                "100$lopenummer",
                tekst,
                "https://nav.no/systemX/$lopenummer",
                sikkerhetsnivaa)
    }

}
