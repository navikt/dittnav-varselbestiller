package no.nav.personbruker.dittnav.varsel.bestiller.config

import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.Beskjed
import no.nav.personbruker.dittnav.varsel.bestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.BrukernotifikasjonProducer
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.Database
import no.nav.personbruker.dittnav.varsel.bestiller.done.Done
import no.nav.personbruker.dittnav.varsel.bestiller.done.DoneEventService
import no.nav.personbruker.dittnav.varsel.bestiller.health.HealthService
import no.nav.personbruker.dittnav.varsel.bestiller.innboks.Innboks
import no.nav.personbruker.dittnav.varsel.bestiller.innboks.InnboksEventService
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.buildEventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.Oppgave
import no.nav.personbruker.dittnav.varsel.bestiller.oppgave.OppgaveEventService

class ApplicationContext {

    var environment = Environment()
    val database: Database = PostgresDatabase(environment)

    val metricsProbe = buildEventMetricsProbe(environment, database)

    val beskjedProducer = BrukernotifikasjonProducer<Beskjed>()
    val beskjedEventProcessor = BeskjedEventService(beskjedProducer, metricsProbe)
    val beskjedKafkaProps = Kafka.consumerProps(environment, EventType.BESKJED)
    val beskjedConsumer = KafkaConsumerSetup.setupConsumerForTheBeskjedTopic(beskjedKafkaProps, beskjedEventProcessor)

    val oppgaveProducer = BrukernotifikasjonProducer<Oppgave>()
    val oppgaveEventProcessor = OppgaveEventService(oppgaveProducer, metricsProbe)
    val oppgaveKafkaProps = Kafka.consumerProps(environment, EventType.OPPGAVE)
    val oppgaveConsumer = KafkaConsumerSetup.setupConsumerForTheOppgaveTopic(oppgaveKafkaProps, oppgaveEventProcessor)

    val innboksProducer = BrukernotifikasjonProducer<Innboks>()
    val innboksEventProcessor = InnboksEventService(innboksProducer, metricsProbe)
    val innboksKafkaProps = Kafka.consumerProps(environment, EventType.INNBOKS)
    val innboksConsumer = KafkaConsumerSetup.setupConsumerForTheInnboksTopic(innboksKafkaProps, innboksEventProcessor)

    val doneProducer = BrukernotifikasjonProducer<Done>()
    val doneEventProcessor = DoneEventService(doneProducer, metricsProbe)
    val doneKafkaProps = Kafka.consumerProps(environment, EventType.DONE)
    val doneConsumer = KafkaConsumerSetup.setupConsumerForTheDoneTopic(doneKafkaProps, doneEventProcessor)

    val healthService = HealthService(this)
}
