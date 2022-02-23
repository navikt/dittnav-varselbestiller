package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class DoknotifikasjonStoppProducerTest {
    private val producerWrapper: KafkaProducerWrapper<String, DoknotifikasjonStopp> = mockk()
    private val repository: VarselbestillingRepository = mockk()

    private val producer = DoknotifikasjonStoppProducer(producerWrapper, repository)

    val events = AvroDoknotifikasjonStoppObjectMother.giveMeANumberOfDoknotifikasjonStopp(10)
            .map { it.getBestillingsId() to it }.toMap()

    @AfterEach
    fun cleanup() {
        clearMocks(producerWrapper, repository)
    }

    @Test
    fun `Midlertidig bare lagre, ikke produsere`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.cancelVarselbestilling(any()) } returns Unit
        every { producerWrapper.commitCurrentTransaction() } returns Unit


        runBlocking {
            producer.sendEventsAndPersistCancellation(events)
        }

        verify(exactly = 0) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 0) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    @Disabled
    fun `Should commit events to kafka if persisting to database is successful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.cancelVarselbestilling(any()) } returns Unit
        every { producerWrapper.commitCurrentTransaction() } returns Unit


        runBlocking {
            producer.sendEventsAndPersistCancellation(events)
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 1) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 0) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    @Disabled
    fun `Should abort kafka transaction kafka if persisting to database is unsuccessful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.cancelVarselbestilling(any()) } throws RetriableDatabaseException("")
        every { producerWrapper.abortCurrentTransaction() } returns Unit

        invoking {
            runBlocking {
                producer.sendEventsAndPersistCancellation(events)
            }
        } `should throw` RetriableDatabaseException::class

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 1) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    @Disabled
    fun `Should not persist events to database if sending events to kafka is unsuccessful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } throws RetriableKafkaException("")
        coEvery { repository.cancelVarselbestilling(any()) } returns Unit
        every { producerWrapper.abortCurrentTransaction() } returns Unit

        invoking {
            runBlocking {
                producer.sendEventsAndPersistCancellation(events)
            }
        } `should throw` RetriableKafkaException::class

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 0) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 1) { producerWrapper.abortCurrentTransaction() }
    }
}
