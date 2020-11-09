package no.nav.personbruker.dittnav.varsel.bestiller.done

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.AvroDoknotifikasjonStoppObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonTransformer
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Disabled frem til sjekk på om brukernotifikasjonen tilhørende Done-eventet faktisk har bestilt ekstern varsling er på plass")
class DoneEventServiceTest {

    private val doknotifikasjonStoppProducer = mockk<DoknotifikasjonStoppProducer>(relaxed = true)
    private val eventService = DoneEventService(doknotifikasjonStoppProducer)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonTransformer)
        clearMocks(doknotifikasjonStoppProducer)
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `skal forkaste eventer som mangler fodselsnummer`() {
        val doneWithoutFodselsnummer = AvroDoneObjectMother.createDoneWithFodselsnummer("")
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("done", doneWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        val capturedNumberOfEntities = slot<List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>>()
        coEvery { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(capture(capturedNumberOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(records)
        }

        capturedNumberOfEntities.captured.size `should be` 0

    }

    @Test
    fun `Skal skrive alle eventer til ny kafka-topic`() {
        val doneRecords = ConsumerRecordsObjectMother.giveMeANumberOfDoneRecords(5, "dummyTopic")
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>>()

        coEvery { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(capture(capturedListOfEntities)) } returns Unit


        runBlocking {
            eventService.processEvents(doneRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(any()) }
        capturedListOfEntities.captured.size `should be` doneRecords.count()

        confirmVerified(doknotifikasjonStoppProducer)
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val records = ConsumerRecordsObjectMother.giveMeANumberOfDoneRecords(totalNumberOfRecords, "dummyTopic")
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>>()
        coEvery { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjonStopp = AvroDoknotifikasjonStoppObjectMother.giveMeANumberOfDoknotifikasjonStopp(5)
        coEvery { DoknotifikasjonTransformer.createDoknotifikasjonStopp(ofType(Nokkel::class)) } throws fieldValidationException andThenMany doknotifikasjonStopp


        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 1) { doknotifikasjonStoppProducer.produceDoknotifikasjonStop(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonStoppProducer)
    }

}

