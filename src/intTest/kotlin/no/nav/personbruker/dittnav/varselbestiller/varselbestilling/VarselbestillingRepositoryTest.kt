package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import org.amshove.kluent.`should contain same`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class VarselbestillingRepositoryTest {

    private val database = H2Database()
    private val varselbestillingRepository = VarselbestillingRepository(database)

    private val varselbestilling1 = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001", fodselsnummer = "123")
    private val varselbestilling2 = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-002", eventId = "002", fodselsnummer = "123")
    private val varselbestilling3 = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "O-test-001", eventId = "001", fodselsnummer = "123")

    @BeforeAll
    fun setup() {
        runBlocking {
            database.dbQuery {
                deleteAllVarselbestilling()
            }
        }
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllVarselbestilling()
            }
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for vellykket persistering av Varselbestillinger i batch`() {
        runBlocking {
            val toPersist = listOf(varselbestilling1, varselbestilling2, varselbestilling3)
            val result = varselbestillingRepository.persistInOneBatch(toPersist)
            result.getPersistedEntitites() `should contain same` toPersist
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for persistering i batch hvis noen Varselbestillinger har unique key constraints`() {
        runBlocking {
            val toPersist = listOf(varselbestilling1, varselbestilling2, varselbestilling3)
            val alreadyPersisted = listOf(varselbestilling1, varselbestilling2)
            val expectedPersistResult = toPersist - alreadyPersisted
            varselbestillingRepository.persistInOneBatch(alreadyPersisted)
            val result = varselbestillingRepository.persistInOneBatch(toPersist)
            result.getPersistedEntitites() `should contain same` expectedPersistResult
            result.getConflictingEntities() `should contain same` alreadyPersisted
        }
    }
}
