package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class DoknotifikasjonStoppTransformerTest {

    @Test
    fun `should throw FieldValidationException when eventId field for Done is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        invoking {
            DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(nokkel)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when systembruker field for Done is too long`() {
        val tooLongSystembruker = "1".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        invoking {
            DoknotifikasjonStoppTransformer.createDoknotifikasjonStopp(nokkel)
        } `should throw` FieldValidationException::class
    }

}
