package no.nav.personbruker.dittnav.varselbestiller.varsel

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.getVarselbestillingForEventId
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.getVarselbestillingerForEventIds
import org.junit.jupiter.api.BeforeAll
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

    @BeforeAll
    fun setup() {
        VarselInaktivertSink(
            rapidsConnection = testRapid,
            doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
            varselbestillingRepository = varselbestillingRepository
        )
    }

    @Test
    fun `plukker opp varselInaktivert hendelser og avbestiller varsel`() {
        val expextedEventId = "93643341"
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
}

//language=json
private fun varselInaktivertJson(eventId: String) = """
    {
    "@event_name": "varselInaktivert",
    "eventId": "$eventId"
 
    }
""".trimIndent()
