package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.sql.SQLException

class varselbestillingQueriesTest {

    private val database = H2Database()

    private val varselbestillingBeskjed: Varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001", fodselsnummer = "123")
    private val varselbestillingOppgave: Varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "O-test-001", eventId = "001", fodselsnummer = "123")

    init {
        createDoknotifikasjoner(listOf(varselbestillingBeskjed, varselbestillingOppgave))
    }

    private fun createDoknotifikasjoner(doknotifikasjoner: List<Varselbestilling>) {
        runBlocking {
            database.dbQuery {
                createVarselbestillinger(doknotifikasjoner)
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
    fun `Kaster Exception hvis Varselbestilling med bestillingsId ikke finnes`() {
        invoking {
            runBlocking {
                database.dbQuery { getVarselbestillingForBestillingsId("idFinnesIkke") }
            }
        } `should throw` SQLException::class `with message` "Found no rows"
    }
}
