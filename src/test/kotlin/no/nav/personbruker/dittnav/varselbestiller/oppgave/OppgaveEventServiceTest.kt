package no.nav.personbruker.dittnav.varselbestiller.oppgave

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
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
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother.giveMeANumberOfVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class OppgaveEventServiceTest {

    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val eventService = OppgaveEventService(doknotifikasjonProducer, varselbestillingRepository, metricsCollector)

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
        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val oppgaveWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult
        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
            eventService.processEvents(oppgaveWithoutEksternVarslingRecords)
        }

        verify(exactly = oppgaveWithEksternVarslingRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(NokkelIntern::class), ofType(OppgaveIntern::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(allAny(), any()) }
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count() + oppgaveWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` oppgaveWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal ikke opprette Doknotifikasjon for eventer som har tidligere bestilt ekstern varsling`() {
        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 5))
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns listOf(VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "O-dummyAppnavn-1", eventId = "1"))

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        verify(exactly = oppgaveRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(NokkelIntern::class), ofType(OppgaveIntern::class)) }
        coVerify (exactly = oppgaveRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify(exactly = 1) { metricsSession.countDuplicateVarselbestillingForProducer(any()) }
        coVerify(exactly = oppgaveRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        coVerify(exactly = 1) { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) }
        capturedListOfEntities.captured.size `should be` 4
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Oppgaver som har ekstern varsling`() {
        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val oppgaveWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns Collections.emptyList()
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns ListPersistActionResult.emptyInstance()
        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
            eventService.processEvents(oppgaveWithoutEksternVarslingRecords)
        }

        coVerify (exactly = oppgaveWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count() + oppgaveWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
        capturedListOfEntities.captured.size `should be` oppgaveWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte transformeringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<Map<String, Doknotifikasjon>>()
        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfSuccessfulTransformations))
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(capture(capturedListOfEntities), any()) } returns persistResult

        val fieldValidationException = UntransformableRecordException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(5)
        every { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(NokkelIntern::class), ofType(OppgaveIntern::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        invoking {
            runBlocking {
                eventService.processEvents(oppgaveRecords)
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

        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfRecords))
        coEvery { doknotifikasjonProducer.sendAndPersistEvents(any(), any()) } returns persistResult
        coEvery { varselbestillingRepository.fetchVarselbestillingerForBestillingIds(any()) } returns emptyList()

        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
        }

        coVerify (exactly = numberOfRecords) { metricsSession.countSuccessfulEksternVarslingForProducer(any()) }
        coVerify (exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForProducer(any()) }
    }
}
