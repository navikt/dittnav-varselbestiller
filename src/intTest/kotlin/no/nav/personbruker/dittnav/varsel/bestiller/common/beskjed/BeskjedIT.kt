package no.nav.personbruker.dittnav.varsel.bestiller.common.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.util.KafkaTestUtil
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BeskjedIT {

    private val beskjedTopic = "dittnavBeskjedTopic"
    private val targetBeskjedTopic = "targetBeskjedTopic"

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(beskjedTopic, targetBeskjedTopic))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val beskjedEvents = (1..10).map { createNokkelWithEventId(it) to AvroBeskjedObjectMother.createBeskjed(it) }.toMap()

    private val capturedBeskjedRecords = ArrayList<RecordKeyValueWrapper<Beskjed>>()

    @BeforeAll
    fun setup() {
        embeddedEnv.start()
    }

    @AfterAll
    fun tearDown() {
        embeddedEnv.tearDown()
    }

    @Test
    fun `Started Kafka instance in memory`() {
        embeddedEnv.serverPark.status `should be equal to` KafkaEnvironment.ServerParkStatus.Started
    }

}
