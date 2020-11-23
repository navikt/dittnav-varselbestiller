package no.nav.personbruker.dittnav.varselbestiller.done

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.AvroDoknotifikasjonStoppObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppTransformer
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import org.amshove.kluent.`should be`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DoneEventServiceTest {

    private val doknotifikasjonStoppProducer = mockk<DoknotifikasjonStoppProducer>(relaxed = true)
    private val doknotifikasjonRepository = mockk<VarselbestillingRepository>(relaxed = true)
    private val eventService = DoneEventService(doknotifikasjonStoppProducer, doknotifikasjonRepository)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonStoppTransformer)
        clearMocks(doknotifikasjonStoppProducer)
        clearMocks(doknotifikasjonRepository)
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal forkaste eventer som mangler nokkel`() {
        val done = AvroDoneObjectMother.createDone("001")
        val cr: ConsumerRecord<Nokkel, Done> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = null, actualEvent = done)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)
        coEvery { doknotifikasjonRepository.fetchVarselbestilling(any(), any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("B-test-001", "1", "123")

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(allAny())}
        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal opprette DoknotifikasjonStopp for alle eventer som har ekstern varsling`() {
        val doneRecords = ConsumerRecordsObjectMother.giveMeANumberOfDoneRecords(5, "dummyTopic")
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>>()
        coEvery { doknotifikasjonRepository.fetchVarselbestilling(any(), any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("B-test-001", "1", "123")
        coEvery { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(doneRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(any()) }
        capturedListOfEntities.captured.size `should be` doneRecords.count()

        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal opprette DoknotifikasjonStopp kun for eventer som har ekstern varsling`() {
        val doneEventId1 = "001"
        val doneEventId2 = "002"
        val doneEventId3 = "003"
        val doneConsumerRecord1 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelObjectMother.createNokkelWithEventId(doneEventId1), actualEvent = AvroDoneObjectMother.createDone(eventId = doneEventId1))
        val doneConsumerRecord2 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "done", actualKey = AvroNokkelObjectMother.createNokkelWithEventId(doneEventId2), actualEvent =  AvroDoneObjectMother.createDone(eventId = doneEventId2))
        val doneConsumerRecord3 = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName ="done", actualKey = AvroNokkelObjectMother.createNokkelWithEventId(doneEventId3), actualEvent = AvroDoneObjectMother.createDone(eventId = doneEventId3))
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(listOf(doneConsumerRecord1, doneConsumerRecord2, doneConsumerRecord3))

        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>>()
        coEvery { doknotifikasjonRepository.fetchVarselbestilling(eventId = doneEventId1, any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("B-test-001", doneEventId1, "123")
        coEvery { doknotifikasjonRepository.fetchVarselbestilling(eventId = doneEventId2, any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("B-test-002", doneEventId2, "123")
        coEvery { doknotifikasjonRepository.fetchVarselbestilling(eventId = doneEventId3, any(), any()) } returns null
        coEvery { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(any())}
        capturedListOfEntities.captured.size `should be` 2
        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val records = ConsumerRecordsObjectMother.giveMeANumberOfDoneRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", )
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>>()

        coEvery { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(capture(capturedListOfEntities)) } returns Unit
        coEvery { doknotifikasjonRepository.fetchVarselbestilling(any(), any(), any()) } returns VarselbestillingObjectMother.createVarselbestilling("B-test-001", "1", "123")

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjonStopp = AvroDoknotifikasjonStoppObjectMother.giveMeANumberOfDoknotifikasjonStopp(5)
        coEvery { DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(ofType(Nokkel::class)) } throws fieldValidationException andThenMany doknotifikasjonStopp

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonStoppProducer)
    }

}

