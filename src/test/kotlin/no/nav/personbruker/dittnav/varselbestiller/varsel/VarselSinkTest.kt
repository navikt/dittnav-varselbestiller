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

        val varselJson = varselJson(VarselType.BESKJED, "1")
        testRapid.sendTestMessage(varselJson)

        doknotifikasjonKafkaProducer.history().size shouldBe 1
        val varselbestillinger = bestilleringerFromDb()
        varselbestillinger.size shouldBe 1

        val varselbestilling = varselbestillinger.first()
        val varselJsonNode = ObjectMapper().readTree(varselJson)
        varselbestilling.appnavn shouldBe varselJsonNode["appnavn"].textValue()
        varselbestilling.bestillingsId shouldBe varselJsonNode["eventId"].textValue()
        varselbestilling.eventId shouldBe varselJsonNode["eventId"].textValue()
        varselbestilling.fodselsnummer shouldBe varselJsonNode["fodselsnummer"].textValue()
        varselbestilling.namespace shouldBe varselJsonNode["namespace"].textValue()
        varselbestilling.appnavn shouldBe varselJsonNode["appnavn"].textValue()
        varselbestilling.prefererteKanaler shouldBe varselJsonNode["prefererteKanaler"].map { it.textValue() }
        varselbestilling.avbestilt shouldBe false
    }

    @ParameterizedTest
    @CsvSource("BESKJED,0,null", "OPPGAVE,1,7", "INNBOKS,1,4", nullValues = ["null"])
    fun `Bestiller ekstern varsling for varsel`(varselType: VarselType, antallRenotifikasjoner: Int, renotifikasjonIntervall: Int?) = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        val varselJson = varselJson(varselType, "1")
        testRapid.sendTestMessage(varselJson)

        doknotifikasjonKafkaProducer.history().size shouldBe 1
        bestilleringerFromDb().size shouldBe 1

        val doknotifikasjon = doknotifikasjonKafkaProducer.history().first()
        val varselJsonNode = ObjectMapper().readTree(varselJson)
        doknotifikasjon.key() shouldBe varselJsonNode["eventId"].textValue()
        doknotifikasjon.value().getBestillingsId() shouldBe varselJsonNode["eventId"].textValue()
        doknotifikasjon.value().getBestillerId() shouldBe varselJsonNode["appnavn"].textValue()
        doknotifikasjon.value().getSikkerhetsnivaa() shouldBe varselJsonNode["sikkerhetsnivaa"].intValue()
        doknotifikasjon.value().getFodselsnummer() shouldBe varselJsonNode["fodselsnummer"].textValue()
        doknotifikasjon.value().getTittel() shouldBe varselJsonNode["epostVarslingstittel"].textValue()
        doknotifikasjon.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>${varselJsonNode["epostVarslingstittel"].textValue()}</title></head><body>${varselJsonNode["epostVarslingstekst"].textValue()}</body></html>\n"
        doknotifikasjon.value().getSmsTekst() shouldBe varselJsonNode["smsVarslingstekst"].textValue()
        doknotifikasjon.value().getPrefererteKanaler() shouldBe varselJsonNode["prefererteKanaler"].map { PrefererteKanal.valueOf(it.textValue()) }
        doknotifikasjon.value().getAntallRenotifikasjoner() shouldBe antallRenotifikasjoner
        doknotifikasjon.value().getRenotifikasjonIntervall() shouldBe renotifikasjonIntervall
    }

    @Test
    fun `Ignorerer duplikate varsler`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1"))
        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1"))

        testRapid.sendTestMessage(varselJson(VarselType.OPPGAVE, "2"))
        testRapid.sendTestMessage(varselJson(VarselType.OPPGAVE, "2"))

        testRapid.sendTestMessage(varselJson(VarselType.INNBOKS, "3"))
        testRapid.sendTestMessage(varselJson(VarselType.INNBOKS, "3"))

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

        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1", eksternVarsling = false))

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 0
    }

    @Test
    fun `Setter default-tekster hvis ikke oppgitt`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1", smsVarslingstekst = null, epostVarslingstekst = null, epostVarslingstittel = null))
        testRapid.sendTestMessage(varselJson(VarselType.OPPGAVE, "2", smsVarslingstekst = null, epostVarslingstekst = null, epostVarslingstittel = null))
        testRapid.sendTestMessage(varselJson(VarselType.INNBOKS, "3", smsVarslingstekst = null, epostVarslingstekst = null, epostVarslingstittel = null))

        doknotifikasjonKafkaProducer.history().size shouldBe 3
        val doknotifikasjonBeskjed = doknotifikasjonKafkaProducer.history().first { it.key() == "1" }
        doknotifikasjonBeskjed.value().getTittel() shouldBe "Beskjed fra NAV"
        doknotifikasjonBeskjed.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Melding</title></head><body><p>Hei!</p><p>Du har fått en ny beskjed fra NAV. Logg inn på nav.no for å se hva beskjeden gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonBeskjed.value().getSmsTekst() shouldBe "Hei! Du har fått en ny beskjed fra NAV. Logg inn for å se hva beskjeden gjelder. Vennlig hilsen NAV\n"

        val doknotifikasjonOppgave = doknotifikasjonKafkaProducer.history().first { it.key() == "2" }
        doknotifikasjonOppgave.value().getTittel() shouldBe "Du har fått en oppgave fra NAV"
        doknotifikasjonOppgave.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Oppgave</title></head><body><p>Hei!</p><p>Du har fått en ny oppgave fra NAV. Logg inn på nav.no for å se hva oppgaven gjelder.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonOppgave.value().getSmsTekst() shouldBe "Hei! Du har fått en ny oppgave fra NAV. Logg inn på nav.no for å se hva oppgaven gjelder. Vennlig hilsen NAV\n"

        val doknotifikasjonInnboks = doknotifikasjonKafkaProducer.history().first { it.key() == "3" }
        doknotifikasjonInnboks.value().getTittel() shouldBe "Du har fått en melding fra NAV"
        doknotifikasjonInnboks.value().getEpostTekst() shouldBe "<!DOCTYPE html><html><head><title>Innboks</title></head><body><p>Hei!</p><p>Du har fått en ny melding fra NAV. Logg inn på nav.no for å lese meldingen.</p><p>Vennlig hilsen</p><p>NAV</p></body></html>\n"
        doknotifikasjonInnboks.value().getSmsTekst() shouldBe "Hei! Du har fått en ny melding fra NAV. Logg inn på nav.no for å lese meldingen. Vennlig hilsen NAV\n"
    }

    @Test
    fun `dryryn-modus når writeToDb er false`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid, writeToDb = false)

        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1"))

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 0
    }

    private fun setupVarselSink(testRapid: TestRapid, writeToDb: Boolean = true) = VarselSink(
        rapidsConnection = testRapid,
        doknotifikasjonProducer = doknotifikasjonProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true),
        writeToDb = writeToDb
    )

    private suspend fun bestilleringerFromDb(): List<Varselbestilling> {
        return database.dbQuery { getAllVarselbestilling() }
    }

    private fun varselJson(
        type: VarselType,
        eventId: String,
        eksternVarsling: Boolean = true,
        smsVarslingstekst: String? = "smstekst",
        epostVarslingstekst: String? = "eposttekst",
        epostVarslingstittel: String? = "eposttittel"
    ) = """{
        "@event_name": "${type.name.lowercase()}",
        "namespace": "ns",
        "appnavn": "app",
        "eventId": "$eventId",
        "forstBehandlet": "2022-02-01T00:00:00",
        "fodselsnummer": "12345678910",
        "tekst": "Tekst",
        "link": "url",
        "sikkerhetsnivaa": 4,
        "synligFremTil": "2022-04-01T00:00:00",
        "aktiv": true,
        "eksternVarsling": $eksternVarsling,
        "prefererteKanaler": ["EPOST", "SMS"],
        "smsVarslingstekst": ${smsVarslingstekst?.let { "\"$smsVarslingstekst\"" }},
        "epostVarslingstekst": ${epostVarslingstekst?.let { "\"$epostVarslingstekst\"" }},
        "epostVarslingstittel": ${epostVarslingstittel?.let { "\"$epostVarslingstittel\"" }}
    }""".trimIndent()
}