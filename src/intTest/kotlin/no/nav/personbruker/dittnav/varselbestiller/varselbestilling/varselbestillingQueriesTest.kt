package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class varselbestillingQueriesTest {

    private val database = H2Database()

    private val varselbestillingBeskjed: Varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001", fodselsnummer = "123")
    private val varselbestillingOppgave: Varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "O-test-001", eventId = "001", fodselsnummer = "123")

    init {
        createVarselbestillinger(listOf(varselbestillingBeskjed, varselbestillingOppgave))
    }

    private fun createVarselbestillinger(varselbestillinger: List<Varselbestilling>) {
        runBlocking {
            database.dbQuery {
                createVarselbestillinger(varselbestillinger)
            }
        }
    }

    @AfterAll
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllVarselbestilling()
            }
        }
    }

    @Test
    fun `Finner Varselbestilling med bestillingsId`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingForBestillingsId(varselbestillingBeskjed.bestillingsId) }
            result `should be equal to` varselbestillingBeskjed
        }
    }

    @Test
    fun `Returnerer null hvis Varselbestilling med bestillingsId ikke finnes`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingForBestillingsId("idFinnesIkke") }
            result.`should be null`()
        }
    }

    @Test
    fun `Finner Varselbestilling med eventId`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingForEvent(varselbestillingBeskjed.eventId, varselbestillingBeskjed.systembruker, varselbestillingBeskjed.fodselsnummer) }
            result `should be equal to` varselbestillingBeskjed
        }
    }

    @Test
    fun `Returnerer null hvis Varselbestilling med eventId ikke finnes`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingForEvent("idFinnesIkke", varselbestillingBeskjed.systembruker, varselbestillingBeskjed.fodselsnummer) }
            result.`should be null`()
        }
    }

    @Test
    fun `Setter avbestilt for Varselbestilling`() {
        runBlocking {
            database.dbQuery {
                setVarselbestillingAvbestiltFlag(listOf(varselbestillingBeskjed), true)
                val varselbestilling = getVarselbestillingForBestillingsId(varselbestillingBeskjed.bestillingsId)
                varselbestilling?.avbestilt `should be equal to` true
            }
        }
    }

    @Test
    fun `Persister ikke entitet dersom rad med samme bestillingsId finnes`() {
        runBlocking {
            database.dbQuery {
                val numberOfEntities = getAllVarselbestilling().size
                createVarselbestillinger(listOf(varselbestillingBeskjed, varselbestillingOppgave))
                getAllVarselbestilling().size `should be equal to` numberOfEntities
            }
        }
    }
}
