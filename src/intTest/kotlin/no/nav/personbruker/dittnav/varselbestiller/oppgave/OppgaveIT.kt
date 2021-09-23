package no.nav.personbruker.dittnav.varselbestiller.oppgave

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.common.KafkaEnvironment
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.CapturingEventProcessor
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.getClient
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.*
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaEmbed
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.config.Kafka
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
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

class OppgaveIT {

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(KafkaTestTopics.oppgaveTopicName, KafkaTestTopics.doknotifikasjonTopicName))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val database = LocalPostgresDatabase()

    private val oppgaveEvents = (1..10).map { AvroNokkelObjectMother.createNokkelWithEventId(it) to AvroOppgaveObjectMother.createOppgaveWithEksternVarsling(eksternVarsling = true) }.toMap()

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
    fun `Should read Oppgave-events and send to varselbestiller-topic`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, KafkaTestTopics.oppgaveTopicName, oppgaveEvents)
        } shouldBeEqualTo true

        `Read all Oppgave-events from our topic and verify that they have been sent to varselbestiller-topic`()

        oppgaveEvents.all {
            capturedDoknotifikasjonRecords.contains(RecordKeyValueWrapper(it.key, it.value))
        }
    }

    fun `Read all Oppgave-events from our topic and verify that they have been sent to varselbestiller-topic`() {
        val consumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.OPPGAVE, true)
        val kafkaConsumer = KafkaConsumer<Nokkel, Oppgave>(consumerProps)

        val producerProps = Kafka.producerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON, true)
        val kafkaProducer = KafkaProducer<String, Doknotifikasjon>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(KafkaTestTopics.doknotifikasjonTopicName, kafkaProducer)
        val doknotifikasjonRepository = VarselbestillingRepository(database)
        val doknotifikasjonProducer = DoknotifikasjonProducer(kafkaProducerWrapper, doknotifikasjonRepository)

        val eventService = OppgaveEventService(doknotifikasjonProducer, doknotifikasjonRepository, metricsCollector)
        val consumer = Consumer(KafkaTestTopics.oppgaveTopicName, kafkaConsumer, eventService)

        kafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()

            `Wait until all oppgave events have been received by target topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all oppgave events have been received by target topic`() {
        val targetConsumerProps = KafkaEmbed.consumerProps(testEnvironment, Eventtype.DOKNOTIFIKASJON, true)
        val targetKafkaConsumer = KafkaConsumer<String, Doknotifikasjon>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<String, Doknotifikasjon>()

        val targetConsumer = Consumer(KafkaTestTopics.doknotifikasjonTopicName, targetKafkaConsumer, capturingProcessor)

        var currentNumberOfRecords = 0

        targetConsumer.startPolling()

        while (currentNumberOfRecords < oppgaveEvents.size) {
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
