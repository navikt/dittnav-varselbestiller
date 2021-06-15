package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class varselbestillingQueriesTest {

    private val database = H2Database()

    private val varselbestillingBeskjed: Varselbestilling = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-001", eventId = "001")
    private val varselbestillingOppgave: Varselbestilling = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "O-test-001", eventId = "001")

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
    fun `Finner Varselbestillinger med bestillingsIds`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForBestillingsIds(listOf(varselbestillingBeskjed.bestillingsId, varselbestillingOppgave.bestillingsId)) }
            result.size `should be equal to` 2
            result `should contain all` listOf(varselbestillingBeskjed, varselbestillingOppgave)
        }
    }

    @Test
    fun `Returnerer tom liste hvis Varselbestilling med bestillingsId ikke finnes`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForBestillingsIds(listOf("idFinnesIkke")) }
            result.`should be empty`()
        }
    }

    @Test
    fun `Finner Varselbestillinger med eventIds`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForEventIds(listOf(varselbestillingBeskjed.eventId, varselbestillingOppgave.eventId)) }
            result.size `should be equal to` 2
            result `should contain all` listOf(varselbestillingBeskjed, varselbestillingOppgave)
        }
    }

    @Test
    fun `Returnerer tom liste hvis Varselbestilling med eventId ikke finnes`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForEventIds(listOf("idFinnesIkke")) }
            result.`should be empty`()
        }
    }

    @Test
    fun `Setter avbestilt for Varselbestilling`() {
        runBlocking {
            database.dbQuery {
                setVarselbestillingAvbestiltFlag(listOf(varselbestillingBeskjed.bestillingsId), true)
                val result = getVarselbestillingerForBestillingsIds(listOf(varselbestillingBeskjed.bestillingsId))
                result.first().bestillingsId `should be equal to` varselbestillingBeskjed.bestillingsId
                result.first().avbestilt `should be equal to` true
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

    @Test
    fun `Skal haantere at prefererteKanaler er tom`() {
        val varselbestilling = VarselbestillingObjectMother.createVarselbestillingWithPrefererteKanaler(prefererteKanaler = emptyList())
        runBlocking {
            database.dbQuery { createVarselbestillinger(listOf(varselbestilling)) }
            val result = database.dbQuery { getVarselbestillingerForBestillingsIds(listOf(varselbestilling.bestillingsId)) }
            result.size `should be equal to` 1
            result `should contain all` listOf(varselbestilling)
            result[0].prefererteKanaler.`should be empty`()
        }
    }
}
