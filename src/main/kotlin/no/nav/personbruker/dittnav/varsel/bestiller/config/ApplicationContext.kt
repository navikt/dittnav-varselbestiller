package no.nav.personbruker.dittnav.varsel.bestiller.config

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.doknotifikasjon.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varsel.bestiller.done.DoneEventService
import no.nav.personbruker.dittnav.varsel.bestiller.health.HealthService
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.buildEventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.OppgaveEventService
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)
    private val environment = Environment()
    val database: Database = PostgresDatabase(environment)

    val eventMetricsProbe = buildEventMetricsProbe(environment, database)

    private val kafkaProducerDoknotifikasjon = KafkaProducerWrapper(Kafka.doknotifikasjonTopicName, KafkaProducer<String, Doknotifikasjon>(Kafka.producerProps(environment, EventType.DOKNOTIFIKASJON)))
    private val kafkaProducerDoknotifikasjonStop = KafkaProducerWrapper(Kafka.doknotifikasjonStopTopicName, KafkaProducer<String, DoknotifikasjonStopp>(Kafka.producerProps(environment, EventType.DOKNOTIFIKASJON_STOPP)))
    private val doknotifikasjonProducer = DoknotifikasjonProducer(kafkaProducerDoknotifikasjon)
    private val doknotifikasjonStopProducer = DoknotifikasjonStoppProducer(kafkaProducerDoknotifikasjonStop)

    private val beskjedKafkaProps = Kafka.consumerProps(environment, EventType.BESKJED)
    private val beskjedEventService = BeskjedEventService(doknotifikasjonProducer, eventMetricsProbe)
    var beskjedConsumer = initializeBeskjedConsumer()

    private val oppgaveKafkaProps = Kafka.consumerProps(environment, EventType.OPPGAVE)
    val oppgaveEventService = OppgaveEventService(doknotifikasjonProducer, eventMetricsProbe)
    var oppgaveConsumer = initializeOppgaveConsumer()

    private val doneKafkaProps = Kafka.consumerProps(environment, EventType.DONE)
    private val doneEventService = DoneEventService(doknotifikasjonStopProducer, eventMetricsProbe)
    var doneConsumer = initializeDoneConsumer()

    val healthService = HealthService(this)

    private fun initializeBeskjedConsumer(): Consumer<Beskjed> {
        return KafkaConsumerSetup.setupConsumerForTheBeskjedTopic(beskjedKafkaProps, beskjedEventService)
    }

    private fun initializeOppgaveConsumer(): Consumer<Oppgave> {
        return KafkaConsumerSetup.setupConsumerForTheOppgaveTopic(oppgaveKafkaProps, oppgaveEventService)
    }

    private fun initializeDoneConsumer(): Consumer<Done> {
        return KafkaConsumerSetup.setupConsumerForTheDoneTopic(doneKafkaProps, doneEventService)
    }

    fun reinitializeConsumers() {
        if (beskjedConsumer.isCompleted()) {
            beskjedConsumer = initializeBeskjedConsumer()
            log.info("beskjedConsumer har blitt reinstansiert.")
        } else {
            log.warn("beskjedConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (oppgaveConsumer.isCompleted()) {
            oppgaveConsumer = initializeOppgaveConsumer()
            log.info("oppgaveConsumer har blitt reinstansiert.")
        } else {
            log.warn("oppgaveConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (doneConsumer.isCompleted()) {
            doneConsumer = initializeDoneConsumer()
            log.info("doneConsumer har blitt reinstansiert.")
        } else {
            log.warn("doneConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }
    }
}
