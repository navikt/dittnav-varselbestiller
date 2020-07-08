package no.nav.personbruker.dittnav.varsel.bestiller.done

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.createKeyForEvent
import no.nav.personbruker.dittnav.varsel.bestiller.done.schema.AvroDoneObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithSystembruker
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class doneEksternVarslingCreatorTest {

    @Test
    fun `Should validate and return event`() {
        val original = AvroDoneObjectMother.createDone("1")
        val nokkel = createNokkelWithEventId(1)

        createKeyForEvent(nokkel)
        createDoneEksternVarslingForEvent(original)
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val event = AvroDoneObjectMother.createDoneWithFodselsnummer(fodselsnummerEmpty)

        invoking {
            runBlocking {
                createDoneEksternVarslingForEvent(event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when systembruker field is too long`() {
        val tooLongSystembruker = "1".repeat(101)
        val nokkel = createNokkelWithSystembruker(tooLongSystembruker)

        invoking {
            runBlocking {
                createKeyForEvent(nokkel)
            }
        } `should throw` FieldValidationException::class
    }


}
