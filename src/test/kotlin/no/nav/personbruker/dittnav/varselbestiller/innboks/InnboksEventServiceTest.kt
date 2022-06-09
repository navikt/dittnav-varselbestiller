package no.nav.personbruker.dittnav.varselbestiller.innboks

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections

class InnboksEventServiceTest {

    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = InnboksEventService(doknotifikasjonProducer, varselbestillingRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonCreator)
        clearMocks(doknotifikasjonProducer)
        clearMocks(varselbestillingRepository)
        clearMocks(metricsCollector)
        clearMocks(metricsSession)
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(allAny()) } returns Collections.emptyList()
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal opprette Doknotifikasjon for alle eventer som har ekstern varsling`() {
        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 4, topicName = "innboksTopic", withEksternVarsling = true)
        val innboksWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 6, topicName = "innboksTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<Doknotifikasjon>>()

        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(any()) } returns Collections.emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), capture(capturedListOfEntities)) } returns Unit
        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
            eventService.processEvents(innboksWithoutEksternVarslingRecords)
        }

        verify(exactly = innboksWithEksternVarslingRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(NokkelIntern::class), ofType(InnboksIntern::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistBestillingBatch(allAny(), any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count() + innboksWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size shouldBe innboksWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal ikke opprette Doknotifikasjon for eventer som har tidligere bestilt ekstern varsling`() {
        val innboksRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 5, topicName = "innboksTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<Doknotifikasjon>>()

        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(any()) } returns listOf(
            VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "I-dummyAppnavn-1", eventId = "1"))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(innboksRecords)
        }

        verify(exactly = innboksRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(NokkelIntern::class), ofType(InnboksIntern::class)) }
        coVerify(exactly = innboksRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = 1 ) { metricsSession.countDuplicateVarselbestillingForProducer(any()) }
        coVerify(exactly = innboksRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), any()) }
        capturedListOfEntities.captured.size shouldBe 4
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Innboks som har ekstern varsling`() {
        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 4, topicName = "innboksTopic", withEksternVarsling = true)
        val innboksWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 6, topicName = "innboksTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<Doknotifikasjon>>()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(any()) } returns Collections.emptyList()
        coEvery { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
            eventService.processEvents(innboksWithoutEksternVarslingRecords)
        }

        coVerify(exactly = innboksWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count() + innboksWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size shouldBe innboksWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte transformeringer feiler og fortsette aa transformere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val innboksRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = totalNumberOfRecords, topicName = "innboksTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<Doknotifikasjon>>()
        coEvery { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = UntransformableRecordException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(numberOfSuccessfulTransformations)
        every { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(NokkelIntern::class), ofType(InnboksIntern::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(any()) } returns emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        shouldThrow<UntransformableRecordException> {
            runBlocking {
                eventService.processEvents(innboksRecords)
            }
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations + numberOfFailedTransformations) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size shouldBe numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }


    @Test
    fun `Skal rapportere hvert velykket event`() {
        val numberOfRecords = 5

        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords, topicName = "innboksTopic", withEksternVarsling = true)
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { doknotifikasjonProducer.sendAndPersistBestillingBatch(any(), any()) } returns Unit
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(any()) } returns emptyList()

        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
        }

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
    }
}
