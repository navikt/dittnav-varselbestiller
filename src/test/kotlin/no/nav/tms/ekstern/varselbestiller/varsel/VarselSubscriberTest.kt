package no.nav.tms.ekstern.varselbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselOpprettetSubscriber
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselType
import no.nav.tms.kafka.application.MessageBroadcaster
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VarselSubscriberTest {

    private val kafkaProducer = KafkaTestUtil.createMockProducer<String, Doknotifikasjon>()

    private val doknotProducer = DoknotEventProducer<Doknotifikasjon>("test-topic", kafkaProducer)
    private val testBroadcaster = MessageBroadcaster(listOf(setupVarselSink())) //TODO


    @BeforeEach
    fun cleanUp() {
        kafkaProducer.clear()
    }

    @ParameterizedTest
    @CsvSource("Beskjed,0,null", "Oppgave,1,7", "Innboks,1,4", nullValues = ["null"])
    fun `Bestiller ekstern varsling for varsel`(
        varselType: VarselType,
        antallRenotifikasjoner: Int,
        renotifikasjonIntervall: Int?
    ) {
        val varselJson = varselOpprettetJson(varselType, "1")
        testBroadcaster.broadcastJson(varselJson)

        kafkaProducer.history().size shouldBe 1

        val doknotifikasjon = kafkaProducer.history().first()
        val varselJsonNode = ObjectMapper().readTree(varselJson)
        doknotifikasjon.key() shouldBe varselJsonNode["varselId"].textValue()
        doknotifikasjon.value().bestillingsId shouldBe varselJsonNode["varselId"].textValue()
        doknotifikasjon.value().bestillerId shouldBe varselJsonNode["produsent"]["appnavn"].textValue()
        doknotifikasjon.value()
            .sikkerhetsnivaa shouldBe if (varselJsonNode["sensitivitet"].textValue() == "high") 4 else 3
        doknotifikasjon.value().fodselsnummer shouldBe varselJsonNode["ident"].textValue()
        doknotifikasjon.value()
            .tittel shouldBe varselJsonNode["eksternVarslingBestilling"]["epostVarslingstittel"].textValue()
        doknotifikasjon.value()
            .epostTekst shouldBe "<!DOCTYPE html><html><head><title>${varselJsonNode["eksternVarslingBestilling"]["epostVarslingstittel"].textValue()}</title></head><body>${varselJsonNode["eksternVarslingBestilling"]["epostVarslingstekst"].textValue()}</body></html>\n"
        doknotifikasjon.value()
            .smsTekst shouldBe varselJsonNode["eksternVarslingBestilling"]["smsVarslingstekst"].textValue()
        doknotifikasjon.value()
            .prefererteKanaler shouldBe varselJsonNode["eksternVarslingBestilling"]["prefererteKanaler"].map {
            PrefererteKanal.valueOf(
                it.textValue()
            )
        }
        doknotifikasjon.value().antallRenotifikasjoner shouldBe antallRenotifikasjoner
        doknotifikasjon.value().renotifikasjonIntervall shouldBe renotifikasjonIntervall
    }

    @Test
    fun `Ignorerer varsel uten ekstern varsling satt`() {

        testBroadcaster.broadcastJson(varselOpprettetJson(VarselType.Beskjed, "1", eksternVarsling = false))

        kafkaProducer.history().size shouldBe 0
    }

    @Test
    fun `Setter default-tekster hvis ikke oppgitt`() {

        testBroadcaster.broadcastJson(varselAktivertJsonWithNullableFields(VarselType.Beskjed, "1"))
        testBroadcaster.broadcastJson(varselAktivertJsonWithNullableFields(VarselType.Oppgave, "2"))
        testBroadcaster.broadcastJson(varselAktivertJsonWithNullableFields(VarselType.Innboks, "3"))

        kafkaProducer.history().size shouldBe 3
        val doknotifikasjonBeskjed = kafkaProducer.history().first { it.key() == "1" }
        doknotifikasjonBeskjed.value().getTittel() shouldBe "Beskjed fra NAV"
        doknotifikasjonBeskjed.value()
            .epostTekst shouldBe "<!DOCTYPE html><html><head><title>Melding</title></head><body><p>Hei!</p><p>Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonBeskjed.value()
            .smsTekst shouldBe "Hei! Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder. Vennlig hilsen NAV"

        val doknotifikasjonOppgave = kafkaProducer.history().first { it.key() == "2" }
        doknotifikasjonOppgave.value().getTittel() shouldBe "Du har fått en oppgave fra NAV"
        doknotifikasjonOppgave.value()
            .epostTekst shouldBe "<!DOCTYPE html><html><head><title>Oppgave</title></head><body><p>Hei!</p><p>Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonOppgave.value()
            .smsTekst shouldBe "Hei! Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder. Vennlig hilsen NAV"

        val doknotifikasjonInnboks = kafkaProducer.history().first { it.key() == "3" }
        doknotifikasjonInnboks.value().tittel shouldBe "Du har fått en melding fra NAV"
        doknotifikasjonInnboks.value()
            .epostTekst shouldBe "<!DOCTYPE html><html><head><title>Innboks</title></head><body><p>Hei!</p><p>Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonInnboks.value()
            .smsTekst shouldBe "Hei! Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen. Vennlig hilsen NAV"
    }

    //TODO
    private fun setupVarselSink() = VarselOpprettetSubscriber(
        doknotifikasjonProducer = doknotProducer
    )
}
