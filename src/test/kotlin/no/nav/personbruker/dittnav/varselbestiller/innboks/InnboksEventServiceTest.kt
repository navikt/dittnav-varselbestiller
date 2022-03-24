package no.nav.personbruker.dittnav.varselbestiller.innboks

import io.mockk.*
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
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEvent
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEventRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class InnboksEventServiceTest {

    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val earlyDoneEventRepository = mockk<EarlyDoneEventRepository>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = InnboksEventService(doknotifikasjonProducer, varselbestillingRepository, earlyDoneEventRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonCreator)
        clearMocks(doknotifikasjonProducer)
        clearMocks(varselbestillingRepository)
        clearMocks(metricsCollector)
        clearMocks(metricsSession)
        coEvery { varselbestillingRepository.fetchVarselbestillingerForEventIds(allAny()) } returns Collections.emptyList()
        coEvery { metricsCollector.createSession(any()) } returns metricsSession
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal opprette Doknotifikasjon for alle eventer som har ekstern varsling`() {
        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 4, topicName = "innboksTopic", withEksternVarsling = true)
        val innboksWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 6, topicName = "innboksTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()

        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult
        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
            eventService.processEvents(innboksWithoutEksternVarslingRecords)
        }

        verify(exactly = innboksWithEksternVarslingRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(NokkelIntern::class), ofType(InnboksIntern::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count() + innboksWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` innboksWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal ikke opprette Doknotifikasjon for eventer som har tidligere bestilt ekstern varsling`() {
        val innboksRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 5, topicName = "innboksTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfEvents = 5))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns listOf(
            VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "I-dummyAppnavn-1", eventId = "1"))
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        runBlocking {
            eventService.processEvents(innboksRecords)
        }

        verify(exactly = innboksRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(NokkelIntern::class), ofType(InnboksIntern::class)) }
        coVerify(exactly = innboksRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = 1 ) { metricsSession.countDuplicateVarselbestillingForProducer(any()) }
        coVerify(exactly = innboksRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        capturedListOfEntities.captured.size `should be` 4
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Innboks som har ekstern varsling`() {
        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 4, topicName = "innboksTopic", withEksternVarsling = true)
        val innboksWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 6, topicName = "innboksTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns ListPersistActionResult.emptyInstance()

        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
            eventService.processEvents(innboksWithoutEksternVarslingRecords)
        }

        coVerify(exactly = innboksWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count() + innboksWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` innboksWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte transformeringer feiler og fortsette aa transformere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val persistResult = successfulEvents(
            VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(
                numberOfSuccessfulTransformations
            )
        )
        val innboksRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = totalNumberOfRecords, topicName = "innboksTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        val fieldValidationException = UntransformableRecordException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(numberOfSuccessfulTransformations)
        every { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(NokkelIntern::class), ofType(InnboksIntern::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        invoking {
            runBlocking {
                eventService.processEvents(innboksRecords)
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

        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords, topicName = "innboksTopic", withEksternVarsling = true)

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfRecords))
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) } returns persistResult
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
        }

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
    }

    @Test
    fun `Skal ikke bestille varsler for eventer som var tidligere kansellert`() {
        val records = ConsumerRecordsObjectMother.createInnboksRecords(
            topicName = "dummyTopic",
            totalNumber = 3,
            withEksternVarsling = true
        ).toMutableList()
        val eventIdForEventWitheEarlyDone = "event-with-early-cancellation-id"
        val fodselsnummer = "1234"
        val recordWithEarlyCancellation = ConsumerRecordsObjectMother.createConsumerRecordWithKey(
            "dummyTopic",
            AvroNokkelInternObjectMother.createNokkelIntern(eventId = eventIdForEventWitheEarlyDone, fodselsnummer = fodselsnummer),
            AvroInnboksInternObjectMother.createInnboksIntern(eksternVarsling = true)
        )
        records.add(recordWithEarlyCancellation)
        val consumerRecords = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(records)

        coEvery { earlyDoneEventRepository.findByEventIds(any()) } returns listOf(
            EarlyDoneEvent(eventIdForEventWitheEarlyDone, "app", "ns", fodselsnummer, "sbruker", LocalDateTime.now())
        )
        val capturedVarsler = slot<List<Varselbestilling>>()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(any(), capture(capturedVarsler)) } returns ListPersistActionResult.emptyInstance()
        val capturedEarlyCancellationForDeletion = slot<List<String>>()
        coEvery { earlyDoneEventRepository.deleteByEventIds(capture(capturedEarlyCancellationForDeletion)) } returns Unit

        runBlocking {
            eventService.processEvents(consumerRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        capturedVarsler.captured.size `should be equal to` 3
        capturedVarsler.captured.map { it.eventId } shouldNotContain eventIdForEventWitheEarlyDone
        coVerify(exactly = 1) { earlyDoneEventRepository.deleteByEventIds(any()) }
        capturedEarlyCancellationForDeletion.captured.size `should be equal to` 1
        capturedEarlyCancellationForDeletion.captured `should contain` eventIdForEventWitheEarlyDone
    }
}
