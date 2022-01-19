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

    fun createOppgave(
        fodselsnummer: String = defaultFodselsnr,
        tekst: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        grupperingsid: String = defaultGrupperingsid,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = null,
        epostVarslingstittel: String? = null,
        smsVarslingstekst: String? = null
    ): Oppgave {
        return Oppgave(
            Instant.now().toEpochMilli(),
            Instant.now().toEpochMilli(),
            fodselsnummer,
            grupperingsid,
            tekst,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }

    fun createOppgaveWithFodselsnummer(fodselsnummer: String): Oppgave {
        return createOppgave(
            fodselsnummer = fodselsnummer,
        )
    }

    fun createOppgaveWithText(tekst: String): Oppgave {
        return createOppgave(
            tekst = tekst,
        )
    }

    fun createOppgaveWithSikkerhetsnivaa(sikkerhetsnivaa: Int): Oppgave {
        return createOppgave(
            sikkerhetsnivaa = sikkerhetsnivaa,
        )
    }

    fun createOppgaveWithLink(link: String): Oppgave {
        return createOppgave(
            link = link,
        )
    }

    fun createOppgaveWithGrupperingsid(grupperingsid: String): Oppgave {
        return createOppgave(
            grupperingsid = grupperingsid,
        )
    }

    fun createOppgaveWithEksternVarsling(eksternVarsling: Boolean): Oppgave {
        return createOppgave(
            eksternVarsling = eksternVarsling,
        )
    }

    fun createOppgaveWithEksternVarslingOgPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): Oppgave {
        return createOppgave(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }

    fun createOppgaveWithFodselsnummerOgEksternVarsling(fodselsnummer: String, eksternVarsling: Boolean): Oppgave {
        return createOppgave(
            fodselsnummer = fodselsnummer,
            eksternVarsling = eksternVarsling,
        )
    }

    fun createOppgaveWithEpostVarslingstekst(epostVarslingstekst: String): Oppgave {
        return createOppgave(
            epostVarslingstekst = epostVarslingstekst
        )
    }

    fun createOppgaveWithSmsVarslingstekst(smsVarslingstekst: String): Oppgave {
        return createOppgave(
            smsVarslingstekst = smsVarslingstekst
        )
    }
}
