package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test

internal class VarselbestillingTest {

    @Test
    fun `skal returnere maskerte data fra toString-metoden`() {
        val varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001", "12345678901")
        val varselbestillingAsString = varselbestilling.toString()
        varselbestillingAsString `should contain` "fodselsnummer=***"
        varselbestillingAsString `should contain` "systembruker=***"
    }
}
