package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.FieldValidationException
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveInternObjectMother
import org.junit.jupiter.api.Test

class DoknotifikasjonCreatorTest {

    @Test
    fun `Skal opprette Doknotifikasjon fra Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)

        doknotifikasjon.getBestillingsId() shouldBe "B-${nokkel.getAppnavn()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() shouldBe nokkel.getAppnavn()
        doknotifikasjon.getSikkerhetsnivaa() shouldBe 4
        doknotifikasjon.getFodselsnummer() shouldBe nokkel.getFodselsnummer()
        doknotifikasjon.getTittel().length shouldBeGreaterThan 0
        doknotifikasjon.getEpostTekst().length shouldBeGreaterThan 0
        doknotifikasjon.getSmsTekst().length shouldBeGreaterThan 0
        doknotifikasjon.getAntallRenotifikasjoner() shouldBe 0
        doknotifikasjon.getRenotifikasjonIntervall().shouldBeNull()
        doknotifikasjon.getPrefererteKanaler().size shouldBe beskjed.getPrefererteKanaler().size
    }

    @Test
    fun `Skal opprette Doknotifikasjon fra Oppgave`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)

        doknotifikasjon.getBestillingsId() shouldBe "O-${nokkel.getAppnavn()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() shouldBe nokkel.getAppnavn()
        doknotifikasjon.getSikkerhetsnivaa() shouldBe 4
        doknotifikasjon.getFodselsnummer() shouldBe nokkel.getFodselsnummer()
        doknotifikasjon.getTittel().length shouldBeGreaterThan 0
        doknotifikasjon.getEpostTekst().length shouldBeGreaterThan 0
        doknotifikasjon.getSmsTekst().length shouldBeGreaterThan 0
        doknotifikasjon.getAntallRenotifikasjoner() shouldBe 1
        doknotifikasjon.getRenotifikasjonIntervall() shouldBe 7
        doknotifikasjon.getPrefererteKanaler().size shouldBe oppgave.getPrefererteKanaler().size
    }

    @Test
    fun `Skal opprette Doknotifikasjon fra Innboks`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern()
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)

        doknotifikasjon.getBestillingsId() shouldBe "I-${nokkel.getAppnavn()}-${nokkel.getEventId()}"
        doknotifikasjon.getBestillerId() shouldBe nokkel.getAppnavn()
        doknotifikasjon.getSikkerhetsnivaa() shouldBe 4
        doknotifikasjon.getFodselsnummer() shouldBe nokkel.getFodselsnummer()
        doknotifikasjon.getTittel().length shouldBeGreaterThan 0
        doknotifikasjon.getEpostTekst().length shouldBeGreaterThan 0
        doknotifikasjon.getSmsTekst().length shouldBeGreaterThan 0
        doknotifikasjon.getAntallRenotifikasjoner() shouldBe 1
        doknotifikasjon.getRenotifikasjonIntervall() shouldBe 4
        doknotifikasjon.getPrefererteKanaler().size shouldBe innboks.getPrefererteKanaler().size
    }

    @Test
    fun `Skal opprette Doknotifikasjon med epost tekst fra Beskjed med epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedWithEpostVarslingstekst("epost varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getEpostTekst() shouldContain "epost varslingstekst"
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
        doknotifikasjon.getEpostTekst() shouldContain "epost tittel"
        doknotifikasjon.getTittel() shouldBe "epost tittel"
    }

    @Test
    fun `Skal opprette Doknotifikasjon med default epost tekst fra Beskjed uten epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getEpostTekst() shouldBe this::class.java.getResource("/texts/epost_beskjed.txt").readText(Charsets.UTF_8)
    }

    @Test
    fun `Skal opprette Doknotifikasjon med sms tekst fra Beskjed med smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedWithSmsVarslingstekst("sms varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getSmsTekst() shouldBe "sms varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Beskjed uten smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        doknotifikasjon.getSmsTekst() shouldBe this::class.java.getResource("/texts/sms_beskjed.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Oppgave med epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveWithEpostVarslingstekst("epost varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getEpostTekst() shouldContain "epost varslingstekst"
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
        doknotifikasjon.getEpostTekst() shouldContain "epost tittel"
        doknotifikasjon.getTittel() shouldBe "epost tittel"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med default epost tekst fra Oppgave uten epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getEpostTekst() shouldBe this::class.java.getResource("/texts/epost_oppgave.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Oppgave med smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveWithSmsVarslingstekst("sms varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getSmsTekst() shouldBe "sms varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Oppgave uten smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        doknotifikasjon.getSmsTekst() shouldBe this::class.java.getResource("/texts/sms_oppgave.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med epost tekst fra Innboks med epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern(epostVarslingstekst = "epost varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getEpostTekst() shouldContain "epost varslingstekst"
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
        doknotifikasjon.getEpostTekst() shouldContain "epost tittel"
        doknotifikasjon.getTittel() shouldBe "epost tittel"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med default epost tekst fra Innboks uten epostVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getEpostTekst() shouldBe this::class.java.getResource("/texts/epost_innboks.txt").readText(Charsets.UTF_8)
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Innboks med smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern(smsVarslingstekst = "sms varslingstekst")

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getSmsTekst() shouldBe "sms varslingstekst"
    }

    @Test
    internal fun `Skal opprette Doknotifikasjon med sms tekst fra Innboks uten smsVarslingstekst`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksIntern()

        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        doknotifikasjon.getSmsTekst() shouldBe this::class.java.getResource("/texts/sms_innboks.txt").readText(Charsets.UTF_8)
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Beskjed ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedInternWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        }.message shouldContain "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Beskjed`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val beskjed = AvroBeskjedInternObjectMother.createBeskjedInternWithEksternVarslingOgPrefererteKanaler(false, listOf(
            PreferertKanal.SMS.toString()))
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)
        }.message shouldContain "Prefererte kanaler"
    }


    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Oppgave ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveInternWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        }.message shouldContain "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Oppgave`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val oppgave = AvroOppgaveInternObjectMother.createOppgaveInternWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)
        }.message shouldContain "Prefererte kanaler"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal for Innboks ikke støttes av Doknotifikasjon`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksInternWithEksternVarslingOgPrefererteKanaler(true, listOf("UgyldigKanal"))
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        }.message shouldContain "preferert kanal"
    }

    @Test
    fun `Skal kaste FieldValidationException hvis preferert kanal settes uten at ekstern varsling er satt for Innboks`() {
        val eventId = 1
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(eventId)
        val innboks = AvroInnboksInternObjectMother.createInnboksInternWithEksternVarslingOgPrefererteKanaler(false, listOf(PreferertKanal.SMS.toString()))
        shouldThrow<FieldValidationException> {
            DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)
        }.message shouldContain "Prefererte kanaler"
    }
}
