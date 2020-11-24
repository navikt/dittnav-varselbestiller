package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class DoknotifikasjonStoppTransformerTest {

    @Test
    fun `Skal transformere fra Varselbestilling til DoknotifikasjonStopp`() {
        val varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001", fodselsnummer = "12345")
        val doknotifikasjonStopp = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
        doknotifikasjonStopp.getBestillingsId() `should be equal to` varselbestilling.bestillingsId
        doknotifikasjonStopp.getBestillerId() `should be equal to` varselbestilling.systembruker
    }
}
