package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class VarselbestillingTeardownQueriesTest {

    private val database = LocalPostgresDatabase.cleanDb()

    private val varselbestilling1 = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-001", eventId = "001")
    private val varselbestilling2 = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-002", eventId = "002")

    @BeforeAll
    fun setup() {
        runBlocking {
            deleteAllVarselbestillinger()
        }
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            deleteAllVarselbestillinger()
        }
    }

    @Test
    fun `Skal slette alle rader i varselbestilling-tabellen`() {
        runBlocking {
            createVarselbestillinger(listOf(varselbestilling1, varselbestilling2))
            var result = database.dbQuery { getAllVarselbestilling() }
            result.size shouldBe 2
            deleteAllVarselbestillinger()
            result = database.dbQuery { getAllVarselbestilling() }
            result.isEmpty() shouldBe true
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



