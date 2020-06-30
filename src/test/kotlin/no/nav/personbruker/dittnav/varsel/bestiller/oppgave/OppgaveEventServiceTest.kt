package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveEventServiceTest {

    private val kafkaProducer = mockk<KafkaProducerWrapper<Oppgave>>(relaxed = true)
    private val metricsProbe = mockk<EventMetricsProbe>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = OppgaveEventService(kafkaProducer, metricsProbe)

    @BeforeEach
    fun resetMocks() {
        mockkObject(OppgaveValidation)
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

        val records = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords, "dummyTopic")
        val transformedRecords = createANumberOfTransformedOppgaveRecords(numberOfSuccessfulTransformations)

        val capturedStores = slot<List<Oppgave>>()

        coEvery { persistingService.sendToKafka(capture(capturedStores)) } returns Unit

        val mockedException = UntransformableRecordException("Simulated Exception")

        every { OppgaveValidation.toInternal(any(), any()) } throws mockedException andThenMany transformedRecords

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                eventService.processEvents(records)
            }
        } `should throw` UntransformableRecordException::class

        verify(exactly = numberOfRecords) { OppgaveValidation.toInternal(any(), any()) }
        coVerify(exactly = 1) { persistingService.sendToKafka(allAny()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForProducer(any()) }
        capturedStores.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(persistingService)
        confirmVerified(OppgaveValidation)
    }


 */
    @Test
    fun shouldRegisterMetricsForEveryEvent() {
        val numberOfRecords = 5

        val records = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords, "oppgave")

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
        val oppgaveWithTooLongText = AvroOppgaveObjectMother.createOppgave(tooLongText)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("oppgave", oppgaveWithTooLongText)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedNumberOfEntitiesWrittenToTheDb =  slot<List<RecordKeyValueWrapper<Oppgave>>>()
        coEvery { kafkaProducer.sendEvents(capture(capturedNumberOfEntitiesWrittenToTheDb)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntitiesWrittenToTheDb.captured.size `should be` 0

        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }

}