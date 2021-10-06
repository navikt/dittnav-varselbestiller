package no.nav.personbruker.dittnav.varselbestiller.done

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.common.KafkaEnvironment
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.getClient
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.*
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.config.Kafka
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.varselbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.createVarselbestillinger
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.deleteAllVarselbestilling
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DoneIT {

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(KafkaTestTopics.doneTopicName, KafkaTestTopics.doknotifikasjonStopTopicName))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = LocalPostgresDatabase()

    private val doneEvents = (1..10).map { AvroNokkelInternObjectMother.createNokkelInternWithEventId(it) to AvroDoneInternObjectMother.createDoneIntern() }.toMap()
    private val varselbestillinger = listOf(VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-1", eventId = "1"),
                                                                VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-2", eventId = "2"),
                                                                VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-3", eventId = "3"))

    private val capturedDoknotifikasjonStopRecords = ArrayList<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>()

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
        `Delete varselbestillinger from DB`()
    }

    @Test
    fun `Started Kafka instance in memory`() {
        embeddedEnv.serverPark.status `should be equal to` KafkaEnvironment.ServerParkStatus.Started
    }

    @Test
    fun `Should read Done-events and send to DoknotifikasjonStopp-topic`() {
        `Create varselbestillinger in DB`()

        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, KafkaTestTopics.doneTopicName, doneEvents)
        } shouldBeEqualTo true

        `Read all Done-events from our topic and verify that they have been sent to DoknotifikasjonStopp-topic`()

        doneEvents.all {
            capturedDoknotifikasjonStopRecords.contains(RecordKeyValueWrapper(it.key, it.value))
        }
    }

    fun `Read all Done-events from our topic and verify that they have been sent to DoknotifikasjonStopp-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DONE_INTERN, true)
        val kafkaConsumer = KafkaConsumer<NokkelIntern, DoneIntern>(consumerProps)

        val producerProps = Kafka.producerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON_STOPP, true)
        val kafkaProducer = KafkaProducer<String, DoknotifikasjonStopp>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(KafkaTestTopics.doknotifikasjonStopTopicName, kafkaProducer)
        val doknotifikasjonRepository = VarselbestillingRepository(database)
        val doknotifikasjonStoppProducer = DoknotifikasjonStoppProducer(kafkaProducerWrapper, doknotifikasjonRepository)

        val eventService = DoneEventService(doknotifikasjonStoppProducer, doknotifikasjonRepository, metricsCollector)
        val consumer = Consumer(KafkaTestTopics.doneTopicName, kafkaConsumer, eventService)

        kafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()
            `Wait until all DoknotifikasjonStopp-events have been received by target topic`()
            consumer.stopPolling()
        }
    }

    private fun `Create varselbestillinger in DB`() {
        runBlocking {
            database.dbQuery {
                createVarselbestillinger(varselbestillinger)
            }
        }
    }

    private fun `Delete varselbestillinger from DB`() {
        runBlocking {
            database.dbQuery {
                deleteAllVarselbestilling()
            }
        }
    }

    private fun `Wait until all DoknotifikasjonStopp-events have been received by target topic`() {
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON_STOPP, true)
        val targetKafkaConsumer = KafkaConsumer<String, DoknotifikasjonStopp>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<String, DoknotifikasjonStopp>()

        val targetConsumer = Consumer(KafkaTestTopics.doknotifikasjonStopTopicName, targetKafkaConsumer, capturingProcessor)

        var currentNumberOfRecords = 0

        targetConsumer.startPolling()

        while (currentNumberOfRecords < varselbestillinger.size) {
            runBlocking {
                currentNumberOfRecords = capturingProcessor.getEvents().size
                delay(100)
            }
        }

        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedDoknotifikasjonStopRecords.addAll(capturingProcessor.getEvents())
    }
}
