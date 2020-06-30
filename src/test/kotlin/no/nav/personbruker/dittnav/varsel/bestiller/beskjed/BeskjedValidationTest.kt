package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkel
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.time.ZoneId

class BeskjedValidationTest {

    private val dummyNokkel = createNokkel(1)

    @Test
    fun `Should validate and be okay`() {
        val eventId = 1
        val original = AvroBeskjedObjectMother.createBeskjed(eventId)
        val nokkel = createNokkel(eventId)

        BeskjedValidation.validateEvent(nokkel, original)
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val event = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                BeskjedValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if text field is too long`() {
        val tooLongText = "A".repeat(501)
        val event = AvroBeskjedObjectMother.createBeskjedWithText(tooLongText)

        invoking {
            runBlocking {
                BeskjedValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow text length up to the limit`() {
        val textWithMaxAllowedLength = "B".repeat(500)
        val event = AvroBeskjedObjectMother.createBeskjedWithText(textWithMaxAllowedLength)

        runBlocking {
            BeskjedValidation.validateEvent(dummyNokkel, event)
        }
    }

    @Test
    fun `should not allow empty text`() {
        val emptyText = ""
        val event = AvroBeskjedObjectMother.createBeskjedWithText(emptyText)

        invoking {
            runBlocking {
                BeskjedValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should allow synligFremTil to be null`() {
        val beskjedUtenSynligTilSatt = AvroBeskjedObjectMother.createBeskjedWithoutSynligFremTilSatt()

        BeskjedValidation.validateEvent(dummyNokkel, beskjedUtenSynligTilSatt)

    }

}
