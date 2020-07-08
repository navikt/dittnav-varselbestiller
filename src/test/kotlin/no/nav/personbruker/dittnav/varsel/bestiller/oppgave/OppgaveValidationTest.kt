package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class OppgaveValidationTest {

    private val dummyNokkel = createNokkelWithEventId(1)
    private val dummyFnr = "dummyFrn"
    private val dummyEventId = 1

    @Test
    fun `Should validate and be okay`() {
        val eventId = 1
        val original = AvroOppgaveObjectMother.createOppgave(eventId)
        val nokkel = createNokkelWithEventId(eventId)

        OppgaveValidation.validateEvent(nokkel, original)
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val event = AvroOppgaveObjectMother.createOppgave(dummyEventId, fodselsnummerEmpty)

        invoking {
            runBlocking {
                OppgaveValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if text field is too long`() {
        val tooLongText = "A".repeat(501)
        val event = AvroOppgaveObjectMother.createOppgave(dummyEventId, dummyFnr, tooLongText)

        invoking {
            runBlocking {
                OppgaveValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(500)
        val event = AvroOppgaveObjectMother.createOppgave(dummyEventId, dummyFnr, textWithMaxAllowedLength)

        runBlocking {
            OppgaveValidation.validateEvent(dummyNokkel, event)
        }
    }

    @Test
    fun `should not allow empty text`() {
        val emptyText = ""
        val event = AvroOppgaveObjectMother.createOppgave(dummyEventId, dummyFnr, emptyText)

        invoking {
            runBlocking {
                OppgaveValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

}

