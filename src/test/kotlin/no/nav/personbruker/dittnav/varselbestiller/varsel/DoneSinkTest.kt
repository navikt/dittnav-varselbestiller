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

    private fun setupVarselSink(testRapid: TestRapid) = VarselSink(
        rapidsConnection = testRapid,
        doknotifikasjonProducer = doknotifikasjonProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true)
    )

    private fun setupDoneSink(testRapid: TestRapid) = DoneSink(
        rapidsConnection = testRapid,
        doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true)
    )

    private suspend fun bestilleringerFromDb(): List<Varselbestilling> {
        return database.dbQuery { getAllVarselbestilling() }
    }

    private fun doneJson(eventId: String) =
        """{
        "@event_name": "done",
        "eventId": "$eventId"
    }""".trimIndent()
}