package no.nav.personbruker.dittnav.varsel.bestiller.done

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.done.schema.AvroDoneObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkel
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithSystembruker
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class DoneValidationTest {

    private val dummyNokkel = createNokkel(1)
    private val dummyText = "dummyText"

    @Test
    fun `Should validate and be okay`() {
        val original = AvroDoneObjectMother.createDone("1")
        val nokkel = createNokkel(1)

        DoneValidation.validateEvent(nokkel, original)
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val event = AvroDoneObjectMother.createDoneWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                DoneValidation.validateEvent(dummyNokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when systembruker field is too long`() {
        val tooLongSystembruker = "1".repeat(1001)
        val nokkel = createNokkelWithSystembruker(tooLongSystembruker)
        val event = AvroDoneObjectMother.createDone(dummyText)

        invoking {
            runBlocking {
                DoneValidation.validateEvent(nokkel, event)
            }
        } `should throw` FieldValidationException::class
    }


}
