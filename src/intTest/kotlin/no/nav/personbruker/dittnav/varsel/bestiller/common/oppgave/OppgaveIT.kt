package no.nav.personbruker.dittnav.varsel.bestiller.common.oppgave

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.common.KafkaEnvironment
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.masking.ProducerNameScrubber
import no.nav.personbruker.dittnav.common.metrics.masking.PublicAliasResolver
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.CapturingEventProcessor
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.H2Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.util.KafkaTestUtil
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType
import no.nav.personbruker.dittnav.varsel.bestiller.config.Kafka
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.db.getProdusentnavn
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkelWithEventId
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.AvroOppgaveObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.OppgaveEventService
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OppgaveIT {

    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(listOf(Kafka.oppgaveTopicName, Kafka.doknotifikasjonTopicName))
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)

    private val oppgaveEvents = (1..10).map { createNokkelWithEventId(it) to AvroOppgaveObjectMother.createOppgave(it) }.toMap()

    private val capturedDoknotifikasjonRecords = ArrayList<RecordKeyValueWrapper<String, Doknotifikasjon>>()

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
    fun `Should read Oppgave-events and send to varsel-bestiller-topic`() {
        runBlocking {
            KafkaTestUtil.produceEvents(testEnvironment, Kafka.oppgaveTopicName, oppgaveEvents)
        } shouldBeEqualTo true

        `Read all Oppgave-events from our topic and verify that they have been sent to varsel-bestiller-topic`()

        oppgaveEvents.all {
            capturedDoknotifikasjonRecords.contains(RecordKeyValueWrapper(it.key, it.value))
        }
    }

    fun `Read all Oppgave-events from our topic and verify that they have been sent to varsel-bestiller-topic`() {
        val consumerProps = Kafka.consumerProps(testEnvironment, EventType.OPPGAVE, true)
        val kafkaConsumer = KafkaConsumer<Nokkel, Oppgave>(consumerProps)

        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doknotifikasjonTopicName, KafkaProducer<String, Doknotifikasjon>(Kafka.producerProps(testEnvironment, EventType.DOKNOTIFIKASJON)))
        val doknotifikasjonProducer = DoknotifikasjonProducer(kafkaProducerWrapper)

        val eventService = OppgaveEventService(doknotifikasjonProducer, metricsProbe)
        val consumer = Consumer(Kafka.oppgaveTopicName, kafkaConsumer, eventService)

        runBlocking {
            consumer.startPolling()

            `Wait until all oppgave events have been received by target topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all oppgave events have been received by target topic`() {
        val targetConsumerProps = Kafka.consumerProps(testEnvironment, EventType.OPPGAVE, true)
        val targetKafkaConsumer = KafkaConsumer<Nokkel, Oppgave>(targetConsumerProps)
        val capturingProcessor = CapturingEventProcessor<Oppgave>()

        val targetConsumer = Consumer(Kafka.doknotifikasjonTopicName, targetKafkaConsumer, capturingProcessor)

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
