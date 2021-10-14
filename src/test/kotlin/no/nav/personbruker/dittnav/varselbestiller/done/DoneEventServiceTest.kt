package no.nav.personbruker.dittnav.varselbestiller.done

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.AvroDoknotifikasjonStoppObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DoneEventServiceTest {

    private val doknotifikasjonStoppProducer = mockk<DoknotifikasjonStoppProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = DoneEventService(doknotifikasjonStoppProducer, varselbestillingRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonStoppTransformer)
        clearMocks(doknotifikasjonStoppProducer)
        clearMocks(varselbestillingRepository)
        clearMocks(metricsCollector)
        clearMocks(metricsSession)
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal opprette DoknotifikasjonStopp for alle eventer som har ekstern varsling`() {
        val doneEventId1 = "001"
        val doneEventId2 = "002"
        val doneEventId3 = "003"
        val doneConsumerRecord1 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId1), actualEvent = AvroDoneInternObjectMother.createDoneIntern())
        val doneConsumerRecord2 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId2), actualEvent =  AvroDoneInternObjectMother.createDoneIntern())
        val doneConsumerRecord3 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName ="done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId3), actualEvent = AvroDoneInternObjectMother.createDoneIntern())
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(listOf(doneConsumerRecord1, doneConsumerRecord2, doneConsumerRecord3))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        val capturedListOfEntities = slot<Map<String, DoknotifikasjonStopp>>()
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(listOf(doneEventId1, doneEventId2, doneEventId3)) } returns
                listOf(VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-dummyAppnavn-001", doneEventId1),
                        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-dummyAppnavn-002", doneEventId2),
                        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-dummyAppnavn-003", doneEventId3))
        coEvery { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(any()) }
        coVerify (exactly = 3) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify (exactly = 3) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` records.count()

        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal opprette DoknotifikasjonStopp kun for eventer som har ekstern varsling`() {
        val doneEventId1 = "001"
        val doneEventId2 = "002"
        val doneEventId3 = "003"
        val doneConsumerRecord1 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId1), actualEvent = AvroDoneInternObjectMother.createDoneIntern())
        val doneConsumerRecord2 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId2), actualEvent =  AvroDoneInternObjectMother.createDoneIntern())
        val doneConsumerRecord3 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName ="done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId3), actualEvent = AvroDoneInternObjectMother.createDoneIntern())
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(listOf(doneConsumerRecord1, doneConsumerRecord2, doneConsumerRecord3))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val capturedListOfEntities = slot<Map<String, DoknotifikasjonStopp>>()
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(listOf(doneEventId1, doneEventId2, doneEventId3)) } returns
                listOf(VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-dummyAppnavn-001", doneEventId1),
                        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-dummyAppnavn-002", doneEventId2))
        coEvery { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(any())}
        coVerify (exactly = 2) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify (exactly = 3) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` 2
        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal ikke opprette DoknotifikasjonStopp hvis varsel er avbestilt tidligere`() {
        val doneEventId1 = "001"
        val doneEventId2 = "002"
        val doneEventId3 = "003"
        val doneConsumerRecord1 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId1), actualEvent = AvroDoneInternObjectMother.createDoneIntern())
        val doneConsumerRecord2 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId2), actualEvent =  AvroDoneInternObjectMother.createDoneIntern())
        val doneConsumerRecord3 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName ="done", actualKey = AvroNokkelInternObjectMother.createNokkelInternWithEventId(doneEventId3), actualEvent = AvroDoneInternObjectMother.createDoneIntern())
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(listOf(doneConsumerRecord1, doneConsumerRecord2, doneConsumerRecord3))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val varselbestilling1Avbestilt = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-test-001", doneEventId1).copy(avbestilt = true)
        val varselbestilling2Avbestilt = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-test-002", doneEventId2).copy(avbestilt = true)
        val varselbestilling3IkkeAvbestilt = VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId("B-test-002", doneEventId3)


        val capturedListOfEntities = slot<Map<String, DoknotifikasjonStopp>>()
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(listOf(doneEventId1, doneEventId2, doneEventId3)) } returns listOf(varselbestilling1Avbestilt, varselbestilling2Avbestilt, varselbestilling3IkkeAvbestilt)
        coEvery { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(any())}
        coVerify (exactly = 1) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        capturedListOfEntities.captured.size `should be` 1
        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal haandtere at enkelte transformeringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 4
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        val records = ConsumerRecordsObjectMother.giveMeANumberOfDoneRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", )
        val capturedListOfEntities = slot<Map<String, DoknotifikasjonStopp>>()
        coEvery { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(capture(capturedListOfEntities)) } returns Unit
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(allAny()) } returns listOf(
                VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-dummyAppnavn-0", eventId = "0"),
                VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-dummyAppnavn-1", eventId = "1"),
                VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-dummyAppnavn-2", eventId = "2"),
                VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-dummyAppnavn-3", eventId = "3")
        )

        val fieldValidationException = UntransformableRecordException("Simulert feil i en test")
        val doknotifikasjonStopp = AvroDoknotifikasjonStoppObjectMother.giveMeANumberOfDoknotifikasjonStopp(5)
        coEvery { DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(ofType(Varselbestilling::class)) } throws fieldValidationException andThenMany doknotifikasjonStopp

        invoking {
            runBlocking {
                eventService.processEvents(records)
            }
        } `should throw` UntransformableRecordException::class

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.sendEventsAndPersistCancellation(any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEksternvarslingForProducer(any()) }
        coVerify (exactly = numberOfSuccessfulTransformations) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify (exactly = numberOfSuccessfulTransformations + numberOfFailedTransformations) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonStoppProducer)
    }
}

