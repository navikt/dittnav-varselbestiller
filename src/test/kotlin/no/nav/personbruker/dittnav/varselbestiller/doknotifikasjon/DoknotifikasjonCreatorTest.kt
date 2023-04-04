package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import no.nav.personbruker.dittnav.varselbestiller.common.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.varsel.Varsel
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselType
import org.junit.jupiter.api.Test

class DoknotifikasjonCreatorTest {

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Varsel ikke støttes av Doknotifikasjon`() {
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromVarsel(
                Varsel(
                    varselType = VarselType.BESKJED,
                    namespace = "",
                    appnavn = "",
                    eventId = "",
                    fodselsnummer = "",
                    sikkerhetsnivaa = 4,
                    eksternVarsling = true,
                    prefererteKanaler = listOf("UgyldigKanal"),
                    smsVarslingstekst = null,
                    epostVarslingstekst = null,
                    epostVarslingstittel = null
                )
            )
        }.message shouldContain "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Varsel`() {
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromVarsel(
                Varsel(
                    varselType = VarselType.BESKJED,
                    namespace = "",
                    appnavn = "",
                    eventId = "",
                    fodselsnummer = "",
                    sikkerhetsnivaa = 4,
                    eksternVarsling = false,
                    prefererteKanaler = listOf("SMS"),
                    smsVarslingstekst = null,
                    epostVarslingstekst = null,
                    epostVarslingstittel = null
                )
            )
        }.message shouldContain "Prefererte kanaler"
    }
}
