package no.nav.personbruker.dittnav.varselbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.deleteAllVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.getAllVarselbestilling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VarselSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val varselbestillingRepository = VarselbestillingRepository(database)

    private val doknotifikasjonKafkaProducer = KafkaTestUtil.createMockProducer<String, Doknotifikasjon>().also {
        it.initTransactions()
    }

    private val doknotifikasjonProducer = DoknotifikasjonProducer(
        producer = KafkaProducerWrapper("topic", doknotifikasjonKafkaProducer),
        varselbestillingRepository = varselbestillingRepository
    )

    @BeforeEach
    fun resetDb() {
        runBlocking {
            database.dbQuery { deleteAllVarselbestilling() }
        }
        doknotifikasjonKafkaProducer.clear()
    }

    @Test
    fun `lagrer ekstern varsling`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        val varselJson = varselAktivertJson(VarselType.Beskjed, "1")
        testRapid.sendTestMessage(varselJson)

        doknotifikasjonKafkaProducer.history().size shouldBe 1
        val varselbestillinger = bestilleringerFromDb()
        varselbestillinger.size shouldBe 1

        val varselbestilling = varselbestillinger.first()
        val varselJsonNode = ObjectMapper().readTree(varselJson)
        varselbestilling.appnavn shouldBe varselJsonNode["produsent"]["appnavn"].textValue()
        varselbestilling.namespace shouldBe varselJsonNode["produsent"]["namespace"].textValue()
        varselbestilling.bestillingsId shouldBe varselJsonNode["varselId"].textValue()
        varselbestilling.eventId shouldBe varselJsonNode["varselId"].textValue()
        varselbestilling.fodselsnummer shouldBe varselJsonNode["ident"].textValue()
        varselbestilling.prefererteKanaler shouldBe varselJsonNode["eksternVarslingBestilling"]["prefererteKanaler"].map { it.textValue() }
        varselbestilling.avbestilt shouldBe false
    }

    @ParameterizedTest
    @CsvSource("Beskjed,0,null", "Oppgave,1,7", "Innboks,1,4", nullValues = ["null"])
    fun `Bestiller ekstern varsling for varsel`(varselType: VarselType, antallRenotifikasjoner: Int, renotifikasjonIntervall: Int?) = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        val varselJson = varselAktivertJson(varselType, "1")
        testRapid.sendTestMessage(varselJson)

        doknotifikasjonKafkaProducer.history().size shouldBe 1
        bestilleringerFromDb().size shouldBe 1

        val doknotifikasjon = doknotifikasjonKafkaProducer.history().first()
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
    fun `Ignorerer duplikate varsler`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselAktivertJson(VarselType.Beskjed, "1"))
        testRapid.sendTestMessage(varselAktivertJson(VarselType.Beskjed, "1"))

        testRapid.sendTestMessage(varselAktivertJson(VarselType.Oppgave, "2"))
        testRapid.sendTestMessage(varselAktivertJson(VarselType.Oppgave, "2"))

        testRapid.sendTestMessage(varselAktivertJson(VarselType.Innboks, "3"))
        testRapid.sendTestMessage(varselAktivertJson(VarselType.Innboks, "3"))

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 3
        eksternVarselBestillinger.map { it.bestillingsId }.toSet() shouldBe setOf("1", "2", "3")

        doknotifikasjonKafkaProducer.history().size shouldBe 3
        doknotifikasjonKafkaProducer.history().map { it.key() }.toSet() shouldBe setOf("1", "2", "3")

    }

    @Test
    fun `Ignorerer varsel uten ekstern varsling satt`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselAktivertJson(VarselType.Beskjed, "1", eksternVarsling = false))

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 0
    }

    @Test
    fun `Setter default-tekster hvis ikke oppgitt`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselAktivertJsonWithNullableFields(VarselType.Beskjed, "1"))
        testRapid.sendTestMessage(varselAktivertJsonWithNullableFields(VarselType.Oppgave, "2"))
        testRapid.sendTestMessage(varselAktivertJsonWithNullableFields(VarselType.Innboks, "3"))

        doknotifikasjonKafkaProducer.history().size shouldBe 3
        val doknotifikasjonBeskjed = doknotifikasjonKafkaProducer.history().first { it.key() == "1" }
        doknotifikasjonBeskjed.value().getTittel() shouldBe "Beskjed fra NAV"
        doknotifikasjonBeskjed.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Melding</title></head><body><p>Hei!</p><p>Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonBeskjed.value().getSmsTekst() shouldBe "Hei! Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder. Vennlig hilsen NAV"

        val doknotifikasjonOppgave = doknotifikasjonKafkaProducer.history().first { it.key() == "2" }
        doknotifikasjonOppgave.value().getTittel() shouldBe "Du har fått en oppgave fra NAV"
        doknotifikasjonOppgave.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Oppgave</title></head><body><p>Hei!</p><p>Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonOppgave.value().getSmsTekst() shouldBe "Hei! Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder. Vennlig hilsen NAV"

        val doknotifikasjonInnboks = doknotifikasjonKafkaProducer.history().first { it.key() == "3" }
        doknotifikasjonInnboks.value().getTittel() shouldBe "Du har fått en melding fra NAV"
        doknotifikasjonInnboks.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Innboks</title></head><body><p>Hei!</p><p>Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonInnboks.value().getSmsTekst() shouldBe "Hei! Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen. Vennlig hilsen NAV"
    }

    private fun setupVarselSink(testRapid: TestRapid) = VarselSink(
        rapidsConnection = testRapid,
        doknotifikasjonProducer = doknotifikasjonProducer,
        varselbestillingRepository = varselbestillingRepository
    )

    private suspend fun bestilleringerFromDb(): List<Varselbestilling> {
        return database.dbQuery { getAllVarselbestilling() }
    }
}
