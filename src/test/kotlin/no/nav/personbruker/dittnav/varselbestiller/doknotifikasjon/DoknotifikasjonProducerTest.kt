package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class DoknotifikasjonProducerTest {
    private val producerWrapper: KafkaProducerWrapper<String, Doknotifikasjon> = mockk()
    private val repository: VarselbestillingRepository = mockk()

    private val producer = DoknotifikasjonProducer(producerWrapper, repository)

    private val event = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(10).first()

    private val varselBestilling = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-dummy-001", "001")

    @AfterEach
    fun cleanup() {
        clearMocks(producerWrapper, repository)
    }

    @Test
    fun `Should commit events to kafka if persisting to database is successful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.persistVarselbestilling(any()) } returns 1
        every { producerWrapper.commitCurrentTransaction() } returns Unit

        runBlocking {
            producer.sendAndPersistBestilling(varselBestilling, event)
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.persistVarselbestilling(any()) }
        verify(exactly = 1) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 0) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    fun `Should abort kafka transaction kafka if persisting to database is unsuccessful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } returns Unit
        coEvery { repository.persistVarselbestilling(any()) } throws RetriableDatabaseException("")
        every { producerWrapper.abortCurrentTransaction() } returns Unit

        shouldThrow<RetriableDatabaseException> {
            runBlocking {
                producer.sendAndPersistBestilling(varselBestilling, event)
            }
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 1) { repository.persistVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 1) { producerWrapper.abortCurrentTransaction() }
    }

    @Test
    fun `Should not persist events to database if sending events to kafka is unsuccessful`() {
        every { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) } throws RetriableKafkaException("")
        coEvery { repository.persistVarselbestilling(any()) } returns 1
        every { producerWrapper.abortCurrentTransaction() } returns Unit

        shouldThrow<RetriableKafkaException> {
            runBlocking {
                producer.sendAndPersistBestilling(varselBestilling, event)
            }
        }

        verify(exactly = 1) { producerWrapper.sendEventsAndLeaveTransactionOpen(any()) }
        coVerify(exactly = 0) { repository.persistVarselbestilling(any()) }
        verify(exactly = 0) { producerWrapper.commitCurrentTransaction() }
        verify(exactly = 1) { producerWrapper.abortCurrentTransaction() }
    }
}
