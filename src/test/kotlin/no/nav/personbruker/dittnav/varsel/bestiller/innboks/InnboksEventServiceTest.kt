package no.nav.personbruker.dittnav.varsel.bestiller.innboks

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InnboksEventServiceTest {

    private val kafkaProducer = mockk<KafkaProducerWrapper<Innboks>>(relaxed = true)
    private val metricsProbe = mockk<EventMetricsProbe>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = InnboksEventService(kafkaProducer, metricsProbe)

    @BeforeEach
    fun resetMocks() {
        mockkObject(InnboksValidation)
        clearMocks(kafkaProducer)
        clearMocks(metricsProbe)
        clearMocks(metricsSession)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    /*
    @Test
    fun `Should finish processing batch before throwing exception when unable to transform event`() {
        val numberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = numberOfRecords - numberOfFailedTransformations

        val records = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords, "dummyTopic")
        val transformedRecords = createANumberOfTransformedInnboksRecords(numberOfSuccessfulTransformations)

        val capturedStores = slot<List<RecordKeyValueWrapper<Innboks>>>()

        coEvery { kafkaProducer.sendEvents(capture(capturedStores)) } returns Unit

        val mockedException = UntransformableRecordException("Simulated Exception")

        every { InnboksValidation.validateEvent(any(), any()) } throws mockedException andThenMany transformedRecords

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                eventService.processEvents(records)
            }
        } `should throw` UntransformableRecordException::class

        verify(exactly = numberOfRecords) { InnboksValidation.toInternal(any(), any()) }
        coVerify(exactly = 1) { persistingService.sendToKafka(allAny()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForProducer(any()) }
        capturedStores.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(persistingService)
        confirmVerified(InnboksValidation)
    }

     */

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

        val capturedNumberOfEntitiesWrittenToTheDb = slot<List<RecordKeyValueWrapper<Innboks>>>()
        coEvery { kafkaProducer.sendEvents(capture(capturedNumberOfEntitiesWrittenToTheDb)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntitiesWrittenToTheDb.captured.size `should be` 0

        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }

}