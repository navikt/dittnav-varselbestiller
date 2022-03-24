package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEvent
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEventRepository
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain all`
import org.amshove.kluent.`should contain same`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EarlyDoneEventRepositoryTest {
    private val database = LocalPostgresDatabase()
    private val earlyDoneEventRepository = EarlyDoneEventRepository(database)

    private val earlyDoneEvent1 = EarlyDoneEvent(eventId = "1", appnavn = "app1", namespace = "ns", fodselsnummer = "1234", systembruker = "s-bruker", tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
    private val earlyDoneEvent2 = EarlyDoneEvent(eventId = "2", appnavn = "app2", namespace = "ns", fodselsnummer = "1234", systembruker = "s-bruker", tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
    private val earlyDoneEvent3 = EarlyDoneEvent(eventId = "1", appnavn = "app3", namespace = "ns", fodselsnummer = "1234", systembruker = "s-bruker", tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))

    @BeforeAll
    fun setup() {
        runBlocking {
            database.dbQuery {
                prepareStatement("""DELETE FROM early_done_event""").use {
                    it.execute()
                }
            }
        }
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            database.dbQuery {
                prepareStatement("""DELETE FROM early_done_event""").use {
                    it.execute()
                }
            }
        }
    }

    @Test
    internal fun `Kan hente EarlyDoneEvent fra database`() {
        runBlocking {
            earlyDoneEventRepository.persistInBatch(listOf(earlyDoneEvent1, earlyDoneEvent2))

            val result = earlyDoneEventRepository.findByEventIds(listOf(earlyDoneEvent1.eventId, earlyDoneEvent2.eventId))

            result.size  `should be equal to` 2
            result `should contain all` listOf(earlyDoneEvent1, earlyDoneEvent2)
        }
    }

    @Test
    fun `Kan persistere en batch som har duplikerte EralyDoneEvents i batch`() {
        runBlocking {
            val toPersist = listOf(earlyDoneEvent2, earlyDoneEvent3)
            val alreadyPersisted = listOf(earlyDoneEvent1)
            val expectedPersistResult = listOf(earlyDoneEvent2)

            earlyDoneEventRepository.persistInBatch(alreadyPersisted)
            val result = earlyDoneEventRepository.persistInBatch(toPersist)
            result.getPersistedEntitites() `should contain same` expectedPersistResult
            result.getConflictingEntities() `should contain same` listOf(earlyDoneEvent3)
        }
    }
}
