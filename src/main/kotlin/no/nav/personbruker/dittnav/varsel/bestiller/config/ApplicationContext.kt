package no.nav.personbruker.dittnav.varsel.bestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varsel.bestiller.done.DoneEventService
import no.nav.personbruker.dittnav.varsel.bestiller.health.HealthService
import no.nav.personbruker.dittnav.varsel.bestiller.innboks.InnboksEventService
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.buildEventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.OppgaveEventService
import org.apache.kafka.clients.producer.KafkaProducer

class ApplicationContext {

    var environment = Environment()
    val database: Database = PostgresDatabase(environment)

    val metricsProbe = buildEventMetricsProbe(environment, database)

    val beskjedKafkaProducer = KafkaProducer<Nokkel, Beskjed>(Kafka.producerProps(environment, EventType.BESKJED))
    val beskjedKafkaProducerWrapper = KafkaProducerWrapper(Kafka.beskjedVarselBestillerTopicName, beskjedKafkaProducer)
    val beskjedEventProcessor = BeskjedEventService(beskjedKafkaProducerWrapper, metricsProbe)
    val beskjedKafkaProps = Kafka.consumerProps(environment, EventType.BESKJED)
    val beskjedConsumer = KafkaConsumerSetup.setupConsumerForTheBeskjedTopic(beskjedKafkaProps, beskjedEventProcessor)

    val oppgaveKafkaProducer = KafkaProducer<Nokkel, Oppgave>(Kafka.producerProps(environment, EventType.OPPGAVE))
    val oppgaveKafkaProducerWrapper = KafkaProducerWrapper(Kafka.oppgaveVarselBestillerTopicName, oppgaveKafkaProducer)
    val oppgaveEventProcessor = OppgaveEventService(oppgaveKafkaProducerWrapper, metricsProbe)
    val oppgaveKafkaProps = Kafka.consumerProps(environment, EventType.OPPGAVE)
    val oppgaveConsumer = KafkaConsumerSetup.setupConsumerForTheOppgaveTopic(oppgaveKafkaProps, oppgaveEventProcessor)

    val innboksKafkaProducer = KafkaProducer<Nokkel, Innboks>(Kafka.producerProps(environment, EventType.INNBOKS))
    val innboksKafkaProducerWrapper = KafkaProducerWrapper(Kafka.innboksVarselBestillerTopicName, innboksKafkaProducer)
    val innboksEventProcessor = InnboksEventService(innboksKafkaProducerWrapper, metricsProbe)
    val innboksKafkaProps = Kafka.consumerProps(environment, EventType.INNBOKS)
    val innboksConsumer = KafkaConsumerSetup.setupConsumerForTheInnboksTopic(innboksKafkaProps, innboksEventProcessor)

    val doneKafkaProducer = KafkaProducer<Nokkel, Done>(Kafka.producerProps(environment, EventType.DONE))
    val doneKafkaProducerWrapper = KafkaProducerWrapper(Kafka.doneVarselBestillerTopicName, doneKafkaProducer)
    val doneEventProcessor = DoneEventService(doneKafkaProducerWrapper, metricsProbe)
    val doneKafkaProps = Kafka.consumerProps(environment, EventType.DONE)
    val doneConsumer = KafkaConsumerSetup.setupConsumerForTheDoneTopic(doneKafkaProps, doneEventProcessor)

    val healthService = HealthService(this)
}
