package no.nav.tms.ekstern.varselbestiller.varsel

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselType
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselInaktivertSink
import no.nav.tms.kafka.application.MessageBroadcaster
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InaktivertSubscriberTest {
    private val doknotifikasjonStoppKafkaProducer = KafkaTestUtil.createMockProducer<String, DoknotifikasjonStopp>()

    private val doknotifikasjonStoppProducer = DoknotEventProducer<DoknotifikasjonStopp>(
        topicName = "stopic",
        kafkaProducer = doknotifikasjonStoppKafkaProducer
    )

    private val testBraodcaster = MessageBroadcaster(
        listOf(
            VarselInaktivertSink(
                doknotifikasjonStoppProducer
            )
        )
    )
    private val eventId = "77"
    private val varselAktivertJson = varselOpprettetJson(VarselType.Beskjed, eventId)


    @BeforeEach
    fun setup() {
        doknotifikasjonStoppKafkaProducer.clear()
    }

    @Test
    fun `Sender doknotifikasjonStopp ved inaktivert`() {

        testBraodcaster.broadcastJson(varselAktivertJson)
        testBraodcaster.broadcastJson(varselInaktivertEventJson(eventId))
        testBraodcaster.broadcastJson(
            varselOpprettetJson(
                varselId = "99",
                type = VarselType.Beskjed,
                eksternVarsling = true,
                prefererteKanaler = """["SMS"]"""
            )
        )

        doknotifikasjonStoppKafkaProducer.history().assert {
            size shouldBe 1
            map { it.key() } shouldContainExactly listOf(eventId)
        }
    }

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
