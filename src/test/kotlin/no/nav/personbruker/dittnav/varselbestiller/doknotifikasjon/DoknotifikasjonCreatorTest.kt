package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import `with message containing`
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksInternObjectMother
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class DoknotifikasjonCreatorTest {

    @Test
    fun `Skal opprette Doknotifikasjon fra Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)

        doknotifikasjon.getBestillingsId() `should be equal to` "B-${nokkel.getAppnavn()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getAppnavn()
        doknotifikasjon.getSikkerhetsnivaa() `should be equal to` 4
        doknotifikasjon.getFodselsnummer() `should be equal to` nokkel.getFodselsnummer()
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
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)

        doknotifikasjon.getBestillingsId() `should be equal to` "O-${nokkel.getAppnavn()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getAppnavn()
        doknotifikasjon.getSikkerhetsnivaa() `should be equal to` 4
        doknotifikasjon.getFodselsnummer() `should be equal to` nokkel.getFodselsnummer()
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
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)

        doknotifikasjon.getBestillingsId() `should be equal to` "I-${nokkel.getAppnavn()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() `should be equal to` nokkel.getSystembruker()
        doknotifikasjon.getSikkerhetsnivaa() `should be equal to` 4
        doknotifikasjon.getFodselsnummer() `should be equal to` nokkel.getFodselsnummer()
        doknotifikasjon.getTittel().`should not be null or empty`()
        doknotifikasjon.getEpostTekst().`should not be null or empty`()
        doknotifikasjon.getSmsTekst().`should not be null or empty`()
        doknotifikasjon.getAntallRenotifikasjoner() `should be equal to` 1
        doknotifikasjon.getRenotifikasjonIntervall() `should be equal to` 4
        doknotifikasjon.getPrefererteKanaler().size `should be equal to` innboks.getPrefererteKanaler().size
    }

    @Test
    fun `Skal opprette Doknotifikasjon med epost tekst fra Beskjed med epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedWithEpostVarslingstekst("epost varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getEpostTekst() `should contain` "epost varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Beskjed med epostVarslingstittel`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern(
            epostVarslingstekst = "epost varslingstekst",
            epostVarslingstittel = "epost tittel"
        )

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getEpostTekst() `should contain` "epost tittel"
        doknotifikasjon.getTittel() `should be equal to` "epost tittel"
    }

    @Test
    fun `Skal opprette Doknotifikasjon med default epost tekst fra Beskjed uten epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getEpostTekst() `should be equal to` this::class.java.getResource("/texts/epost_beskjed.txt").readText(Charsets.UTF_8)
    }

    @Test
    fun `Skal opprette Doknotifikasjon med sms tekst fra Beskjed med smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedWithSmsVarslingstekst("sms varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getSmsTekst() `should be equal to` "sms varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Beskjed uten smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getSmsTekst() `should be equal to` this::class.java.getResource("/texts/sms_beskjed.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Oppgave med epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveWithEpostVarslingstekst("epost varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getEpostTekst() `should contain` "epost varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Oppgave med epostVarslingstittel`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern(
            epostVarslingstekst = "epost varslingstekst",
            epostVarslingstittel = "epost tittel"
        )

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getEpostTekst() `should contain` "epost tittel"
        doknotifikasjon.getTittel() `should be equal to` "epost tittel"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med default epost tekst fra Oppgave uten epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getEpostTekst() `should be equal to` this::class.java.getResource("/texts/epost_oppgave.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Oppgave med smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveWithSmsVarslingstekst("sms varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getSmsTekst() `should be equal to` "sms varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Oppgave uten smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getSmsTekst() `should be equal to` this::class.java.getResource("/texts/sms_oppgave.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Innboks med epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern(epostVarslingstekst = "epost varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getEpostTekst() `should contain` "epost varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Innboks med epostVarslingstittel`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern(
            epostVarslingstekst = "epost varslingstekst",
            epostVarslingstittel = "epost tittel"
        )

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getEpostTekst() `should contain` "epost tittel"
        doknotifikasjon.getTittel() `should be equal to` "epost tittel"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med default epost tekst fra Innboks uten epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getEpostTekst() `should be equal to` this::class.java.getResource("/texts/epost_innboks.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Innboks med smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern(smsVarslingstekst = "sms varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getSmsTekst() `should be equal to` "sms varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Innboks uten smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getSmsTekst() `should be equal to` this::class.java.getResource("/texts/sms_innboks.txt").readText(Charsets.UTF_8)
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Beskjed ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedInternWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedInternWithEksternVarslingOgPrefererteKanaler(false, listOf(
            PreferertKanal.SMS.toString()))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        } `should throw` FieldValidationException::class `with message containing` "Prefererte kanaler"
    }


    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Oppgave ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveInternWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Oppgave`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveInternWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        } `should throw` FieldValidationException::class `with message containing` "Prefererte kanaler"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Innboks ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksInternWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Innboks`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksInternWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        invoking {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        } `should throw` FieldValidationException::class `with message containing` "Prefererte kanaler"
    }
}
