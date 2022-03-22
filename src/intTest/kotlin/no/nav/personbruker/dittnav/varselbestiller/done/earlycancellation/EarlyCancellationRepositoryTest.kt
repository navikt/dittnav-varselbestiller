package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.amshove.kluent.`should contain same`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EarlyCancellationRepositoryTest {
    private val database = LocalPostgresDatabase()
    private val earlyCancellationRepository = EarlyCancellationRepository(database)

    private val earlyCancellation1 = EarlyCancellation(eventId = "1", appnavn = "app1", namespace = "ns", fodselsnummer = "1234", systembruker = "s-bruker", tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
    private val earlyCancellation2 = EarlyCancellation(eventId = "2", appnavn = "app2", namespace = "ns", fodselsnummer = "1234", systembruker = "s-bruker", tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
    private val earlyCancellation3 = EarlyCancellation(eventId = "1", appnavn = "app3", namespace = "ns", fodselsnummer = "1234", systembruker = "s-bruker", tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))

    @BeforeAll
    fun setup() {
        runBlocking {
            database.dbQuery {
                prepareStatement("""DELETE FROM early_cancellation""").use {
                    it.execute()
                }
            }
        }
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                prepareStatement("""DELETE FROM early_cancellation""").use {
                    it.execute()
                }
            }
        }
    }

    @Test
    internal fun `Kan hente EarlyCancellation fra database`() {
        runBlocking {
            earlyCancellationRepository.persistInBatch(listOf(earlyCancellation1, earlyCancellation2))

            val result = earlyCancellationRepository.findByEventIds(listOf(earlyCancellation1.eventId, earlyCancellation2.eventId))

            result.size  `should be equal to` 2
            result `should contain all` listOf(earlyCancellation1, earlyCancellation2)
        }
    }

    @Test
    fun `Kan persistere en batch som har duplikerte EralyCancellations i batch`() {
        runBlocking {
            val toPersist = listOf(earlyCancellation2, earlyCancellation3)
            val alreadyPersisted = listOf(earlyCancellation1)
            val expectedPersistResult = listOf(earlyCancellation2)

            earlyCancellationRepository.persistInBatch(alreadyPersisted)
            val result = earlyCancellationRepository.persistInBatch(toPersist)
            result.getPersistedEntitites() `should contain same` expectedPersistResult
            result.getConflictingEntities() `should contain same` listOf(earlyCancellation3)
        }
    }
}
