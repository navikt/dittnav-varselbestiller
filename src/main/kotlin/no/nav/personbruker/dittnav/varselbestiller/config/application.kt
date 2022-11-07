package no.nav.personbruker.dittnav.varselbestiller.config

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxConfig
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxMetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.common.database.Database
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.varsel.DoneSink
import no.nav.personbruker.dittnav.varselbestiller.varsel.RapidMetricsProbe
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselSink
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.producer.KafkaProducer
import java.util.concurrent.TimeUnit

fun main() {
    val environment = Environment()

    val database: Database = PostgresDatabase(environment)
    val varselbestillingRepository = VarselbestillingRepository(database)

    val doknotifikasjonProducer = DoknotifikasjonProducer(
        producer = KafkaProducerWrapper(
            topicName = environment.doknotifikasjonTopicName,
            kafkaProducer = KafkaProducer<String, Doknotifikasjon>(
                Kafka.producerProps(environment, Eventtype.VARSEL)
            ).apply { initTransactions() }
        ),
        varselbestillingRepository = varselbestillingRepository
    )
    val doknotifikasjonStoppProducer = DoknotifikasjonStoppProducer(
        producer = KafkaProducerWrapper(
            topicName = environment.doknotifikasjonStopTopicName,
            kafkaProducer = KafkaProducer<String, DoknotifikasjonStopp>(
                Kafka.producerProps(environment, Eventtype.DOKNOTIFIKASJON_STOPP)
            ).apply { initTransactions() }
        ),
        varselbestillingRepository = varselbestillingRepository
    )
    val rapidMetricsProbe = RapidMetricsProbe(resolveMetricsReporter(environment))

    startRapid(
        environment = environment,
        varselbestillingRepository = varselbestillingRepository,
        doknotifikasjonProducer = doknotifikasjonProducer,
        doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
        rapidMetricsProbe = rapidMetricsProbe
    )
}

private fun startRapid(
    environment: Environment,
    varselbestillingRepository: VarselbestillingRepository,
    doknotifikasjonProducer: DoknotifikasjonProducer,
    doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
    rapidMetricsProbe: RapidMetricsProbe
) {
    RapidApplication.create(environment.rapidConfig()).apply {
        VarselSink(
            rapidsConnection = this,
            doknotifikasjonProducer = doknotifikasjonProducer,
            varselbestillingRepository = varselbestillingRepository,
            rapidMetricsProbe = rapidMetricsProbe,
            writeToDb = true
        )
        DoneSink(
            rapidsConnection = this,
            doknotifikasjonStoppProducer = doknotifikasjonStoppProducer,
            varselbestillingRepository = varselbestillingRepository,
            rapidMetricsProbe = rapidMetricsProbe,
            writeToDb = true
        )
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(environment)
            }
        })
    }.start()
}

fun resolveMetricsReporter(environment: Environment): MetricsReporter {
    return if (environment.influxdbHost == "" || environment.influxdbHost == "stub") {
        StubMetricsReporter()
    } else {
        val influxConfig = InfluxConfig(
            applicationName = environment.applicationName,
            hostName = environment.influxdbHost,
            hostPort = environment.influxdbPort,
            databaseName = environment.influxdbName,
            retentionPolicyName = environment.influxdbRetentionPolicy,
            clusterName = environment.clusterName,
            namespace = environment.namespace,
            userName = environment.influxdbUser,
            password = environment.influxdbPassword,
            timePrecision = TimeUnit.NANOSECONDS
        )
        InfluxMetricsReporter(influxConfig)
    }
}