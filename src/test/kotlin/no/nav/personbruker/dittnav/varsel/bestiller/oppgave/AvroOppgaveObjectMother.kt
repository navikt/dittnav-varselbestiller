package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Oppgave
import java.time.Instant

object AvroOppgaveObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "12345"
    private val defaultTekst = "Dette er oppgave til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false

    fun createOppgave(lopenummer: Int): Oppgave {
        return createOppgave(lopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createOppgaveWithFodselsnummer(lopenummer: Int, fodselsnummer: String): Oppgave {
        return createOppgave(lopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createOppgaveWithText(tekst: String): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, tekst, defaultSikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createOppgaveWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling)
    }

    fun createOppgaveWithEksternVarsling(): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, true)
    }

    fun createOppgave(lopenummer: Int, fodselsnummer: String, tekst: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean): Oppgave {
        return Oppgave(
                Instant.now().toEpochMilli(),
                fodselsnummer,
                "100$lopenummer",
                tekst,
                "https://nav.no/systemX/$lopenummer",
                sikkerhetsnivaa,
                eksternVarsling
        )
    }

}
