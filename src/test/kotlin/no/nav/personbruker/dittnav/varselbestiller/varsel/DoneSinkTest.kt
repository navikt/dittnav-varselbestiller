package no.nav.personbruker.dittnav.varselbestiller.varsel

import io.kotest.matchers.collections.shouldContainExactly
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
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
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

    private val doknotifikasjonStoppKafkaProducer =
        KafkaTestUtil.createMockProducer<String, DoknotifikasjonStopp>().also {
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

    private val eventId = "77"
    private val varselAktivertJson = varselAktivertJson(VarselType.BESKJED, eventId)
    private lateinit var testRapid: TestRapid

    @BeforeEach
    fun setup() {
        runBlocking {
            database.dbQuery { deleteAllVarselbestilling() }
        }
        doknotifikasjonKafkaProducer.clear()
        doknotifikasjonStoppKafkaProducer.clear()
        testRapid = TestRapid()
        setupVarselSink(testRapid)
    }

    @Test
    fun `Sender doknotifikasjonStopp ved inaktivert`() {
        runBlocking {
            setupDoneSink(testRapid, true)

            testRapid.sendTestMessage(varselAktivertJson)
            testRapid.sendTestMessage(varselInaktivertEventJson(eventId))
            testRapid.sendTestMessage(
                varselAktivertJson(
                    eventId = "99",
                    type = VarselType.BESKJED,
                    eksternVarsling = true,
                    prefererteKanaler = "SMS"
                )
            )


            doknotifikasjonStoppKafkaProducer.history().assert {
                size shouldBe 1
                map { it.key() } shouldContainExactly listOf(eventId)
            }
        }
    }

    @Test
    fun `Setter varselbestilling til avbestilt ved inaktivert-event`() {
        runBlocking {
            setupDoneSink(testRapid, true)

            testRapid.sendTestMessage(varselAktivertJson)
            testRapid.sendTestMessage(
                varselAktivertJson(
                    eventId = "99",
                    type = VarselType.BESKJED,
                    eksternVarsling = true,
                    prefererteKanaler = "SMS"
                )
            )


            testRapid.sendTestMessage(varselInaktivertEventJson(eventId))

            bestilleringerFromDb().filter { it.avbestilt }.assert {
                size shouldBe 1
                map { it.eventId } shouldContainExactly listOf(eventId)
            }
        }
    }

    @Test
    fun `Sender ikke doknotifikasjonStopp for duplikat inaktivert-event`() = runBlocking {
        setupDoneSink(testRapid, true)

        testRapid.sendTestMessage(varselAktivertJson)
        testRapid.sendTestMessage(
            varselAktivertJson(
                eventId = "99",
                type = VarselType.BESKJED,
                eksternVarsling = true,
                prefererteKanaler = "SMS"
            )
        )

        testRapid.sendTestMessage(varselInaktivertEventJson(eventId))
        testRapid.sendTestMessage(varselInaktivertEventJson(eventId))

        doknotifikasjonStoppKafkaProducer.history().size shouldBe 1
    }

    @Test
    fun `Skal transformere fra Varselbestilling til DoknotifikasjonStopp`() {
        val varselbestilling = VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "001")
        val doknotifikasjonStopp = createDoknotifikasjonStopp(varselbestilling)
        doknotifikasjonStopp.getBestillingsId() shouldBe varselbestilling.bestillingsId
        doknotifikasjonStopp.getBestillerId() shouldBe varselbestilling.appnavn
    }

    private fun setupVarselSink(testRapid: TestRapid) = VarselSink(
        rapidsConnection = testRapid,
        doknotifikasjonProducer = doknotifikasjonProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true)
    )

    private fun setupDoneSink(testRapid: TestRapid, includeVarselInaktivert: Boolean = false) = DoneSink(
        rapidsConnection = testRapid,
        doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
        varselbestillingRepository = varselbestillingRepository,
        rapidMetricsProbe = mockk(relaxed = true),
        includeVarselInaktivert = includeVarselInaktivert
    )

    private suspend fun bestilleringerFromDb(): List<Varselbestilling> {
        return database.dbQuery { getAllVarselbestilling() }
    }

    private fun varselInaktivertEventJson(eventId: String) =
        """{
        "@event_name": "inaktivert",
        "eventId": "$eventId"
    }""".trimIndent()
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
