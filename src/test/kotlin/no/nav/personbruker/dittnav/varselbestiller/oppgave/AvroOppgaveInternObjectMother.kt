package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.Instant
import java.time.temporal.ChronoUnit

object AvroOppgaveInternObjectMother {

    private val defaultTekst = "Dette er oppgave til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createOppgave(): OppgaveIntern {
        return createOppgave(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler)
    }
    fun createOppgaveWithEksternVarsling(eksternVarsling: Boolean): OppgaveIntern {
        return createOppgave(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createOppgaveWithEksternVarslingOgPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): OppgaveIntern {
        return createOppgave(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, prefererteKanaler)
    }

    fun createOppgaveWithEpostVarslingstekst(epostVarslingstekst: String): OppgaveIntern {
        return createOppgave(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler, epostVarslingstekst = epostVarslingstekst)
    }

    fun createOppgaveWithSmsVarslingstekst(smsVarslingstekst: String): OppgaveIntern {
        return createOppgave(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler, smsVarslingstekst = smsVarslingstekst)
    }

    fun createOppgave(tekst: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, prefererteKanaler: List<String>, epostVarslingstekst: String? = null, smsVarslingstekst: String? = null): OppgaveIntern {
        return OppgaveIntern(
                Instant.now().toEpochMilli(),
                Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli(),
                tekst,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler,
                epostVarslingstekst,
                smsVarslingstekst
        )
    }
}
