package no.nav.personbruker.dittnav.varsel.bestiller.innboks

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.createKeyForEvent
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class innboksEksternVarslingCreatorTest {

    @Test
    fun `should throw FieldValidationException if eventId field is too long`() {
        val tooLongEventId = "1".repeat(51)
        val key = createNokkelWithEventId(tooLongEventId)

        invoking {
            runBlocking {
                createKeyForEvent(key)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val key = createNokkelWithEventId(tooLongSystembruker)

        invoking {
            runBlocking {
                createKeyForEvent(key)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Should validate and return key`() {
        val eventId = "1"
        val key = createNokkelWithEventId(eventId)

        createKeyForEvent(key)
    }

    @Test
    fun `Should validate and return event`() {
        val eventId = 1
        val event = AvroInnboksObjectMother.createInnboks(eventId)

        createInnboksEksternVarslingForEvent(event)
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val event = AvroInnboksObjectMother.createInnboksWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                createInnboksEksternVarslingForEvent(event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if sikkerhetsnivaa field is to low`() {
        val sikkerhetsnivaa = 2
        val event = AvroInnboksObjectMother.createInnboksWithSikkerhetsnivaa(sikkerhetsnivaa)

        invoking {
            runBlocking {
                createInnboksEksternVarslingForEvent(event)
            }
        } `should throw` FieldValidationException::class
    }


    @Test
    fun `should throw FieldValidationException if text field is too long`() {
        val tooLongText = "A".repeat(501)
        val event = AvroInnboksObjectMother.createInnboksWithText(tooLongText)

        invoking {
            runBlocking {
                createInnboksEksternVarslingForEvent(event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(500)
        val event = AvroInnboksObjectMother.createInnboksWithText(textWithMaxAllowedLength)

        runBlocking {
            createInnboksEksternVarslingForEvent(event)
        }
    }

    @Test
    fun `should not allow empty text`() {
        val emptyText = ""
        val event = AvroInnboksObjectMother.createInnboksWithText(emptyText)

        invoking {
            runBlocking {
                createInnboksEksternVarslingForEvent(event)
            }
        } `should throw` FieldValidationException::class
    }

}

