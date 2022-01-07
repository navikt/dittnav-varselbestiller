package no.nav.personbruker.dittnav.varselbestiller.innboks

import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.Instant

object AvroInnboksInternObjectMother {

    private val defaultTekst = "Dette er Innboks til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createInnboksIntern(): InnboksIntern {
        return createInnboksIntern(defaultTekst, defaultSikkerhetsnivaa, defaultEksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createInnboksInternWithEksternVarsling(eksternVarsling: Boolean): InnboksIntern {
        return createInnboksIntern(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, defaultPrefererteKanaler)
    }

    fun createInnboksInternWithEksternVarslingOgPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): InnboksIntern {
        return createInnboksIntern(defaultTekst, defaultSikkerhetsnivaa, eksternVarsling, defaultLink, prefererteKanaler)
    }

    private fun createInnboksIntern(text: String, sikkerhetsnivaa: Int, eksternVarsling: Boolean, link: String, prefererteKanaler: List<String>): InnboksIntern {
        return InnboksIntern(
            Instant.now().toEpochMilli(),
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler
        )
    }
}
