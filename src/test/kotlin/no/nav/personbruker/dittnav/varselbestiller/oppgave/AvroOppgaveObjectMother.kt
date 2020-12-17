package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Oppgave
import java.time.Instant

object AvroOppgaveObjectMother {

    private val defaultLopenummer = 1
    private val defaultFodselsnr = "12345"
    private val defaultTekst = "Dette er oppgave til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = false
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"

    fun createOppgave(lopenummer: Int): Oppgave {
        return createOppgave(lopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createOppgaveWithFodselsnummer(lopenummer: Int, fodselsnummer: String): Oppgave {
        return createOppgave(lopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createOppgaveWithText(tekst: String): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, tekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createOppgaveWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createOppgaveWithLink(link: String): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid)
    }

    fun createOppgaveWithGrupperingsid(grupperingsid: String): Oppgave {
        return createOppgave(defaultLopenummer, defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid)
    }

    fun createOppgaveWithEksternVarsling(lopenummer: Int, eksternVarsling: Boolean): Oppgave {
        return createOppgaveWithFodselsnummerOgEksternVarsling(lopenummer, defaultFodselsnr, eksternVarsling)
    }

    fun createOppgaveWithFodselsnummerOgEksternVarsling(lopenummer: Int, fodselsnummer: String, eksternVarsling: Boolean): Oppgave {
        return createOppgave(lopenummer, fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid)
    }

    fun createOppgave(lopenummer: Int, fodselsnummer: String, tekst: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String): Oppgave {
        return Oppgave(
                Instant.now().toEpochMilli(),
                fodselsnummer,
                grupperingsid,
                tekst,
                link,
                sikkerhetsnivaa,
                eksternVarsling
        )
    }

}
