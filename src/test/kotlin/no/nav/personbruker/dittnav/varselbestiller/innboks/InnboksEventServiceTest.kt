package no.nav.personbruker.dittnav.varselbestiller.innboks

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should be`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

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
    fun `Skal forkaste eventer som mangler nokkel`() {
        val innboks = AvroInnboksObjectMother.createInnboks()
        val cr: ConsumerRecord<Nokkel, Innboks> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "innboksTopic", actualKey = null, actualEvent = innboks)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify(exactly = 1) { metricsSession.countNokkelWasNull() }
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal forkaste eventer som mangler fodselsnummer`() {
        val innboksWithoutFodselsnummer = AvroInnboksObjectMother.createInnboksWithFodselsnummerOgEksternVarsling("", true)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("innboksTopic", innboksWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEksternvarslingForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal opprette Doknotifikasjon for alle eventer som har ekstern varsling`() {
        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 4, topicName = "innboksTopic", withEksternVarsling = true)
        val innboksWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 6, topicName = "innboksTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult
        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
            eventService.processEvents(innboksWithoutEksternVarslingRecords)
        }

        verify(exactly = innboksWithEksternVarslingRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(Nokkel::class), ofType(Innboks::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count() + innboksWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` innboksWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal ikke opprette Doknotifikasjon for eventer som har tidligere bestilt ekstern varsling`() {
        val innboksRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 5, topicName = "innboksTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfEvents = 5))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns listOf(
            VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "I-dummySystembruker-1", eventId = "1"))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        runBlocking {
            eventService.processEvents(innboksRecords)
        }

        verify(exactly = innboksRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(Nokkel::class), ofType(Innboks::class)) }
        coVerify(exactly = innboksRecords.count()) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify(exactly = 1 ) { metricsSession.countDuplicateVarselbestillingForSystemUser(any()) }
        coVerify(exactly = innboksRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        capturedListOfEntities.captured.size `should be` 4
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal haandtere at et event med feil type har havnet paa topic`() {
        val malplacedOppgave = AvroOppgaveObjectMother.createOppgave()
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("innboksTopic", malplacedOppgave)
        val malplacedRecords = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)
        val records = malplacedRecords as ConsumerRecords<Nokkel, Innboks>

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify(exactly = 1) { metricsSession.countFailedEksternvarslingForSystemUser(any()) }
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Innboks som har ekstern varsling`() {
        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 4, topicName = "innboksTopic", withEksternVarsling = true)
        val innboksWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = 6, topicName = "innboksTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns ListPersistActionResult.emptyInstance()

        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
            eventService.processEvents(innboksWithoutEksternVarslingRecords)
        }

        coVerify(exactly = innboksWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify(exactly = innboksWithEksternVarslingRecords.count() + innboksWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` innboksWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfSuccessfulTransformations))
        val innboksRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords = totalNumberOfRecords, topicName = "innboksTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(numberOfSuccessfulTransformations)
        every { DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(ofType(Nokkel::class), ofType(Innboks::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(innboksRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEksternvarslingForSystemUser(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations + numberOfFailedTransformations) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal rapportere hvert velykket event`() {
        val numberOfRecords = 5

        val innboksWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfInnboksRecords(numberOfRecords, topicName = "innboksTopic", withEksternVarsling = true)
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        val persistResult = successfulEvents(VarselbestillingObjectMother.giveMeANumberOfVarselbestilling(numberOfRecords))
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) } returns persistResult
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(innboksWithEksternVarslingRecords)
        }

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify(exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
    }
}
