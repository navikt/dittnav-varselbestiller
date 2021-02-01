package no.nav.personbruker.dittnav.varselbestiller.oppgave

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.giveMeANumberOfVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.metrics.EventMetricsSession
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should be`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveEventServiceTest {

    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val doknotifikasjonProducer = mockk<Producer<String, Doknotifikasjon>>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val eventService = OppgaveEventService(doknotifikasjonProducer, varselbestillingRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonCreator)
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
    fun `Skal forkaste eventer som mangler nokkel`() {
        val defaultLopenummer = 1
        val oppgave = AvroOppgaveObjectMother.createOppgave(defaultLopenummer)
        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = null, actualEvent = oppgave)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.fetchVarselbestilling(any(), any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("O-test-001", "1", "123")

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.produceEvents(allAny()) }
        coVerify (exactly = 1) { metricsSession.countNokkelWasNull() }
        confirmVerified(doknotifikasjonProducer)
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

        coVerify(exactly = 0) { doknotifikasjonProducer.produceEvents(allAny()) }
        coVerify (exactly = 1) { metricsSession.countFailedEksternvarslingForSystemUser(any()) }
        coVerify (exactly = 1) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        confirmVerified(doknotifikasjonProducer)

    }

    @Test
    fun `Skal opprette Doknotifikasjon kun for eventer som har ekstern varsling`() {
        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val oppgaveWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.produceEvents(capture(capturedListOfEntities)) } returns Unit
        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
            eventService.processEvents(oppgaveWithoutEksternVarslingRecords)
        }

        verify(exactly = oppgaveWithEksternVarslingRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceEvents(allAny()) }
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count() + oppgaveWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
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

        coEvery { doknotifikasjonProducer.produceEvents(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        verify(exactly = oppgaveRecords.count()) { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceEvents(allAny()) }
        coVerify (exactly = oppgaveRecords.count()) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify (exactly = oppgaveRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
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
        coEvery { doknotifikasjonProducer.produceEvents(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        verify { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) wasNot Called }
        coVerify { doknotifikasjonProducer wasNot Called}
        coVerify (exactly = 0) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify (exactly = oppgaveRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
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
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify (exactly = oppgaveWithEksternVarslingRecords.count() + oppgaveWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` oppgaveWithEksternVarslingRecords.count()
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()
        coEvery { doknotifikasjonProducer.produceEvents(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(5)
        every { DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfSuccessfulTransformations))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.produceEvents(any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEksternvarslingForSystemUser(any()) }
        coVerify(exactly = numberOfFailedTransformations + numberOfSuccessfulTransformations) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
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

        coVerify (exactly = numberOfRecords) { metricsSession.countSuccessfulEksternvarslingForSystemUser(any()) }
        coVerify (exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
    }
}
