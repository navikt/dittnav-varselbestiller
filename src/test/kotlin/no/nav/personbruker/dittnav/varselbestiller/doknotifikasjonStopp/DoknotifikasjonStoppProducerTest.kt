package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class DoknotifikasjonStoppProducerTest {
    private val producerWrapper: KafkaProducerWrapper<String, DoknotifikasjonStopp> = mockk()
    private val repository: VarselbestillingRepository = mockk()

    private val producer = DoknotifikasjonStoppProducer(producerWrapper, repository)

    private val event = AvroDoknotifikasjonStoppObjectMother.giveMeANumberOfDoknotifikasjonStopp(10).first()

    @AfterEach
    fun cleanup() {
        clearMocks(producerWrapper, repository)
    }

    @Test
    fun `Should commit events to kafka if persisting to database is successful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.cancelVarselbestilling(any()) } returns Unit
        every { producerWrapper.commitCurrentTransaction() } returns Unit


        runBlocking {
            producer.sendEventsAndPersistCancellation(event)
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 1) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 0) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    fun `Should abort kafka transaction kafka if persisting to database is unsuccessful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.cancelVarselbestilling(any()) } throws RetriableDatabaseException("")
        every { producerWrapper.abortCurrentTransaction() } returns Unit

        shouldThrow<RetriableDatabaseException> {
            runBlocking {
                producer.sendEventsAndPersistCancellation(event)
            }
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 1) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    fun `Should not persist events to database if sending events to kafka is unsuccessful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } throws RetriableKafkaException("")
        coEvery { repository.cancelVarselbestilling(any()) } returns Unit
        every { producerWrapper.abortCurrentTransaction() } returns Unit

        shouldThrow<RetriableKafkaException> {
            runBlocking {
                producer.sendEventsAndPersistCancellation(event)
            }
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 0) { repository.cancelVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 1) { producerWrapper.abortCurrentTransaction() }
    }
}
