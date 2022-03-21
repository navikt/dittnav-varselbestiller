package no.nav.personbruker.dittnav.varselbestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.UntransformableRecordException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation.EarlyCancellation
import no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation.EarlyCancellationRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother.giveMeANumberOfVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class BeskjedEventServiceTest {

    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val earlyCancellationRepository = mockk<EarlyCancellationRepository>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = BeskjedEventService(doknotifikasjonProducer, varselbestillingRepository, earlyCancellationRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonCreator)
        clearMocks(doknotifikasjonProducer)
        clearMocks(varselbestillingRepository)
        clearMocks(earlyCancellationRepository)
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
        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val beskjedWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult
        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
            eventService.processEvents(beskjedWithoutEksternVarslingRecords)
        }

        verify(exactly = beskjedWithEksternVarslingRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(ofType(NokkelIntern::class), ofType(BeskjedIntern::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count() + beskjedWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` beskjedWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal ikke opprette Doknotifikasjon for eventer som har tidligere bestilt ekstern varsling`() {
        val beskjedRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 5))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns listOf(VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-dummyAppnavn-1", eventId = "1"))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        runBlocking {
            eventService.processEvents(beskjedRecords)
        }

        verify(exactly = beskjedRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(ofType(NokkelIntern::class), ofType(BeskjedIntern::class)) }
        coVerify(exactly = beskjedRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = 1 ) { metricsSession.countDuplicateVarselbestillingForProducer(any()) }
        coVerify(exactly = beskjedRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        capturedListOfEntities.captured.size `should be` 4
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Beskjeder som har ekstern varsling`() {
        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val beskjedWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns ListPersistActionResult.emptyInstance()

        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
            eventService.processEvents(beskjedWithoutEksternVarslingRecords)
        }

        coVerify(exactly = beskjedWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count() + beskjedWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` beskjedWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte transformeringer feiler og fortsette aa transformere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfSuccessfulTransformations))
        val beskjedRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        val fieldValidationException = UntransformableRecordException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(numberOfSuccessfulTransformations)
        every { DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(ofType(NokkelIntern::class), ofType(BeskjedIntern::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                eventService.processEvents(beskjedRecords)
            }
        } `should throw` UntransformableRecordException::class

        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations + numberOfFailedTransformations) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal rapportere hvert velykket event`() {
        val numberOfRecords = 5

        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfRecords))
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) } returns persistResult
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
        }

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
    }

    @Test
    fun `Skal ikke bestille varsler for eventer som var tidligere kansellert`() {
        val records = ConsumerRecordsObjectMother.createBeskjedRecords(
            topicName = "dummyTopic",
            totalNumber = 3,
            withEksternVarsling = true
        ).toMutableList()
        val eventIdForEventWitheEarlyCancellation = "event-with-early-cancellation-id"
        val recordWithEarlyCancellation = ConsumerRecordsObjectMother.createConsumerRecordWithKey(
            "dummyTopic",
            AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventIdForEventWitheEarlyCancellation),
            AvroBeskjedInternObjectMother.createBeskjedIntern(eksternVarsling = true)
        )
        records.add(recordWithEarlyCancellation)
        val consumerRecords = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(records)

        coEvery { earlyCancellationRepository.findByEventIds(any()) } returns listOf(
            EarlyCancellation(eventIdForEventWitheEarlyCancellation, "app", "ns", "1234", "sbruker", LocalDateTime.now())
        )
        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers { slot.captured.invoke(metricsSession) }
        val capturedVarsler = slot<List<Varselbestilling>>()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(any(), capture(capturedVarsler)) } returns ListPersistActionResult.emptyInstance()
        val capturedEarlyCancellationForDeletion = slot<List<String>>()
        coEvery { earlyCancellationRepository.deleteByEventIds(capture(capturedEarlyCancellationForDeletion)) } returns Unit

        runBlocking {
            eventService.processEvents(consumerRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        capturedVarsler.captured.size `should be equal to` 3
        capturedVarsler.captured.map { it.eventId } shouldNotContain eventIdForEventWitheEarlyCancellation
        coVerify(exactly = 1) { earlyCancellationRepository.deleteByEventIds(any()) }
        capturedEarlyCancellationForDeletion.captured.size `should be equal to` 1
        capturedEarlyCancellationForDeletion.captured `should contain` eventIdForEventWitheEarlyCancellation
    }
}
