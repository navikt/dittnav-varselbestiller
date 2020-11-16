package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother.createOppgaveWithFodselsnummer
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class DoknotifikasjonTransformerTest {

    @Test
    fun `should transform from Beskjed to Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjed(eventId)
        val doknotifikasjon = DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, beskjed)

        doknotifikasjon.getBestillingsId() `should be equal to` "B-${nokkel.getSystembruker()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getSystembruker()
        doknotifikasjon.getFodselsnummer() `should be equal to` beskjed.getFodselsnummer()
        doknotifikasjon.getTittel().`should not be null or empty`()
        doknotifikasjon.getEpostTekst().`should not be null or empty`()
        doknotifikasjon.getSmsTekst().`should not be null or empty`()
        doknotifikasjon.getPrefererteKanaler() `should contain any` {it == PrefererteKanal.SMS || it == PrefererteKanal.EPOST}
    }

    @Test
    fun `should transform from Oppgave to Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(1)
        val oppgave = AvroOppgaveObjectMother.createOppgave(eventId)
        val doknotifikasjon = DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, oppgave)

        doknotifikasjon.getBestillingsId() `should be equal to` "O-${nokkel.getSystembruker()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getSystembruker()
        doknotifikasjon.getFodselsnummer() `should be equal to` oppgave.getFodselsnummer()
        doknotifikasjon.getTittel().`should not be null or empty`()
        doknotifikasjon.getEpostTekst().`should not be null or empty`()
        doknotifikasjon.getSmsTekst().`should not be null or empty`()
        doknotifikasjon.getPrefererteKanaler() `should contain any` {it == PrefererteKanal.SMS || it == PrefererteKanal.EPOST}
    }

    @Test
    fun `should throw FieldValidationException if eventId field for Beskjed is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjed(1)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field for Beskjed is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val beskjed = AvroBeskjedObjectMother.createBeskjed(1)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer for Beskjed is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if eventId field for Oppgave is too long`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val oppgave = AvroOppgaveObjectMother.createOppgave(1)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException if systembruker field for Oppgave is too long`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongSystembruker)
        val oppgave = AvroOppgaveObjectMother.createOppgave(1)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `should throw FieldValidationException when fodselsnummer for Oppgave is empty`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val event = createOppgaveWithFodselsnummer(1, fodselsnummerEmpty)
        invoking {
            DoknotifikasjonTransformer.createDoknotifikasjonFromOppgave(nokkel, event)
        } `should throw` FieldValidationException::class
    }
}
