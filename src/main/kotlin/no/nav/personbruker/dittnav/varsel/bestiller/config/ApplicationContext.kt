package no.nav.personbruker.dittnav.varsel.bestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.LogKafkaProducer
import no.nav.personbruker.dittnav.varsel.bestiller.done.DoneEventService
import no.nav.personbruker.dittnav.varsel.bestiller.health.HealthService
import no.nav.personbruker.dittnav.varsel.bestiller.innboks.InnboksEventService
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.buildEventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.OppgaveEventService
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)
    val environment = Environment()
    val database: Database = PostgresDatabase(environment)

    val eventMetricsProbe = buildEventMetricsProbe(environment, database)

    val beskjedKafkaProps = Kafka.consumerProps(environment, EventType.BESKJED)
    var beskjedConsumer = initializeBeskjedConsumer()

    val oppgaveKafkaProps = Kafka.consumerProps(environment, EventType.OPPGAVE)
    var oppgaveConsumer = initializeOppgaveConsumer()

    val doneKafkaProps = Kafka.consumerProps(environment, EventType.DONE)
    var doneConsumer = initializeDoneConsumer()

    val innboksKafkaProps = Kafka.consumerProps(environment, EventType.INNBOKS)
    var innboksConsumer = initializeInnboksConsumer()


    val healthService = HealthService(this)

    private fun initializeBeskjedConsumer(): Consumer<Beskjed> {
        val beskjedKafkaProducer = KafkaProducer<Nokkel, Beskjed>(Kafka.producerProps(environment, EventType.BESKJED_EKSTERN_VARSLING))
        beskjedKafkaProducer.initTransactions()
        val beskjedKafkaProducerWrapper: no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducer<Beskjed>

        if (ConfigUtil.isCurrentlyRunningOnNais()) {
            beskjedKafkaProducerWrapper = LogKafkaProducer<Beskjed>()
        } else {
            beskjedKafkaProducerWrapper = KafkaProducerWrapper(Kafka.beskjedVarselBestillerTopicName, beskjedKafkaProducer)
        }

        val beskjedEventService = BeskjedEventService(beskjedKafkaProducerWrapper, eventMetricsProbe)
        return KafkaConsumerSetup.setupConsumerForTheBeskjedTopic(beskjedKafkaProps, beskjedEventService)
    }

    private fun initializeOppgaveConsumer(): Consumer<Oppgave> {
        val oppgaveKafkaProducer = KafkaProducer<Nokkel, Oppgave>(Kafka.producerProps(environment, EventType.OPPGAVE))
        oppgaveKafkaProducer.initTransactions()
        val oppgaveKafkaProducerWrapper: no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducer<Oppgave>

        if (ConfigUtil.isCurrentlyRunningOnNais()) {
            oppgaveKafkaProducerWrapper = LogKafkaProducer<Oppgave>()
        } else {
            oppgaveKafkaProducerWrapper = KafkaProducerWrapper(Kafka.oppgaveVarselBestillerTopicName, oppgaveKafkaProducer)
        }

        val oppgaveEventService = OppgaveEventService(oppgaveKafkaProducerWrapper, eventMetricsProbe)
        return KafkaConsumerSetup.setupConsumerForTheOppgaveTopic(oppgaveKafkaProps, oppgaveEventService)
    }

    private fun initializeDoneConsumer(): Consumer<Done> {
        val doneKafkaProducer = KafkaProducer<Nokkel, Done>(Kafka.producerProps(environment, EventType.DONE))
        doneKafkaProducer.initTransactions()
        val doneKafkaProducerWrapper: no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducer<Done>

        if (ConfigUtil.isCurrentlyRunningOnNais()) {
            doneKafkaProducerWrapper = LogKafkaProducer<Done>()
        } else {
            doneKafkaProducerWrapper = KafkaProducerWrapper(Kafka.doneVarselBestillerTopicName, doneKafkaProducer)
        }

        val doneEventService = DoneEventService(doneKafkaProducerWrapper, eventMetricsProbe)
        return KafkaConsumerSetup.setupConsumerForTheDoneTopic(doneKafkaProps, doneEventService)
    }

    private fun initializeInnboksConsumer(): Consumer<Innboks> {
        val innboksKafkaProducer = KafkaProducer<Nokkel, Innboks>(Kafka.producerProps(environment, EventType.INNBOKS))
        innboksKafkaProducer.initTransactions()
        val innboksKafkaProducerWrapper: no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducer<Innboks>

        if (ConfigUtil.isCurrentlyRunningOnNais()) {
            innboksKafkaProducerWrapper = LogKafkaProducer<Innboks>()
        } else {
            innboksKafkaProducerWrapper = KafkaProducerWrapper(Kafka.innboksVarselBestillerTopicName, innboksKafkaProducer)
        }

        val innboksEventService = InnboksEventService(innboksKafkaProducerWrapper, eventMetricsProbe)
        return KafkaConsumerSetup.setupConsumerForTheInnboksTopic(innboksKafkaProps, innboksEventService)
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

        if (innboksConsumer.isCompleted()) {
            innboksConsumer = initializeInnboksConsumer()
            log.info("innboksConsumer har blitt reinstansiert.")
        } else {
            log.warn("innboksConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (doneConsumer.isCompleted()) {
            doneConsumer = initializeDoneConsumer()
            log.info("doneConsumer har blitt reinstansiert.")
        } else {
            log.warn("doneConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }
    }
}
