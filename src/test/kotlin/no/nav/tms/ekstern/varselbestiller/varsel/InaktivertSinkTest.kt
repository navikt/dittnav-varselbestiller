package no.nav.tms.ekstern.varselbestiller.varsel

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselType
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.InaktivertSink
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InaktivertSinkTest {
    private val doknotifikasjonStoppKafkaProducer = KafkaTestUtil.createMockProducer<String, DoknotifikasjonStopp>()

    private val doknotifikasjonStoppProducer = DoknotEventProducer<DoknotifikasjonStopp>(
        topicName = "stopic",
        kafkaProducer = doknotifikasjonStoppKafkaProducer
    )

    private val eventId = "77"
    private val varselAktivertJson = varselOpprettetJson(VarselType.Beskjed, eventId)
    private lateinit var testRapid: TestRapid

    @BeforeEach
    fun setup() {
        doknotifikasjonStoppKafkaProducer.clear()
        testRapid = TestRapid()
    }

    @Test
    fun `Sender doknotifikasjonStopp ved inaktivert`() {
        setupInaktivertSink(testRapid)

        testRapid.sendTestMessage(varselAktivertJson)
        testRapid.sendTestMessage(varselInaktivertEventJson(eventId))
        testRapid.sendTestMessage(
            varselOpprettetJson(
                varselId = "99",
                type = VarselType.Beskjed,
                eksternVarsling = true,
                prefererteKanaler = "SMS"
            )
        )

        doknotifikasjonStoppKafkaProducer.history().assert {
            size shouldBe 1
            map { it.key() } shouldContainExactly listOf(eventId)
        }
    }

    private fun setupInaktivertSink(testRapid: TestRapid) = InaktivertSink(
        rapidsConnection = testRapid,
        doknotifikasjonStoppProducer
    )

    private fun varselInaktivertEventJson(varselId: String) =
        """{
        "@event_name": "inaktivert",
        "varselId": "$varselId",
        "produsent": {
            "cluster": "cluster",
            "namespace": "namespace",
            "appnavn": "appnavn"
        }
    }""".trimIndent()
}

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
