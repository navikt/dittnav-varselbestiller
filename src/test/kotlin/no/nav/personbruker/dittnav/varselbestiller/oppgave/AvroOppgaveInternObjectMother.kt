package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.Instant

object AvroOppgaveInternObjectMother {

    private val defaultTekst = "Dette er oppgave til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createOppgaveIntern(): OppgaveIntern {
        return createOppgaveIntern(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler)
    }
    fun createOppgaveInternWithEksternVarsling(eksternVarsling: Boolean): OppgaveIntern {
        return createOppgaveIntern(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createOppgaveInternWithEksternVarslingOgPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): OppgaveIntern {
        return createOppgaveIntern(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, prefererteKanaler)
    }

    private fun createOppgaveIntern(tekst: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, prefererteKanaler: List<String>): OppgaveIntern {
        return OppgaveIntern(
                Instant.now().toEpochMilli(),
                tekst,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler
        )
    }
}
