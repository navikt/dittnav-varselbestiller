package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeskjedEventServiceTest {

    private val producer = mockk<KafkaProducerWrapper<Beskjed>>(relaxed = true)
    private val metricsProbe = mockk<EventMetricsProbe>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = BeskjedEventService(producer, metricsProbe)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(BeskjedValidation)
        clearMocks(producer)
        clearMocks(metricsProbe)
        clearMocks(metricsSession)
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal skrive alle eventer til ny kafka-topic`() {
        val records = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(5, "dummyTopic")

        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<Beskjed>>>()
       coEvery { producer.sendEvents(capture(capturedListOfEntities)) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        verify(exactly = records.count()) { BeskjedValidation.validateEvent(any(), any()) }
        coVerify(exactly = 1) { producer.sendEvents(allAny()) }
        capturedListOfEntities.captured.size `should be` records.count()

        confirmVerified(BeskjedValidation)
        confirmVerified(producer)
    }
/* //TODO
    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en, for det til slutt kastes en exception`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val records = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(totalNumberOfRecords, "dummyTopic")

        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<Beskjed>>>()
       coEvery { producer.sendEvents(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationExp = FieldValidationException("Simulert feil i en test")
        every { BeskjedValidation.validateEvent(any(), any()) } throws fieldValidationExp

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                eventService.processEvents(records)
            }
        } `should throw` FieldValidationException::class

        coVerify(exactly = totalNumberOfRecords) { BeskjedValidation.validateEvent(any(), any()) }
        coVerify(exactly = 1) { producer.sendEvents(allAny()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForProducer(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(BeskjedValidation)
        confirmVerified(producer)
    }
*/
    @Test
    fun shouldReportEverySuccessfulEvent() {
        val numberOfRecords = 5

        val records = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords, "beskjed")
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify (exactly = numberOfRecords) { metricsSession.countSuccessfulEventForProducer(any()) }
    }

    @Test
    fun `skal forkaste eventer som mangler tekst i tekstfeltet`() {
        val beskjedWithoutText = AvroBeskjedObjectMother.createBeskjedWithText("")
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("beskjed", beskjedWithoutText)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedNumberOfEntitiesWrittenToTheDb = slot<List<RecordKeyValueWrapper<Beskjed>>>()
        coEvery { producer.sendEvents(capture(capturedNumberOfEntitiesWrittenToTheDb)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntitiesWrittenToTheDb.captured.size `should be` 0

        coVerify (exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }

    @Test
    fun `skal forkaste eventer som har valideringsfeil`() {
        val tooLongText = "A".repeat(501)
        val beskjedWithTooLongText = AvroBeskjedObjectMother.createBeskjedWithText(tooLongText)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("beskjed", beskjedWithTooLongText)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedNumberOfEntitiesWrittenToTheDb = slot<List<RecordKeyValueWrapper<Beskjed>>>()
        coEvery { producer.sendEvents(capture(capturedNumberOfEntitiesWrittenToTheDb)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntitiesWrittenToTheDb.captured.size `should be` 0

        coVerify (exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }

}
