package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother.createOppgaveWithFodselsnummer
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class DoknotifikasjonCreatorTest {

    @Test
    fun `Skal opprette Doknotifikasjon fra Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjed(eventId)
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)

        doknotifikasjon.getBestillingsId() `should be equal to` "B-${nokkel.getSystembruker()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getSystembruker()
        doknotifikasjon.getSikkerhetsnivaa() `should be equal to` 4
        doknotifikasjon.getFodselsnummer() `should be equal to` beskjed.getFodselsnummer()
        doknotifikasjon.getTittel().`should not be null or empty`()
        doknotifikasjon.getEpostTekst().`should not be null or empty`()
        doknotifikasjon.getSmsTekst().`should not be null or empty`()
        doknotifikasjon.getAntallRenotifikasjoner() `should be equal to` 0
        doknotifikasjon.getRenotifikasjonIntervall().`should be null`()
        doknotifikasjon.getPrefererteKanaler().`should be empty`()
    }

    @Test
    fun `Skal opprette Doknotifikasjon fra Oppgave`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgave = AvroOppgaveObjectMother.createOppgave(eventId)
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)

        doknotifikasjon.getBestillingsId() `should be equal to` "O-${nokkel.getSystembruker()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getSystembruker()
        doknotifikasjon.getSikkerhetsnivaa() `should be equal to` 4
        doknotifikasjon.getFodselsnummer() `should be equal to` oppgave.getFodselsnummer()
        doknotifikasjon.getTittel().`should not be null or empty`()
        doknotifikasjon.getEpostTekst().`should not be null or empty`()
        doknotifikasjon.getSmsTekst().`should not be null or empty`()
        doknotifikasjon.getAntallRenotifikasjoner() `should be equal to` 1
        doknotifikasjon.getRenotifikasjonIntervall() `should be equal to` 7
        doknotifikasjon.getPrefererteKanaler().`should be empty`()
    }

    @Test
    fun `Skal kaste FieldValidationException hvis eventId for Beskjed er for lang`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjed(1)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis systembruker for Beskjed er for lang`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val beskjed = AvroBeskjedObjectMother.createBeskjed(1)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis fodselsnummer for Beskjed er tomt`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis sikkerhetsnivaa for Beskjed er for lavt`() {
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(2)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis eventId for Oppgave er for lavt`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val oppgave = AvroOppgaveObjectMother.createOppgave(1)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis systembruker for Oppgave er for lang`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongSystembruker)
        val oppgave = AvroOppgaveObjectMother.createOppgave(1)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis fodselsnummer for Oppgave er tomt`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val event = createOppgaveWithFodselsnummer(1, fodselsnummerEmpty)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, event)
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste FieldValidationException hvis sikkerhetsnivaa for Oppgave er for lavt`() {
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithSikkerhetsnivaa(2)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class
    }

}
