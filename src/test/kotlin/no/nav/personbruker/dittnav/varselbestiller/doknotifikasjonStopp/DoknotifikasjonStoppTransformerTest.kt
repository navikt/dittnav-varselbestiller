package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import org.junit.jupiter.api.Test

class DoknotifikasjonStoppTransformerTest {

    @Test
    fun `Skal transformere fra Varselbestilling til DoknotifikasjonStopp`() {
        val varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001")
        val doknotifikasjonStopp = DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(varselbestilling)
        doknotifikasjonStopp.getBestillingsId() shouldBe varselbestilling.bestillingsId
        doknotifikasjonStopp.getBestillerId() shouldBe varselbestilling.appnavn
    }
}
