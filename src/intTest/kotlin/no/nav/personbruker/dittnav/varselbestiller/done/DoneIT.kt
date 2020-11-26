package no.nav.personbruker.dittnav.varselbestiller.done

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.common.KafkaEnvironment
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.varselbestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.config.Kafka
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
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

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(Kafka.doneTopicName, Kafka.doknotifikasjonStopTopicName))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = H2Database()

    private val doneEvents = (1..10).map { AvroNokkelObjectMother.createNokkelWithEventId(it) to AvroDoneObjectMother.createDone(it) }.toMap()
    private val varselbestillinger = listOf(VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-001", eventId = "1", fodselsnummer = "12345"),
                                                                VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-002", eventId = "2", fodselsnummer = "12345"),
                                                                VarselbestillingObjectMother.createVarselbestilling(bestillingsId = "B-test-003", eventId = "3", fodselsnummer = "12345"))

    private val capturedDoknotifikasjonStopRecords = ArrayList<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>()

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
            KafkaTestUtil.produceEvents(testEnvironment, Kafka.doneTopicName, doneEvents)
        } shouldBeEqualTo true

        `Read all Done-events from our topic and verify that they have been sent to DoknotifikasjonStopp-topic`()

        doneEvents.all {
            capturedDoknotifikasjonStopRecords.contains(RecordKeyValueWrapper(it.key, it.value))
        }
    }

    fun `Read all Done-events from our topic and verify that they have been sent to DoknotifikasjonStopp-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DONE, true)
        val kafkaConsumer = KafkaConsumer<Nokkel, Done>(consumerProps)

        val producerProps = Kafka.producerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON_STOPP, true)
        val kafkaProducer = KafkaProducer<String, DoknotifikasjonStopp>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doknotifikasjonStopTopicName, kafkaProducer)
        val doknotifikasjonStoppProducer = DoknotifikasjonStoppProducer(kafkaProducerWrapper)
        val doknotifikasjonRepository = VarselbestillingRepository(database)

        val eventService = DoneEventService(doknotifikasjonStoppProducer, doknotifikasjonRepository)
        val consumer = Consumer(Kafka.doneTopicName, kafkaConsumer, eventService)

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

        val targetConsumer = Consumer(Kafka.doknotifikasjonStopTopicName, targetKafkaConsumer, capturingProcessor)

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
