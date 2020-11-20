package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.*
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class VarselbestillingTeardownQueriesTest {

    private val database = H2Database()

    private val varselbestilling1 = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001", fodselsnummer = "123")
    private val varselbestilling2 = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-002", eventId = "002", fodselsnummer = "123")
    private val varselbestilling3 = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "O-test-001", eventId = "001", fodselsnummer = "234")

    @Test
    fun `Skal slette alle rader i doknotifikasjon-tabellen`() {
        runBlocking {
            createVarselbestillinger(listOf(varselbestilling1, varselbestilling2, varselbestilling3))
            var result = database.dbQuery { getAllVarselbestilling() }
            result.size `should be equal to` 3
            deleteAllVarselbestillinger()
            result = database.dbQuery { getAllVarselbestilling() }
            result.isEmpty() `should be equal to` true
        }
    }

    private suspend fun createVarselbestillinger(varselbestillinger: List<Varselbestilling>) {
        database.dbQuery {
            createVarselbestillinger(varselbestillinger)
        }
    }

    private suspend fun deleteAllVarselbestillinger() {
        database.dbQuery {
            deleteAllVarselbestilling()
        }
    }
}



