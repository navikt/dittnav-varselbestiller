package no.nav.personbruker.dittnav.varsel.bestiller

import kotlinx.coroutines.runBlocking
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.*
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.kafka.util.KafkaTestUtil
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkel
import org.amshove.kluent.`should equal`
import org.amshove.kluent.shouldEqualTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class EndToEndTestIT {

    private val database = H2Database()

    private val topicen = "endToEndTestItBeskjed"
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(topicen))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val metricsReporter = StubMetricsReporter()
    private val nameResolver = ProducerNameResolver(database)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsProbe = EventMetricsProbe(metricsReporter, nameScrubber)

    private val adminClient = embeddedEnv.adminClient

    private val events = (1..10).map { createNokkel(it) to AvroBeskjedObjectMother.createBeskjed(it) }.toMap()

    init {
       embeddedEnv.start()
    }

    @AfterAll
    fun tearDown() {
        adminClient?.close()
        embeddedEnv.tearDown()
    }

    @Test
    fun `Kafka instansen i minnet har blitt staret`() {
        embeddedEnv.serverPark.status `should equal` KafkaEnvironment.ServerParkStatus.Started
    }



    fun `Produserer noen testeventer`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, topicen, events)
        } shouldEqualTo true


    }





}
