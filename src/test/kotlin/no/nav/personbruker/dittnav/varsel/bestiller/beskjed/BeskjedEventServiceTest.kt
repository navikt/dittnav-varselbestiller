package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.createKeyForEvent
import no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsSession
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class BeskjedEventServiceTest {

    private val producer = mockk<KafkaProducerWrapper<Beskjed>>(relaxed = true)
    private val metricsProbe = mockk<EventMetricsProbe>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = BeskjedEventService(producer, metricsProbe)
    private val log = LoggerFactory.getLogger(BeskjedEventServiceTest::class.java)

    @BeforeEach
    private fun resetMocks() {
        clearMocks(producer)
        clearMocks(metricsProbe)
        clearMocks(metricsSession)
        mockkStatic("no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.VarselKeyCreatorKt")
        mockkStatic("no.nav.personbruker.dittnav.varsel.bestiller.beskjed.BeskjedEksternVarslingCreatorKt")
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `skal forkaste eventer som mangler fodselsnummer`() {
        val beskjedWithoutFodselsnummer = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer("")
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("beskjed", beskjedWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedNumberOfEntities = slot<List<RecordKeyValueWrapper<Beskjed>>>()
        coEvery { producer.sendEvents(capture(capturedNumberOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntities.captured.size `should be` 0

        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }

    @Test
    fun `skal forkaste eventer som har feil sikkerhetsnivaa`() {
        val tooLowSecurityLevel = 2
        val beskjedWithTooLowSecurityLevel = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(tooLowSecurityLevel)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("beskjed", beskjedWithTooLowSecurityLevel)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedNumberOfEntities = slot<List<RecordKeyValueWrapper<Beskjed>>>()
        coEvery { producer.sendEvents(capture(capturedNumberOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntities.captured.size `should be` 0

        coVerify(exactly = 1) { metricsSession.countFailedEventForProducer(any()) }
    }

    @Test
    fun `Skal skrive alle eventer til ny kafka-topic`() {
        val records = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(5, "dummyTopic")
        val successfullKeys = mutableListOf<Nokkel>()
        val successfullEvents = mutableListOf<Beskjed>()

        records.forEach { record -> successfullKeys.add(record.key()) }
        records.forEach { record -> successfullEvents.add(record.value()) }

        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<Beskjed>>>()
        coEvery { createKeyForEvent(any()) } returnsMany successfullKeys
        coEvery { createBeskjedEksternVarslingForEvent(any()) } returnsMany successfullEvents
        coEvery { producer.sendEvents(capture(capturedListOfEntities)) } returns Unit

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        verify(exactly = records.count()) { createKeyForEvent(any()) }
        verify(exactly = records.count()) { createBeskjedEksternVarslingForEvent(any()) }
        coVerify(exactly = 1) { producer.sendEvents(allAny()) }
        capturedListOfEntities.captured.size `should be` records.count()

        confirmVerified(producer)
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val records = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(totalNumberOfRecords, "dummyTopic")
        val successfullKey = mutableListOf<Nokkel>()
        val successfullEvents = mutableListOf<Beskjed>()

        records.forEach { record -> successfullKey.add(record.key()) }
        records.forEach { record -> successfullEvents.add(record.value()) }

        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<Beskjed>>>()
        coEvery { producer.sendEvents(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        every { createKeyForEvent(any()) } returnsMany successfullKey
        every { createBeskjedEksternVarslingForEvent(any()) } throws fieldValidationException andThenMany successfullEvents

        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        coEvery { metricsProbe.runWithMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { producer.sendEvents(allAny()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForProducer(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(producer)
    }

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

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEventForProducer(any()) }
    }


}
