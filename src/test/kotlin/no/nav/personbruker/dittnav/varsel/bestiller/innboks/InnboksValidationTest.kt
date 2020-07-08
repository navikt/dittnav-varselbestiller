package no.nav.personbruker.dittnav.varsel.bestiller.innboks

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class InnboksValidationTest {

    private val dummyNokkel = createNokkelWithEventId(1)

    @Test
    fun `Should validate and be okay`() {
        val eventId = 1
        val original = AvroInnboksObjectMother.createInnboks(eventId)
        val nokkel = createNokkelWithEventId(eventId)

        InnboksValidation.validateEvent(nokkel, original)
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val event = AvroInnboksObjectMother.createInnboksWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                InnboksValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if text field is too long`() {
        val tooLongText = "A".repeat(501)
        val event = AvroInnboksObjectMother.createInnboksWithText(tooLongText)

        invoking {
            runBlocking {
                InnboksValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(500)
        val event = AvroInnboksObjectMother.createInnboksWithText(textWithMaxAllowedLength)

        runBlocking {
            InnboksValidation.validateEvent(dummyNokkel, event)
        }
    }

    @Test
    fun `should not allow empty text`() {
        val emptyText = ""
        val event = AvroInnboksObjectMother.createInnboksWithText(emptyText)

        invoking {
            runBlocking {
                InnboksValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

}
