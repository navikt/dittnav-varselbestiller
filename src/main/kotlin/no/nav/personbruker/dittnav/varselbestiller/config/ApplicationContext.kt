package no.nav.personbruker.dittnav.varselbestiller.config

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.varselbestiller.common.database.Database
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.done.DoneEventService
import no.nav.personbruker.dittnav.varselbestiller.health.HealthService
import no.nav.personbruker.dittnav.varselbestiller.oppgave.OppgaveEventService
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)
    val environment = Environment()
    val database: Database = PostgresDatabase(environment)

    private val doknotifikasjonProducer = initializeDoknotifikasjonProducer()
    private val doknotifikasjonStopProducer = initializeDoknotifikasjonStoppProducer()
    private val doknotifikasjonRepository = VarselbestillingRepository(database)

    private val beskjedKafkaProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
    private val beskjedEventService = BeskjedEventService(doknotifikasjonProducer, doknotifikasjonRepository)
    var beskjedConsumer = initializeBeskjedConsumer()

    private val oppgaveKafkaProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
    val oppgaveEventService = OppgaveEventService(doknotifikasjonProducer, doknotifikasjonRepository)
    var oppgaveConsumer = initializeOppgaveConsumer()

    private val doneKafkaProps = Kafka.consumerProps(environment, Eventtype.DONE)
    private val doneEventService = DoneEventService(doknotifikasjonStopProducer, doknotifikasjonRepository)
    var doneConsumer = initializeDoneConsumer()

    val healthService = HealthService(this)

    private fun initializeBeskjedConsumer(): Consumer<Nokkel, Beskjed> {
        return KafkaConsumerSetup.setupConsumerForTheBeskjedTopic(beskjedKafkaProps, beskjedEventService)
    }

    private fun initializeOppgaveConsumer(): Consumer<Nokkel, Oppgave> {
        return KafkaConsumerSetup.setupConsumerForTheOppgaveTopic(oppgaveKafkaProps, oppgaveEventService)
    }

    private fun initializeDoneConsumer(): Consumer<Nokkel, Done> {
        return KafkaConsumerSetup.setupConsumerForTheDoneTopic(doneKafkaProps, doneEventService)
    }

    private fun initializeDoknotifikasjonProducer(): DoknotifikasjonProducer {
        val producerProps = Kafka.producerProps(environment, Eventtype.DOKNOTIFIKASJON)
        val kafkaProducer = KafkaProducer<String, Doknotifikasjon>(producerProps)
        kafkaProducer.initTransactions()
        val kafkaProducerDoknotifikasjon = KafkaProducerWrapper(Kafka.doknotifikasjonTopicName, kafkaProducer)
        return DoknotifikasjonProducer(kafkaProducerDoknotifikasjon)
    }

    private fun initializeDoknotifikasjonStoppProducer(): DoknotifikasjonStoppProducer {
        val producerProps = Kafka.producerProps(environment, Eventtype.DOKNOTIFIKASJON_STOPP)
        val kafkaProducer = KafkaProducer<String, DoknotifikasjonStopp>(producerProps)
        kafkaProducer.initTransactions()
        val kafkaProducerDoknotifikasjonStopp = KafkaProducerWrapper(Kafka.doknotifikasjonStopTopicName, kafkaProducer)
        return DoknotifikasjonStoppProducer(kafkaProducerDoknotifikasjonStopp)
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
