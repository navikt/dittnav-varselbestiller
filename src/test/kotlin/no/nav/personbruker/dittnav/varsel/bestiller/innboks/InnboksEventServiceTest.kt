package no.nav.personbruker.dittnav.varsel.bestiller.innboks

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.BrukernotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InnboksEventServiceTest {

    private val persistingService = mockk<BrukernotifikasjonProducer<Innboks>>(relaxed = true)
    private val metricsProbe = mockk<EventMetricsProbe>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = InnboksEventService(persistingService, metricsProbe)

    @BeforeEach
    fun resetMocks() {
        mockkObject(InnboksTransformer)
        clearMocks(persistingService)
        clearMocks(metricsProbe)
        clearMocks(metricsSession)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Should finish processing batch before throwing exception when unable to transform event`() {
        val numberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = numberOfRecords - numberOfFailedTransformations

        val records = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords, "dummyTopic")
        val transformedRecords = createANumberOfTransformedInnboksRecords(numberOfSuccessfulTransformations)

        val capturedStores = slot<List<Innboks>>()

        coEvery { persistingService.sendToKafka(capture(capturedStores)) } returns Unit

        val mockedException = UntransformableRecordException("Simulated Exception")

        every { InnboksTransformer.toInternal(any(), any()) } throws mockedException andThenMany transformedRecords

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                eventService.processEvents(records)
            }
        } `should throw` UntransformableRecordException::class

        verify(exactly = numberOfRecords) { InnboksTransformer.toInternal(any(), any()) }
        coVerify(exactly = 1) { persistingService.sendToKafka(allAny()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForProducer(any()) }
        capturedStores.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(persistingService)
        confirmVerified(InnboksTransformer)
    }

    @Test
    fun shouldReportEverySuccessfulEvent() {
        val numberOfRecords = 5

        val records = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords, "innboks")

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEventForProducer(any()) }
    }

    @Test
    fun `skal forkaste eventer som har valideringsfeil`() {
        val tooLongText = "A".repeat(501)
        val innboksWithTooLongText = AvroInnboksObjectMother.createInnboksWithText(tooLongText)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("innboks", innboksWithTooLongText)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedNumberOfEntitiesWrittenToTheDb = slot<List<Innboks>>()
        coEvery { persistingService.sendToKafka(capture(capturedNumberOfEntitiesWrittenToTheDb)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntitiesWrittenToTheDb.captured.size `should be` 0

        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }


    private fun createANumberOfTransformedInnboksRecords(number: Int): List<Innboks> {
        return (1..number).map {
            InnboksObjectMother.giveMeAktivInnboks(it.toString(), "12345")
        }
    }

}