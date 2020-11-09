package no.nav.personbruker.dittnav.varselbestiller.oppgave

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.AvroDoknotifikasjonObjectMother
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonTransformer
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveEventServiceTest {

    private val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    private val eventService = OppgaveEventService(doknotifikasjonProducer)

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
    fun `Skal forkaste eventer som mangler fodselsnummer`() {
        val oppgaveWithoutFodselsnummer = AvroOppgaveObjectMother.createOppgaveWithFodselsnummerOgEksternVarsling(1, "", true)
        val cr = ConsumerRecordsObjectMother.createConsumerRecord("oppgave", oppgaveWithoutFodselsnummer)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        runBlocking {
            eventService.processEvents(records)
        }

        coVerify(exactly = 0) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        confirmVerified(doknotifikasjonProducer)

    }

    @Test
    fun `Skal skrive kun eventer som skal varsles eksternt til Doknotifikasjon-topic`() {
        val oppgaveWithEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 4, topicName = "dummyTopic", withEksternVarsling = true)
        val oppgaveWithoutEksternVarslingRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 6, topicName = "dummyTopic", withEksternVarsling = false)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit
        runBlocking {
            eventService.processEvents(oppgaveWithEksternVarslingRecords)
            eventService.processEvents(oppgaveWithoutEksternVarslingRecords)
        }

        verify(exactly = oppgaveWithEksternVarslingRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        capturedListOfEntities.captured.size `should be` oppgaveWithEksternVarslingRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal skrive alle eventer som skal varsles eksternt til Doknotifikasjon-topic`() {
        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = 5, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()

        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        verify(exactly = oppgaveRecords.count()) { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) }
        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(allAny()) }
        capturedListOfEntities.captured.size `should be` oppgaveRecords.count()

        confirmVerified(doknotifikasjonProducer)
    }

    @Test
    fun `Skal haandtere at enkelte valideringer feiler og fortsette aa validere resten av batch-en`() {
        val totalNumberOfRecords = 5
        val numberOfFailedTransformations = 1
        val numberOfSuccessfulTransformations = totalNumberOfRecords - numberOfFailedTransformations

        val oppgaveRecords = ConsumerRecordsObjectMother.giveMeANumberOfOppgaveRecords(numberOfRecords = totalNumberOfRecords, topicName = "dummyTopic", withEksternVarsling = true)
        val capturedListOfEntities = slot<List<RecordKeyValueWrapper<String, Doknotifikasjon>>>()
        coEvery { doknotifikasjonProducer.produceDoknotifikasjon(capture(capturedListOfEntities)) } returns Unit

        val fieldValidationException = FieldValidationException("Simulert feil i en test")
        val doknotifikasjoner = AvroDoknotifikasjonObjectMother.giveMeANumberOfDoknotifikasjoner(5)
        every { DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(ofType(Nokkel::class), ofType(Oppgave::class)) } throws fieldValidationException andThenMany doknotifikasjoner

        runBlocking {
            eventService.processEvents(oppgaveRecords)
        }

        coVerify(exactly = 1) { doknotifikasjonProducer.produceDoknotifikasjon(any()) }
        capturedListOfEntities.captured.size `should be` numberOfSuccessfulTransformations

        confirmVerified(doknotifikasjonProducer)
    }
}
