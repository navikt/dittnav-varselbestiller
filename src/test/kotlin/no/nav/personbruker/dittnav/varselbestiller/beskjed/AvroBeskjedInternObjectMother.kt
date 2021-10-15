package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.Instant

object AvroBeskjedInternObjectMother {

    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())

    fun createBeskjedIntern(): BeskjedIntern {
        return createBeskjedIntern(defaultTekst, defaultSikkerhetsnivaa, defaultLink, defaultEksternVarsling, defaultPrefererteKanaler)
    }

    fun createBeskjedWithEksternVarsling(eksternVarsling: Boolean): BeskjedIntern {
        return createBeskjedWithEksternVarslingOgPrefererteKanaler(eksternVarsling, defaultPrefererteKanaler)
    }

    fun createBeskjedWithEksternVarslingOgPrefererteKanaler(eksternVarsling: Boolean, prefererteKanaler: List<String>): BeskjedIntern {
        return createBeskjedIntern(defaultTekst, defaultSikkerhetsnivaa, defaultLink, eksternVarsling, prefererteKanaler)
    }

    fun createBeskjedIntern(text: String, sikkerhetsnivaa: Int, link: String, eksternVarsling: Boolean, prefererteKanaler: List<String>): BeskjedIntern {
        return BeskjedIntern(
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                text,
                link,
                sikkerhetsnivaa,
                eksternVarsling,
                prefererteKanaler
        )
    }
}
