package no.nav.personbruker.dittnav.varselbestiller.oppgave

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.giveMeANumberOfVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveEventServiceTest {

    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val eventService = OppgaveEventService(doknotifikasjonProducer, varselbestillingRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonTransformer)
        clearMocks(doknotifikasjonProducer)
        clearMocks(varselbestillingRepository)
        clearMocks(metricsCollector)
        clearMocks(metricsSession)
        coEvery { varselbestillingRepository.fetchVarselbestilling(any()) } returns null
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal forkaste eventer som mangler fodselsnummer`() {
        val oppgaveWithoutFodselsnummer = AvroOppgaveObjectMother.createOppgaveWithFodselsnummerOgEksternVarsling(1, "", true)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("oppgave", oppgaveWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        coVerify (exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        confirmVerified(doknotifikasjonProducer)

    }

    @Test
    fun `Skal opprette Doknotifikasjon kun for eventer som har ekstern varsling`() {
        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val oppgaveWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit
        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
            eventService.processEvents(oppgaveWithoutEksternVarslingRecords)
        }

        verify(exactly = oppgaveWithEksternVarslingRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        coVerify (exactly = 4) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` oppgaveWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal opprette Doknotifikasjon for alle eventer som har ekstern varsling`() {
        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 5))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        verify(exactly = oppgaveRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        coVerify (exactly = 5) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` oppgaveRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal ikke opprette Doknotifikasjon for eventer som har tidligere bestilt ekstern varsling`() {
        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 5))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult
        coEvery { varselbestillingRepository.fetchVarselbestilling(any()) } returns VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "O-test-001", eventId = "001", fodselsnummer = "123")

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        verify { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) wasNot Called }
        coVerify { doknotifikasjonProducer wasNot Called}
        coVerify { metricsSession wasNot Called }
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Beskjeder som har ekstern varsling`() {
        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val oppgaveWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<Varselbestilling>>()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.persistInOneBatch(capture(capturedListOfEntities)) } returns ListPersistActionResult.emptyInstance()
        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
            eventService.processEvents(oppgaveWithoutEksternVarslingRecords)
        }

        coVerify(exactly = 1) { varselbestillingRepository.persistInOneBatch(allAny()) }
        coVerify (exactly = 4) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` oppgaveWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()
        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(5)
        every { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfSuccessfulTransformations))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal rapportere hvert velykket event`() {
        val numberOfRecords = 5

        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfRecords))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
        }

        coVerify (exactly = numberOfRecords) { metricsSession.countSuccessfulEventForSystemUser(any()) }
    }
}
