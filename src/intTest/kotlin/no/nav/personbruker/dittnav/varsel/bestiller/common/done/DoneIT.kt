package no.nav.personbruker.dittnav.varsel.bestiller.common.done

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.masking.ProducerNameScrubber
import no.nav.personbruker.dittnav.common.metrics.masking.PublicAliasResolver
import no.nav.personbruker.dittnav.varsel.bestiller.common.CapturingEventProcessor
import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.util.KafkaTestUtil
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType
import no.nav.personbruker.dittnav.varsel.bestiller.config.Kafka
import no.nav.personbruker.dittnav.varsel.bestiller.done.DoneEventService
import no.nav.personbruker.dittnav.varsel.bestiller.done.schema.AvroDoneObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.db.getProdusentnavn
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DoneIT {

    private val doneTopic = "dittnavDoneTopic"
    private val targetDoneTopic = "targetDoneTopic"

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(doneTopic, targetDoneTopic))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val doneEvents = (1..10).map { createNokkelWithEventId(it) to AvroDoneObjectMother.createDone(it) }.toMap()

    private val capturedDoneRecords = ArrayList<RecordKeyValueWrapper<Done>>()

    private val metricsReporter = StubMetricsReporter()
    private val database = H2Database()
    private val producerNameAliasResolver = PublicAliasResolver({ database.queryWithExceptionTranslation { getProdusentnavn() } })
    private val nameScrubber = ProducerNameScrubber(producerNameAliasResolver)
    private val metricsProbe = EventMetricsProbe(metricsReporter, nameScrubber)

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

    @Test
    fun `Should read Done-events and send to varsel-bestiller-topic`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, doneTopic, doneEvents)
        } shouldBeEqualTo true

        `Read all Done-events from our topic and verify that they have been sent to varsel-bestiller-topic`()

        doneEvents.all {
            capturedDoneRecords.contains(RecordKeyValueWrapper(it.key, it.value))
        }
    }

    fun `Read all Done-events from our topic and verify that they have been sent to varsel-bestiller-topic`() {
        val consumerProps = Kafka.consumerProps(testEnvironment, EventType.DONE, true)
        val kafkaConsumer = KafkaConsumer<Nokkel, Done>(consumerProps)

        val producerProps = Kafka.producerProps(testEnvironment, EventType.DONE, true)
        val kafkaProducer = KafkaProducer<Nokkel, Done>(producerProps)
        val producerWrapper = KafkaProducerWrapper(targetDoneTopic, kafkaProducer)

        val eventService = DoneEventService(producerWrapper, metricsProbe)
        val consumer = Consumer(doneTopic, kafkaConsumer, eventService)

        kafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()

            `Wait until all done events have been received by target topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all done events have been received by target topic`() {
        val targetConsumerProps = Kafka.consumerProps(testEnvironment, EventType.DONE, true)
        val targetKafkaConsumer = KafkaConsumer<Nokkel, Done>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<Done>()

        val targetConsumer = Consumer(targetDoneTopic, targetKafkaConsumer, capturingProcessor)

        var currentNumberOfRecords = 0

        targetConsumer.startPolling()

        while (currentNumberOfRecords < doneEvents.size) {
            runBlocking {
                currentNumberOfRecords = capturingProcessor.getEvents().size
                delay(100)
            }
        }

        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedDoneRecords.addAll(capturingProcessor.getEvents())
    }
}
