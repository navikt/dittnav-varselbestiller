package no.nav.personbruker.dittnav.varselbestiller.varsel

import io.kotest.matchers.shouldBe
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class VarselInaktivertSink {
    private val doknotifikasjonStopProducer = KafkaTestUtil.createMockProducer<String, Doknotifikasjon>()
    private val testRapid = TestRapid()


    @Test
    fun `plukker opp varselInaktivert hendelser og avbestiller varsel`() {
        val expextedEventId = "93643341"
        testRapid.sendTestMessage(varselInaktivertJson(expextedEventId))
        doknotifikasjonStopProducer.history().size shouldBe 1
    }
}

//language=json
private fun varselInaktivertJson(eventId: String) = """
    {
    "@event_name": "varselInaktivert",
    "eventId": "$eventId"
 
    }
""".trimIndent()