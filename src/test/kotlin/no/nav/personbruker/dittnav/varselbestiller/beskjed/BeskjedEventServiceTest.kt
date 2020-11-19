package no.nav.personbruker.dittnav.varselbestiller.beskjed

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonRepository
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeskjedEventServiceTest {

    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val doknotifikasjonRepository = mockk<DoknotifikasjonRepository>(relaxed = true)
    private val eventService = BeskjedEventService(doknotifikasjonProducer, doknotifikasjonRepository)

    @BeforeEach
    private fun resetMocks() {
        mockkObject(DoknotifikasjonTransformer)
        clearMocks(doknotifikasjonProducer)
    }

    @AfterAll
    private fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `skal forkaste eventer som mangler fodselsnummer`() {
        val beskjedWithoutFodselsnummer = AvroBeskjedObjectMother.createBeskjedWithFodselsnummerOgEksternVarsling(1,"", true)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("beskjed", beskjedWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive kun eventer som skal varsles eksternt til Doknotifikasjon-topic`() {
        val beskjedWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val beskjedWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit
        runBlocking {
            eventService.processEvents(beskjedWithEksternVarslingRecords)
            eventService.processEvents(beskjedWithoutEksternVarslingRecords)
        }

        verify(exactly = beskjedWithEksternVarslingRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(ofType(Nokkel::class), ofType(Beskjed::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        capturedListOfEntities.captured.size `should be` beskjedWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive alle eventer som skal varsles eksternt til ny kafka-topic`() {
        val beskjedRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(beskjedRecords)
        }

        verify(exactly = beskjedRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(ofType(Nokkel::class), ofType(Beskjed::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(any()) }
        capturedListOfEntities.captured.size `should be` beskjedRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val beskjedRecords = ConsumerRecordsObjectMother.giveMeANumberOfBeskjedRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()
        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(5)
        every { DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(ofType(Nokkel::class), ofType(Beskjed::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        runBlocking {
            eventService.processEvents(beskjedRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }
}
