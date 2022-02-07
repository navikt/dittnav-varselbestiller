package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.Instant
import java.time.temporal.ChronoUnit

object AvroBeskjedInternObjectMother {

    private val defaultTekst = "Dette er Beskjed til brukeren"
    private val defaultSikkerhetsnivaa = 4
    private val defaultEksternVarsling = true
    private val defaultLink = "http://dummyUrl.no"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString())
    private val defaultSynligFremTil = Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli()
    private val defaultEpostVarslingstekst: String? = null
    private val defaultEpostVarslingstittel: String? = null
    private val defaultSmsVarslingstekst: String? = null

    fun createBeskjedIntern(
        text: String = defaultTekst,
        synligFremTil: Long? = defaultSynligFremTil,
        sikkerhetsnivaa: Int = defaultSikkerhetsnivaa,
        link: String = defaultLink,
        eksternVarsling: Boolean = defaultEksternVarsling,
        prefererteKanaler: List<String> = defaultPrefererteKanaler,
        epostVarslingstekst: String? = defaultEpostVarslingstekst,
        epostVarslingstittel: String? = defaultEpostVarslingstittel,
        smsVarslingstekst: String? = defaultSmsVarslingstekst
    ): BeskjedIntern {
        return BeskjedIntern(
            Instant.now().toEpochMilli(),
            synligFremTil,
            text,
            link,
            sikkerhetsnivaa,
            eksternVarsling,
            prefererteKanaler,
            epostVarslingstekst,
            epostVarslingstittel,
            smsVarslingstekst
        )
    }

    fun createBeskjedInternWithEksternVarsling(eksternVarsling: Boolean): BeskjedIntern {
        return createBeskjedInternWithEksternVarslingOgPrefererteKanaler(eksternVarsling, defaultPrefererteKanaler)
    }

    fun createBeskjedInternWithEksternVarslingOgPrefererteKanaler(
        eksternVarsling: Boolean,
        prefererteKanaler: List<String>
    ): BeskjedIntern {
        return createBeskjedIntern(
            eksternVarsling = eksternVarsling,
            prefererteKanaler = prefererteKanaler
        )
    }

    fun createBeskjedWithEpostVarslingstekst(epostVarslingstekst: String): BeskjedIntern {
        return createBeskjedIntern(
            epostVarslingstekst = epostVarslingstekst
        )
    }

    fun createBeskjedWithSmsVarslingstekst(smsVarslingstekst: String): BeskjedIntern {
        return createBeskjedIntern(
            smsVarslingstekst = smsVarslingstekst
        )
    }
}
