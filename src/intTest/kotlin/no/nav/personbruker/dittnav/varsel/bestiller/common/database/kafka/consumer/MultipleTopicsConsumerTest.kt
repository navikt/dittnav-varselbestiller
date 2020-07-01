package no.nav.personbruker.dittnav.varsel.bestiller.common.database.kafka.consumer

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.varsel.bestiller.common.CapturingEventProcessor
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.kafka.util.KafkaTestUtil
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varsel.bestiller.config.Environment
import no.nav.personbruker.dittnav.varsel.bestiller.config.EventType
import no.nav.personbruker.dittnav.varsel.bestiller.config.Kafka
import no.nav.personbruker.dittnav.varsel.bestiller.config.KafkaConsumerSetup.setupConsumerForTheBeskjedTopic
import no.nav.personbruker.dittnav.varsel.bestiller.config.KafkaConsumerSetup.setupConsumerForTheInnboksTopic
import no.nav.personbruker.dittnav.varsel.bestiller.config.KafkaConsumerSetup.setupConsumerForTheOppgaveTopic
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.innboks.AvroInnboksObjectMother
import no.nav.personbruker.dittnav.varsel.bestiller.nokkel.createNokkel
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.AvroOppgaveObjectMother
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class MultipleTopicsConsumerTest {

    private val topics = listOf(Kafka.beskjedTopicName, Kafka.oppgaveTopicName, Kafka.innboksTopicName)
    private val embeddedEnv = KafkaTestUtil.createDefaultKafkaEmbeddedInstance(topics)
    private val testEnvironment = KafkaTestUtil.createEnvironmentForEmbeddedKafka(embeddedEnv)
    private val adminClient = embeddedEnv.adminClient

    private val beskjedEventProcessor = CapturingEventProcessor<Beskjed>()
    private val oppgaveEventProcessor = CapturingEventProcessor<Oppgave>()
    private val innboksEventProcessor = CapturingEventProcessor<Innboks>()

    private val beskjedEvents = (1..10).map { createNokkel(it) to AvroBeskjedObjectMother.createBeskjed(it) }.toMap()
    private val oppgaveEvents = (1..11).map { createNokkel(it) to AvroOppgaveObjectMother.createOppgave(it) }.toMap()
    private val innboksEvents = (1..12).map { createNokkel(it) to AvroInnboksObjectMother.createInnboks(it) }.toMap()

    init {
        embeddedEnv.start()
    }

    @AfterAll
    fun tearDown() {
        adminClient?.close()
        embeddedEnv.tearDown()
    }

    @Test
    fun `Kafka instansen i minnet har blitt startet`() {
        embeddedEnv.serverPark.status `should equal` KafkaEnvironment.ServerParkStatus.Started
    }

    fun `Produserer testeventer for alle topics`() {
        runBlocking {
            val producedAllBeskjed = KafkaTestUtil.produceEvents(testEnvironment, Kafka.beskjedTopicName, beskjedEvents)
            val producedAllOppgave = KafkaTestUtil.produceEvents(testEnvironment, Kafka.oppgaveTopicName, oppgaveEvents)
            val producedAllInnboks = KafkaTestUtil.produceEvents(testEnvironment, Kafka.innboksTopicName, innboksEvents)
            producedAllBeskjed `should be equal to` true
            producedAllOppgave `should be equal to` true
            producedAllInnboks `should be equal to` true
        }
    }

    @Test
    fun `Skal kunne konsumere fra flere topics i parallell`() {
        `Produserer testeventer for alle topics`()

        val beskjedConsumer = createInfoConsumer(testEnvironment, beskjedEventProcessor)
        val oppgaveConsumer = createOppgaveConsumer(testEnvironment, oppgaveEventProcessor)
        val innboksConsumer = createInnboksConsumer(testEnvironment, innboksEventProcessor)

        runBlocking {
            beskjedConsumer.startPolling()
            oppgaveConsumer.startPolling()
            innboksConsumer.startPolling()

            `Vent til alle eventer har blitt konsumert`()

            beskjedConsumer.stopPolling()
            oppgaveConsumer.stopPolling()
            innboksConsumer.stopPolling()

            beskjedEvents.size `should be equal to` beskjedEventProcessor.eventCounter
            oppgaveEvents.size `should be equal to` oppgaveEventProcessor.eventCounter
            innboksEvents.size `should be equal to`  innboksEventProcessor.eventCounter
        }
    }

    private suspend fun `Vent til alle eventer har blitt konsumert`() {
        while (`Har alle eventer blitt lest`(beskjedEventProcessor, beskjedEvents,
                        oppgaveEventProcessor, oppgaveEvents,
                        innboksEventProcessor, innboksEvents)) {
            delay(100)
        }
    }

    private fun `Har alle eventer blitt lest`(beskjed: CapturingEventProcessor<Beskjed>, BeskjedEvents: Map<Nokkel, Beskjed>,
                                              oppgave: CapturingEventProcessor<Oppgave>, oppgaveEvents: Map<Nokkel, Oppgave>,
                                              innboks: CapturingEventProcessor<Innboks>, innboksEvents: Map<Nokkel, Innboks>): Boolean {
        return beskjed.eventCounter < BeskjedEvents.size &&
                oppgave.eventCounter < oppgaveEvents.size &&
                innboks.eventCounter < innboksEvents.size
    }

    private fun createInfoConsumer(env: Environment, beskjedEventProcessor: CapturingEventProcessor<Beskjed>): Consumer<Beskjed> {
        val kafkaProps = Kafka.consumerProps(env, EventType.BESKJED, true)
        return setupConsumerForTheBeskjedTopic(kafkaProps, beskjedEventProcessor)
    }

    private fun createOppgaveConsumer(env: Environment, oppgaveEventProcessor: CapturingEventProcessor<Oppgave>): Consumer<Oppgave> {
        val kafkaProps = Kafka.consumerProps(env, EventType.OPPGAVE, true)
        return setupConsumerForTheOppgaveTopic(kafkaProps, oppgaveEventProcessor)
    }

    private fun createInnboksConsumer(env: Environment, innboksEventProcessor: CapturingEventProcessor<Innboks>): Consumer<Innboks> {
        val kafkaProps = Kafka.consumerProps(env, EventType.INNBOKS, true)
        return setupConsumerForTheInnboksTopic(kafkaProps, innboksEventProcessor)
    }

}
