package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import no.nav.personbruker.dittnav.varselbestiller.common.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.varsel.*
import org.junit.jupiter.api.Test

class DoknotifikasjonCreatorTest {

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Varsel ikke støttes av Doknotifikasjon`() {
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromVarsel(
                Varsel(
                    type = VarselType.Beskjed,
                    produsent = Produsent("", ""),
                    varselId = "",
                    ident = "",
                    sensitivitet = Sensitivitet.High,
                    eksternVarslingBestilling = EksternVarslingBestilling(
                        prefererteKanaler = listOf("UgyldigKanal"),
                        smsVarslingstekst = null,
                        epostVarslingstekst = null,
                        epostVarslingstittel = null
                    )
                )
            )
        }.message shouldContain "preferert kanal"
    }
}
