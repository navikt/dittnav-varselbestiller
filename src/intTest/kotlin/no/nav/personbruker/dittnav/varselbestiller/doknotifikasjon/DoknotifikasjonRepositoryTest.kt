package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import org.amshove.kluent.`should contain same`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DoknotifikasjonRepositoryTest {

    private val database = H2Database()
    private val doknotifikasjonRepository = DoknotifikasjonRepository(database)

    private val doknotifikasjon1 = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("B-test-001", Eventtype.BESKJED)
    private val doknotifikasjon2 = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("B-test-002", Eventtype.BESKJED)
    private val doknotifikasjon3 = DoknotifikasjonObjectMother.giveMeDoknotifikasjon("O-test-001", Eventtype.OPPGAVE)

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                deleteAllDoknotifikasjon()
            }
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for vellykket persistering av Doknotifikasjoner i batch`() {
        runBlocking {
            val toPersist = listOf(doknotifikasjon1, doknotifikasjon2, doknotifikasjon3)
            val result = doknotifikasjonRepository.createInOneBatch(toPersist)
            result.getPersistedEntitites() `should contain same` toPersist
        }
    }

    @Test
    fun `Skal returnere korrekt resultat for persistering hvis noen Doknotifikasjoner har unique key constraints`() {
        runBlocking {
            val toPersist = listOf(doknotifikasjon1, doknotifikasjon2, doknotifikasjon3)
            val alreadyPersisted = listOf(doknotifikasjon1, doknotifikasjon2)
            val expectedPersistResult = toPersist - alreadyPersisted
            doknotifikasjonRepository.createInOneBatch(alreadyPersisted)
            val result = doknotifikasjonRepository.createInOneBatch(toPersist)
            result.getPersistedEntitites() `should contain same` expectedPersistResult
            result.getConflictingEntities() `should contain same` alreadyPersisted
        }
    }
}
