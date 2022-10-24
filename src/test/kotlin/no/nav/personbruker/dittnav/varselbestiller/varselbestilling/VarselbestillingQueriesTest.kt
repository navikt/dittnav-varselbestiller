package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class VarselbestillingQueriesTest {

    private val database = LocalPostgresDatabase.cleanDb()

    private val varselbestillingBeskjed: Varselbestilling = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-001", eventId = "001")
    private val varselbestillingOppgave: Varselbestilling = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "O-test-001", eventId = "002")

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
            result.size shouldBe 2
            result shouldBe listOf(varselbestillingBeskjed, varselbestillingOppgave)
        }
    }

    @Test
    fun `Returnerer tom liste hvis Varselbestilling med bestillingsId ikke finnes`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForBestillingsIds(listOf("idFinnesIkke")) }
            result.shouldBeEmpty()
        }
    }

    @Test
    fun `Finner Varselbestillinger med eventIds`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForEventIds(listOf(varselbestillingBeskjed.eventId, varselbestillingOppgave.eventId)) }
            result.size shouldBe 2
            result shouldBe listOf(varselbestillingBeskjed, varselbestillingOppgave)
        }
    }

    @Test
    fun `Finner varselbestilling for en eventId`(){
        runBlocking {
            database.dbQuery {
                getVarselbestillingForEventId(varselbestillingBeskjed.eventId)
            } shouldBe varselbestillingBeskjed
        }
    }

    @Test
    fun `Returnerer tom liste hvis Varselbestilling med eventId ikke finnes`() {
        runBlocking {
            val result = database.dbQuery { getVarselbestillingerForEventIds(listOf("idFinnesIkke")) }
            result.shouldBeEmpty()
        }
    }

    @Test
    fun `Setter avbestilt for Varselbestilling`() {
        runBlocking {
            database.dbQuery {
                setVarselbestillingAvbestiltFlag(listOf(varselbestillingBeskjed.bestillingsId), true)
                val result = getVarselbestillingerForBestillingsIds(listOf(varselbestillingBeskjed.bestillingsId))
                result.first().bestillingsId shouldBe varselbestillingBeskjed.bestillingsId
                result.first().avbestilt shouldBe true
            }
        }
    }

    @Test
    fun `Persister ikke entitet dersom rad med samme bestillingsId finnes`() {
        runBlocking {
            database.dbQuery {
                val numberOfEntities = getAllVarselbestilling().size
                createVarselbestillinger(listOf(varselbestillingBeskjed, varselbestillingOppgave))
                getAllVarselbestilling().size shouldBe numberOfEntities
            }
        }
    }

    @Test
    fun `Skal haantere at prefererteKanaler er tom`() {
        val varselbestilling = VarselbestillingObjectMother.createVarselbestillingWithPrefererteKanaler(prefererteKanaler = emptyList())
        runBlocking {
            database.dbQuery { createVarselbestillinger(listOf(varselbestilling)) }
            val result = database.dbQuery { getVarselbestillingerForBestillingsIds(listOf(varselbestilling.bestillingsId)) }
            result.size shouldBe 1
            result shouldBe listOf(varselbestilling)
            result[0].prefererteKanaler.shouldBeEmpty()
        }
    }
}
