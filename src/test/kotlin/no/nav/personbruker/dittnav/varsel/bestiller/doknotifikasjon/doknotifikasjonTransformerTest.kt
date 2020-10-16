package no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon

import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.AvroBeskjedObjectMother.createBeskjed
import no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varsel.bestiller.done.AvroDoneObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithSystembruker
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.AvroOppgaveObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.AvroOppgaveObjectMother.createOppgave
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.jupiter.api.Test

class doknotifikasjonTransformerTest {

    @Test
    fun `should throw FieldValidationException if eventId field for Beskjed is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = createNokkelWithEventId(tooLongEventId)
        invoking {
            createDoknotifikasjonFromBeskjed(nokkel, createBeskjed(1))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field for Beskjed is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = createNokkelWithSystembruker(tooLongSystembruker)
        invoking {
            createDoknotifikasjonFromBeskjed(nokkel, createBeskjed(2))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer for Beskjed is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = createNokkelWithEventId(3)
        val event = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            createDoknotifikasjonFromBeskjed(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if sikkerhetsnivaa field for Beskjed is to low`() {
        val sikkerhetsnivaa = 2
        val nokkel = createNokkelWithEventId(4)
        val event = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(sikkerhetsnivaa)
        invoking {
            createDoknotifikasjonFromBeskjed(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer for Done is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = createNokkelWithEventId(5)
        val event = AvroDoneObjectMother.createDoneWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            createDoknotifikasjonStoppFromDone(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when systembruker field for Done is too long`() {
        val tooLongSystembruker = "1".repeat(101)
        val nokkel = createNokkelWithSystembruker(tooLongSystembruker)
        val event = AvroDoneObjectMother.createDone(1)
        invoking {
            createDoknotifikasjonStoppFromDone(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if eventId field for Oppgave is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = createNokkelWithEventId(tooLongEventId)
        invoking {
            createDoknotifikasjonFromOppgave(nokkel, createOppgave(1))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = createNokkelWithEventId(tooLongSystembruker)
        invoking {
            createDoknotifikasjonFromOppgave(nokkel, createOppgave(2))
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = createNokkelWithEventId(3)
        val event = AvroOppgaveObjectMother.createOppgaveWithFodselsnummer(1, fodselsnummerEmpty)
        invoking {
            runBlocking {
                createDoknotifikasjonFromOppgave(nokkel, event)
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if sikkerhetsnivaa field is to low`() {
        val sikkerhetsnivaa = 2
        val nokkel = createNokkelWithEventId(4)
        val event = AvroOppgaveObjectMother.createOppgaveWithSikkerhetsnivaa(sikkerhetsnivaa)
        invoking {
            runBlocking {
                createDoknotifikasjonFromOppgave(nokkel, event)
            }
        } `should throw` FieldValidationException::class
    }
}
