package no.nav.personbruker.dittnav.varselbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.deleteAllVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.getAllVarselbestilling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DoneSinkTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val varselbestillingRepository = VarselbestillingRepository(database)

    private val doknotifikasjonKafkaProducer = KafkaTestUtil.createMockProducer<String, Doknotifikasjon>().also {
        it.initTransactions()
    }

    private val doknotifikasjonStoppKafkaProducer = KafkaTestUtil.createMockProducer<String, DoknotifikasjonStopp>().also {
        it.initTransactions()
    }

    private val doknotifikasjonProducer = DoknotifikasjonProducer(
        producer = KafkaProducerWrapper("topic", doknotifikasjonKafkaProducer),
        varselbestillingRepository = varselbestillingRepository
    )

    private val doknotifikasjonStoppProducer = DoknotifikasjonStoppProducer(
        producer = KafkaProducerWrapper("stopic", doknotifikasjonStoppKafkaProducer),
        varselbestillingRepository = varselbestillingRepository
    )

    private val eventId = "1"
    private val varselJson = varselJson(VarselType.BESKJED, eventId)

    @BeforeEach
    fun setup() {
        runBlocking {
            database.dbQuery { deleteAllVarselbestilling() }
        }
        doknotifikasjonKafkaProducer.clear()
        doknotifikasjonStoppKafkaProducer.clear()

        val testRapid = TestRapid()
        setupVarselSink(testRapid)
        setupDoneSink(testRapid)

        testRapid.sendTestMessage(varselJson)
        testRapid.sendTestMessage(doneJson(eventId))
    }

    @Test
    fun `Sender doknotifikasjonStopp ved done`() = runBlocking {
        doknotifikasjonStoppKafkaProducer.history().size shouldBe 1

        val doknotifikasjonStopp = doknotifikasjonStoppKafkaProducer.history().first()
        doknotifikasjonStopp.key() shouldBe eventId
        doknotifikasjonStopp.value().getBestillingsId() shouldBe eventId
        doknotifikasjonStopp.value().getBestillerId() shouldBe ObjectMapper().readTree(varselJson)["appnavn"].textValue()
    }

    @Test
    fun `Setter varselbestilling til avbestilt ved done`() = runBlocking {
        val varselbestillinger = bestilleringerFromDb()
        varselbestillinger.size shouldBe 1

        val varselbestilling = varselbestillinger.first()
        varselbestilling.eventId shouldBe ObjectMapper().readTree(varselJson)["eventId"].textValue()
        varselbestilling.avbestilt shouldBe true
    }

    @Test
    fun `dryryn-modus når writeToDb er false`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid, writeToDb = true)
        setupDoneSink(testRapid, writeToDb = false)

        val eventId = "123"
        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, eventId))
        testRapid.sendTestMessage(doneJson(eventId))

        bestilleringerFromDb().first { it.bestillingsId == eventId }
        doknotifikasjonStoppKafkaProducer.history().find { it.key() == eventId } shouldBe null
    }

    private fun setupVarselSink(testRapid: TestRapid, writeToDb: Boolean = true) = VarselSink(
        rapidsConnection = testRapid,
        doknotifikasjonProducer = doknotifikasjonProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true),
        writeToDb = writeToDb
    )

    private fun setupDoneSink(testRapid: TestRapid, writeToDb: Boolean = true) = DoneSink(
        rapidsConnection = testRapid,
        doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true),
        writeToDb = writeToDb
    )

    private suspend fun bestilleringerFromDb(): List<Varselbestilling> {
        return database.dbQuery { getAllVarselbestilling() }
    }

    private fun doneJson(eventId: String) =
        """{
        "@event_name": "done",
        "eventId": "$eventId"
    }""".trimIndent()

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