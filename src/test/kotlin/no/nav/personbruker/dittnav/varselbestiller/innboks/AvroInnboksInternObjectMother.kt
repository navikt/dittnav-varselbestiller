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
    private val defaultEpostVarslingstekst: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createInnboksIntern(
        text: String = defaultTekst,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        eksternVarsling: Boolean = defaultEksternVarsling,
        link: String = defaultLink,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): InnboksIntern {
        return InnboksIntern(
            Instant.now().toEpochMilli(),
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            smsVarslingstekst
        )
    }

    fun createInnboksInternWithEksternVarsling(eksternVarsling: Boolean): InnboksIntern {
        return createInnboksIntern(
            eksternVarsling = eksternVarsling,
        )
    }

    fun createInnboksInternWithEksternVarslingOgPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): InnboksIntern {
        return createInnboksIntern(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }

}
