package no.nav.personbruker.dittnav.varselbestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
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
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeskjedEventServiceTest {

    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val varselbestillingRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val metricsCollector = mockk<MetricsCollector>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)
    private val eventService = BeskjedEventService(doknotifikasjonProducer, varselbestillingRepository, metricsCollector)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonTransformer)
        clearMocks(doknotifikasjonProducer)
        clearMocks(varselbestillingRepository)
        clearMocks(metricsCollector)
        clearMocks(metricsSession)
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal forkaste eventer som mangler nokkel`() {
        val defaultLopenummer = 1
        val beskjed = AvroBeskjedObjectMother.createBeskjed(defaultLopenummer)
        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = null, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { varselbestillingRepository.fetchVarselbestilling(any(), any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("B-test-001", "1", "123")

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal forkaste eventer som mangler fodselsnummer`() {
        val beskjedWithoutFodselsnummer = AvroBeskjedObjectMother.createBeskjedWithFodselsnummerOgEksternVarsling(1, "", true)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("beskjed", beskjedWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        coVerify(exactly = 1) { metricsSession.countFailedEventForSystemUser(any()) }
        coVerify(exactly = 1) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive kun eventer som har ekstern varsling til Doknotifikasjon-topic`() {
        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val beskjedWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = 4))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit
        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
            eventService.processEvents(beskjedWithoutEksternVarslingRecords)
        }

        verify(exactly = beskjedWithEksternVarslingRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(ofType(Nokkel::class), ofType(Beskjed::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count() + beskjedWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` beskjedWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive alle eventer som har ekstern varsling til Doknotifikasjon-topic`() {
        val beskjedRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents = beskjedRecords.count()))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(beskjedRecords)
        }

        verify(exactly = beskjedRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(ofType(Nokkel::class), ofType(Beskjed::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(any()) }
        coVerify(exactly = beskjedRecords.count()) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = beskjedRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive Doknotifikasjon til database for Beskjeder som har ekstern varsling`() {
        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val beskjedWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<Varselbestilling>>()

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }
        coEvery { varselbestillingRepository.persistInOneBatch(capture(capturedListOfEntities)) } returns ListPersistActionResult.emptyInstance()

        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
            eventService.processEvents(beskjedWithoutEksternVarslingRecords)
        }

        coVerify(exactly = 1) { varselbestillingRepository.persistInOneBatch(allAny()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count()) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = beskjedWithEksternVarslingRecords.count() + beskjedWithoutEksternVarslingRecords.count()) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` beskjedWithEksternVarslingRecords.count()

        confirmVerified(varselbestillingRepository)
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val beskjedRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, no.nav.doknotifikasjon.schemas.Doknotifikasjon>>>()
        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(numberOfSuccessfulTransformations)
        every { DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(ofType(Nokkel::class), ofType(Beskjed::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfSuccessfulTransformations))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        val slot = slot<suspend EventMetricsSession.() -> Unit>()
        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(beskjedRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(any()) }
        coVerify(exactly = numberOfFailedTransformations) { metricsSession.countFailedEventForSystemUser(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = numberOfSuccessfulTransformations + numberOfFailedTransformations) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `skal rapportere hvert velykket event`() {
        val numberOfRecords = 5

        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val slot = slot<suspend EventMetricsSession.() -> Unit>()

        val persistResult = successfulEvents(giveMeANumberOfVarselbestilling(numberOfRecords))
        coEvery { varselbestillingRepository.persistInOneBatch(any()) } returns persistResult

        coEvery { metricsCollector.recordMetrics(any(), capture(slot)) } coAnswers {
            slot.captured.invoke(metricsSession)
        }

        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
        }

        coVerify(exactly = numberOfRecords) { metricsSession.countSuccessfulEventForSystemUser(any()) }
        coVerify(exactly = numberOfRecords) { metricsSession.countAllEventsFromKafkaForSystemUser(any()) }
    }
}

