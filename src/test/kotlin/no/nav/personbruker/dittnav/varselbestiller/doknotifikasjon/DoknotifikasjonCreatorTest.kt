package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import `with message containing`
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother.createOppgaveWithFodselsnummer
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class DoknotifikasjonCreatorTest {

    @Test
    fun `Skal opprette Doknotifikasjon fra Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjed()
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
        doknotifikasjon.getPrefererteKanaler().size `should be equal to` beskjed.getPrefererteKanaler().size
    }

    @Test
    fun `Skal opprette Doknotifikasjon fra Oppgave`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgave = AvroOppgaveObjectMother.createOppgave()
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
        doknotifikasjon.getPrefererteKanaler().size `should be equal to` oppgave.getPrefererteKanaler().size
    }

    @Test
    fun `Skal opprette Doknotifikasjon fra Innboks`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val innboks = AvroInnboksObjectMother.createInnboks()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)

        doknotifikasjon.getBestillingsId() `should be equal to` "I-${nokkel.getSystembruker()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getSystembruker()
        doknotifikasjon.getSikkerhetsnivaa() `should be equal to` 4
        doknotifikasjon.getFodselsnummer() `should be equal to` innboks.getFodselsnummer()
        doknotifikasjon.getTittel().`should not be null or empty`()
        doknotifikasjon.getEpostTekst().`should not be null or empty`()
        doknotifikasjon.getSmsTekst().`should not be null or empty`()
        doknotifikasjon.getAntallRenotifikasjoner() `should be equal to` 1
        doknotifikasjon.getRenotifikasjonIntervall() `should be equal to` 4
        doknotifikasjon.getPrefererteKanaler().size `should be equal to` innboks.getPrefererteKanaler().size
    }

    @Test
    fun `Skal kaste FieldValidationException hvis eventId for Beskjed er for lang`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjed()
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis systembruker for Beskjed er for lang`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val beskjed = AvroBeskjedObjectMother.createBeskjed()
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis fodselsnummer for Beskjed er tomt`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis sikkerhetsnivaa for Beskjed er for lavt`() {
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(2)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Beskjed ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "Prefererte kanaler"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis eventId for Oppgave er for lavt`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val oppgave = AvroOppgaveObjectMother.createOppgave()
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis systembruker for Oppgave er for lang`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val oppgave = AvroOppgaveObjectMother.createOppgave()
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis fodselsnummer for Oppgave er tomt`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val event = createOppgaveWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, event)
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis sikkerhetsnivaa for Oppgave er for lavt`() {
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithSikkerhetsnivaa(2)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Oppgave ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Oppgave`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "Prefererte kanaler"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis eventId for Innboks er for lang`() {
        val tooLongEventId = "1".repeat(51)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(tooLongEventId)
        val innboks = AvroInnboksObjectMother.createInnboks()
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "eventId"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis systembruker for Innboks er for lang`() {
        val tooLongSystembruker = "A".repeat(101)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(tooLongSystembruker)
        val innboks = AvroInnboksObjectMother.createInnboks()
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "systembruker"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis fodselsnummer for Innboks er tomt`() {
        val fodselsnummerEmpty = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val innboks = AvroInnboksObjectMother.createInnboksWithFodselsnummer(fodselsnummerEmpty)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "fodselsnummer"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis sikkerhetsnivaa for Innboks er for lavt`() {
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(3)
        val innboks = AvroInnboksObjectMother.createInnboksWithSikkerhetsnivaa(2)
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "Sikkerhetsnivaa"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Innboks ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val innboks = AvroInnboksObjectMother.createInnboksWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Innboks`() {
        val eventId = 1
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val innboks = AvroInnboksObjectMother.createInnboksWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "Prefererte kanaler"
    }
}
