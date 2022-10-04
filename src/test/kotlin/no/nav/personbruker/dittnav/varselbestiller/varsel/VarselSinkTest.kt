package no.nav.personbruker.dittnav.varselbestiller.varsel

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.deleteAllVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.getAllVarselbestilling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class VarselSinkTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val eksternVarselRepository = VarselbestillingRepository(database)

    private val doknotifikasjonKafkaProducer = KafkaTestUtil.createMockProducer<String, Doknotifikasjon>().also {
        it.initTransactions()
    }

    private val doknotifikasjonProducer = DoknotifikasjonProducer(
        producer = KafkaProducerWrapper("topic", doknotifikasjonKafkaProducer),
        varselbestillingRepository = eksternVarselRepository
    )

    @BeforeEach
    fun resetDb() {
        runBlocking {
            database.dbQuery { deleteAllVarselbestilling() }
        }
    }

    @Test
    fun `Bestiller og lagrer ekstern varsling`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1"))
        testRapid.sendTestMessage(varselJson(VarselType.OPPGAVE, "2"))
        testRapid.sendTestMessage(varselJson(VarselType.INNBOKS, "3"))

        val varselbestillinger = bestilleringerFromDb()
        varselbestillinger.size shouldBe 3
        doknotifikasjonKafkaProducer.history().size shouldBe 3

        val varselbestilling = varselbestillinger.first()
    }

    @Test
    @Disabled
    fun `Ignorerer duplikat varsel`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1"))
        testRapid.sendTestMessage(varselJson(VarselType.BESKJED, "1"))

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 1
    }

    @Test
    @Disabled
    fun `Ignorerer varsel uten ekstern varsling satt`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        //testRapid.sendTestMessage(beskjedJson)

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 1
    }

    @Test
    @Disabled
    fun `Takler null-felter`() = runBlocking {
        val testRapid = TestRapid()
        setupVarselSink(testRapid)

        //testRapid.sendTestMessage(varselMedNullJson)

        val eksternVarselBestillinger = bestilleringerFromDb()
        eksternVarselBestillinger.size shouldBe 1
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
        rapidMetricsProbe = mockk(relaxed = true),
        writeToDb = writeToDb
    )

    private suspend fun bestilleringerFromDb(): List<Varselbestilling> {
        return database.dbQuery { getAllVarselbestilling() }
    }

    private fun varselJson(type: VarselType, eventId: String) = """{
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
        "eksternVarsling": true,
        "prefererteKanaler": ["EPOST", "SMS"]
    }""".trimIndent()
}