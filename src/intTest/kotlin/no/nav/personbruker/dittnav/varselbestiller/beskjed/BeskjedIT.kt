package no.nav.personbruker.dittnav.varselbestiller.beskjed

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.common.KafkaEnvironment
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.common.getClient
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.config.Kafka
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.varselbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BeskjedIT {

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(Kafka.beskjedTopicName, Kafka.doknotifikasjonTopicName))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = H2Database()

    private val beskjedEvents = (1..10).map { AvroNokkelObjectMother.createNokkelWithEventId(it) to AvroBeskjedObjectMother.createBeskjedWithEksternVarsling(it, eksternVarsling = true) }.toMap()

    private val capturedDoknotifikasjonRecords = ArrayList<RecordKeyValueWrapper<String, Doknotifikasjon>>()

    private val producerNameAlias = "dittnav"
    private val client = getClient(producerNameAlias)
    private val metricsReporter = StubMetricsReporter()
    private val nameResolver = ProducerNameResolver(client, testEnvironment.eventHandlerURL)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

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
    fun `Should read Beskjed-events and send to varselbestiller-topic`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, Kafka.beskjedTopicName, beskjedEvents)
        } shouldBeEqualTo true

        `Read all Beskjed-events from our topic and verify that they have been sent to varselbestiller-topic`()

        beskjedEvents.all {
            capturedDoknotifikasjonRecords.contains(RecordKeyValueWrapper(it.key, it.value))
        }
    }

    fun `Read all Beskjed-events from our topic and verify that they have been sent to varselbestiller-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.BESKJED, true)
        val kafkaConsumer = KafkaConsumer<Nokkel, Beskjed>(consumerProps)

        val producerProps = Kafka.producerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON, true)
        val kafkaProducer = KafkaProducer<String, Doknotifikasjon>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doknotifikasjonTopicName, kafkaProducer)
        val doknotifikasjonProducer = Producer(kafkaProducerWrapper)
        val doknotifikasjonRepository = VarselbestillingRepository(database)

        val eventService = BeskjedEventService(doknotifikasjonProducer, doknotifikasjonRepository, metricsCollector)
        val consumer = Consumer(Kafka.beskjedTopicName, kafkaConsumer, eventService)

        kafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()

            `Wait until all beskjed events have been received by target topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all beskjed events have been received by target topic`() {
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON, true)
        val targetKafkaConsumer = KafkaConsumer<String, Doknotifikasjon>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<String, Doknotifikasjon>()

        val targetConsumer = Consumer(Kafka.doknotifikasjonTopicName, targetKafkaConsumer, capturingProcessor)

        var currentNumberOfRecords = 0

        targetConsumer.startPolling()

        while (currentNumberOfRecords < beskjedEvents.size) {
            runBlocking {
                currentNumberOfRecords = capturingProcessor.getEvents().size
                delay(100)
            }
        }

        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedDoknotifikasjonRecords.addAll(capturingProcessor.getEvents())
    }
}
