package no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon

import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.AvroBeskjedObjectMother.createBeskjed
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.AvroBeskjedObjectMother.createBeskjedWithFodselsnummer
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithSystembruker
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.AvroOppgaveObjectMother.createOppgave
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.AvroOppgaveObjectMother.createOppgaveWithFodselsnummer
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class DoknotifikasjonTransformerTest {

    @Test
    fun `should throw FieldValidationException if eventId field for Beskjed is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = createNokkelWithEventId(tooLongEventId)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, createBeskjed(1))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field for Beskjed is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = createNokkelWithSystembruker(tooLongSystembruker)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, createBeskjed(2))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer for Beskjed is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = createNokkelWithEventId(3)
        val event = createBeskjedWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if eventId field for Oppgave is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = createNokkelWithEventId(tooLongEventId)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, createOppgave(1))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field for Oppgave is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = createNokkelWithEventId(tooLongSystembruker)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, createOppgave(2))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer for Oppgave is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = createNokkelWithEventId(3)
        val event = createOppgaveWithFodselsnummer(1, fodselsnummerEmpty)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when eventId field for Done is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = createNokkelWithEventId(tooLongEventId)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonStopp(nokkel)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when systembruker field for Done is too long`() {
        val tooLongSystembruker = "1".repeat(101)
        val nokkel = createNokkelWithSystembruker(tooLongSystembruker)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonStopp(nokkel)
        } `should throw` FieldValidationException::class
    }
}
