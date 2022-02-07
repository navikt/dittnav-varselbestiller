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
    private val defaultSynligFremTil = Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli()
    private val defaultEpostVarslingstekst: String? = null
    private val defaultEpostVarslingstittel: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createOppgaveIntern(
        tekst: String = defaultTekst,
        synligFremTil: Long? = defaultSynligFremTil,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): OppgaveIntern {
        return OppgaveIntern(
            Instant.now().toEpochMilli(),
            synligFremTil,
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

    fun createOppgaveInternWithEksternVarsling(eksternVarsling: Boolean): OppgaveIntern {
        return createOppgaveIntern(
            eksternVarsling = eksternVarsling,
        )
    }

    fun createOppgaveInternWithEksternVarslingOgPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): OppgaveIntern {
        return createOppgaveIntern(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }

    fun createOppgaveWithEpostVarslingstekst(epostVarslingstekst: String): OppgaveIntern {
        return createOppgaveIntern(
            epostVarslingstekst = epostVarslingstekst
        )
    }

    fun createOppgaveWithSmsVarslingstekst(smsVarslingstekst: String): OppgaveIntern {
        return createOppgaveIntern(
            smsVarslingstekst = smsVarslingstekst
        )
    }
}
