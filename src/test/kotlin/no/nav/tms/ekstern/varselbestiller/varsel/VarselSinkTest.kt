package no.nav.tms.ekstern.varselbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselOpprettetSink
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VarselSinkTest {

    private val kafkaProducer = KafkaTestUtil.createMockProducer<String, Doknotifikasjon>()

    private val doknotProducer = DoknotEventProducer<Doknotifikasjon>("test-topic", kafkaProducer)


    @BeforeEach
    fun cleanUp() {
        kafkaProducer.clear()
    }

    @ParameterizedTest
    @CsvSource("Beskjed,0,null", "Oppgave,1,7", "Innboks,1,4", nullValues = ["null"])
    fun `Bestiller ekstern varsling for varsel`(varselType: VarselType, antallRenotifikasjoner: Int, renotifikasjonIntervall: Int?) {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        val varselJson = varselOpprettetJson(varselType, "1")
        testRapid.sendTestMessage(varselJson)

        kafkaProducer.history().size shouldBe 1

        val doknotifikasjon = kafkaProducer.history().first()
        val varselJsonNode = ObjectMapper().readTree(varselJson)
        doknotifikasjon.key() shouldBe varselJsonNode["varselId"].textValue()
        doknotifikasjon.value().getBestillingsId() shouldBe varselJsonNode["varselId"].textValue()
        doknotifikasjon.value().getBestillerId() shouldBe varselJsonNode["produsent"]["appnavn"].textValue()
        doknotifikasjon.value().getSikkerhetsnivaa() shouldBe if(varselJsonNode["sensitivitet"].textValue() == "high") 4 else 3
        doknotifikasjon.value().getFodselsnummer() shouldBe varselJsonNode["ident"].textValue()
        doknotifikasjon.value().getTittel() shouldBe varselJsonNode["eksternVarslingBestilling"]["epostVarslingstittel"].textValue()
        doknotifikasjon.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>${varselJsonNode["eksternVarslingBestilling"]["epostVarslingstittel"].textValue()}</title></head><body>${varselJsonNode["eksternVarslingBestilling"]["epostVarslingstekst"].textValue()}</body></html>\n"
        doknotifikasjon.value().getSmsTekst() shouldBe varselJsonNode["eksternVarslingBestilling"]["smsVarslingstekst"].textValue()
        doknotifikasjon.value().getPrefererteKanaler() shouldBe varselJsonNode["eksternVarslingBestilling"]["prefererteKanaler"].map { PrefererteKanal.valueOf(it.textValue()) }
        doknotifikasjon.value().getAntallRenotifikasjoner() shouldBe antallRenotifikasjoner
        doknotifikasjon.value().getRenotifikasjonIntervall() shouldBe renotifikasjonIntervall
    }

    @Test
    fun `Ignorerer varsel uten ekstern varsling satt`() {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselOpprettetJson(VarselType.Beskjed, "1", eksternVarsling = false))

        kafkaProducer.history().size shouldBe 0
    }

    @Test
    fun `Setter default-tekster hvis ikke oppgitt`() {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselAktivertJsonWithNullableFields(VarselType.Beskjed, "1"))
        testRapid.sendTestMessage(varselAktivertJsonWithNullableFields(VarselType.Oppgave, "2"))
        testRapid.sendTestMessage(varselAktivertJsonWithNullableFields(VarselType.Innboks, "3"))

        kafkaProducer.history().size shouldBe 3
        val doknotifikasjonBeskjed = kafkaProducer.history().first { it.key() == "1" }
        doknotifikasjonBeskjed.value().getTittel() shouldBe "Beskjed fra NAV"
        doknotifikasjonBeskjed.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Melding</title></head><body><p>Hei!</p><p>Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonBeskjed.value().getSmsTekst() shouldBe "Hei! Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder. Vennlig hilsen NAV"

        val doknotifikasjonOppgave = kafkaProducer.history().first { it.key() == "2" }
        doknotifikasjonOppgave.value().getTittel() shouldBe "Du har fått en oppgave fra NAV"
        doknotifikasjonOppgave.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Oppgave</title></head><body><p>Hei!</p><p>Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonOppgave.value().getSmsTekst() shouldBe "Hei! Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder. Vennlig hilsen NAV"

        val doknotifikasjonInnboks = kafkaProducer.history().first { it.key() == "3" }
        doknotifikasjonInnboks.value().getTittel() shouldBe "Du har fått en melding fra NAV"
        doknotifikasjonInnboks.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Innboks</title></head><body><p>Hei!</p><p>Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonInnboks.value().getSmsTekst() shouldBe "Hei! Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen. Vennlig hilsen NAV"
    }

    private fun setupVarselSink(testRapid: TestRapid) = VarselOpprettetSink(
        rapidsConnection = testRapid,
        doknotifikasjonProducer = doknotProducer
    )
}
