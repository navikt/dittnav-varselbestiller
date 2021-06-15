package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import java.time.Instant

object AvroOppgaveObjectMother {

    private val defaultFodselsnr = "12345678901"
    private val defaultTekst = "Dette er oppgave til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultGrupperingsid = "123"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createOppgave(): Oppgave {
        return createOppgave(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithFodselsnummer(fodselsnummer: String): Oppgave {
        return createOppgave(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithText(tekst: String): Oppgave {
        return createOppgave(defaultFodselsnr, tekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgave(defaultFodselsnr, defaultTekst, sikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithLink(link: String): Oppgave {
        return createOppgave(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, link, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithGrupperingsid(grupperingsid: String): Oppgave {
        return createOppgave(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, grupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithEksternVarsling(eksternVarsling: Boolean): Oppgave {
        return createOppgave(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgaveWithEksternVarslingOgPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): Oppgave {
        return createOppgave(defaultFodselsnr, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, prefererteKanaler)
    }

    fun createOppgaveWithFodselsnummerOgEksternVarsling(fodselsnummer: String, eksternVarsling: Boolean): Oppgave {
        return createOppgave(fodselsnummer, defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultGrupperingsid, defaultPrefererteKanaler)
    }

    fun createOppgave(fodselsnummer: String, tekst: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, grupperingsid: String, prefererteKanaler: List<String>): Oppgave {
        return Oppgave(
                Instant.now().toEpochMilli(),
                fodselsnummer,
                grupperingsid,
                tekst,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler
        )
    }
}
