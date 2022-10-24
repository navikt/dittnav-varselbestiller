package no.nav.personbruker.dittnav.varselbestiller.varsel

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.createVarselbestillinger
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.deleteAllVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.getVarselbestillingForEventId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VarselInaktivertSinkTest {
    private val doknotifikasjonStoppKafkaProducer =
        KafkaTestUtil.createMockProducer<String, DoknotifikasjonStopp>().also {
            it.initTransactions()
        }
    private val database = LocalPostgresDatabase.cleanDb()
    private val varselbestillingRepository = VarselbestillingRepository(database)
    private val testRapid = TestRapid()

    private val doknotifikasjonStoppProducer = DoknotifikasjonStoppProducer(
        producer = KafkaProducerWrapper("stopic", doknotifikasjonStoppKafkaProducer),
        varselbestillingRepository = varselbestillingRepository
    )

    private val varselbestillingBeskjed: Varselbestilling =
        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(
            bestillingsId = "B-test-001",
            eventId = "001"
        )
    private val inaktivertVarselbestilling: Varselbestilling =
        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(
            bestillingsId = "O-test-001",
            eventId = "002"
        ).avbestill()

    @BeforeAll
    fun setup() {
        VarselInaktivertSink(
            rapidsConnection = testRapid,
            doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
            varselbestillingRepository = varselbestillingRepository
        )
    }

    @BeforeEach
    fun populate(){
        runBlocking {
            database.dbQuery {
                createVarselbestillinger(listOf(varselbestillingBeskjed,inaktivertVarselbestilling))
            }
        }
    }

    @AfterEach
    fun teardown(){
        runBlocking {
            database.dbQuery {
                deleteAllVarselbestilling()
            }
        }
    }
    @Test
    fun `plukker opp varselInaktivert hendelser og avbestiller varsel`() {
        val expextedEventId = varselbestillingBeskjed.eventId
        testRapid.sendTestMessage(varselInaktivertJson(expextedEventId))
        doknotifikasjonStoppKafkaProducer.history().size shouldBe 1
        runBlocking {
            database.dbQuery {
                getVarselbestillingForEventId(expextedEventId).apply {
                    requireNotNull(this)
                    avbestilt shouldBe true
                }
            }
        }
    }

    @Test
    fun `sender ikke melding for allerede inaktiverte varsel`() {
        val expextedEventId = inaktivertVarselbestilling.eventId
        testRapid.sendTestMessage(varselInaktivertJson(expextedEventId))
        doknotifikasjonStoppKafkaProducer.history().size shouldBe 0
    }
}

private fun Varselbestilling.avbestill(): Varselbestilling = copy(avbestilt = true)

//language=json
private fun varselInaktivertJson(eventId: String) = """
    {
    "@event_name": "varselInaktivert",
    "eventId": "$eventId"
 
    }
""".trimIndent()
