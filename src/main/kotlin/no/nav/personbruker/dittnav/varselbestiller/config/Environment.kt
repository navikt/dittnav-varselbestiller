package no.nav.personbruker.dittnav.varselbestiller.config

import no.nav.personbruker.dittnav.common.util.config.IntEnvVar.getEnvVarAsInt
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getOptionalEnvVar
import java.net.URL

data class Environment(val bootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
                       val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMAREGISTRY_SERVERS"),
                       val username: String = getEnvVar("SERVICEUSER_USERNAME"),
                       val password: String = getEnvVar("SERVICEUSER_PASSWORD"),
                       val groupId: String = getEnvVar("GROUP_ID"),
                       val dbHost: String = getEnvVar("DB_HOST"),
                       val dbPort: String = getEnvVar("DB_PORT"),
                       val dbName: String = getEnvVar("DB_DATABASE"),
                       val dbUser: String = getEnvVar("DB_USERNAME"),
                       val dbPassword: String = getEnvVar("DB_PASSWORD"),
                       val dbUrl: String = getDbUrl(dbHost, dbPort, dbName),
                       val clusterName: String = getEnvVar("NAIS_CLUSTER_NAME"),
                       val namespace: String = getEnvVar("NAIS_NAMESPACE"),
                       val influxdbHost: String = getEnvVar("INFLUXDB_HOST"),
                       val influxdbPort: Int = getEnvVarAsInt("INFLUXDB_PORT"),
                       val influxdbName: String = getEnvVar("INFLUXDB_DATABASE_NAME"),
                       val influxdbUser: String = getEnvVar("INFLUXDB_USER"),
                       val influxdbPassword: String = getEnvVar("INFLUXDB_PASSWORD"),
                       val influxdbRetentionPolicy: String = getEnvVar("INFLUXDB_RETENTION_POLICY"),
                       val applicationName: String = "dittnav-varselbestiller",
                       val eventHandlerURL: URL = URL(getEnvVar("EVENT_HANDLER_URL").trimEnd('/')),
                       val beskjedTopicName: String = getEnvVar("INTERN_BESKJED_TOPIC"),
                       val oppgaveTopicName: String = getEnvVar("INTERN_OPPGAVE_TOPIC"),
                       val doneTopicName: String = getEnvVar("INTERN_DONE_TOPIC"),
                       val doknotifikasjonTopicName: String = getEnvVar("DOKNOTIFIKASJON_TOPIC"),
                       val doknotifikasjonStopTopicName: String = getEnvVar("DOKNOTIFIKASJON_STOP_TOPIC")
)

fun isOtherEnvironmentThanProd() = System.getenv("NAIS_CLUSTER_NAME") != "prod-sbs"

fun shouldPollBeskjedToDoknotifikasjon() = getOptionalEnvVar("POLL_BESKJED_TO_DOKNOTIFIKASJON", "false").toBoolean()

fun shouldPollOppgaveToDoknotifikasjon() = getOptionalEnvVar("POLL_OPPGAVE_TO_DOKNOTIFIKASJON", "false").toBoolean()

fun shouldPollDoneToDoknotifikasjonStopp() = getOptionalEnvVar("POLL_DONE_TO_DOKNOTIFIKASJON_STOPP", "false").toBoolean()

fun getDbUrl(host: String, port: String, name: String): String {
    return if (host.endsWith(":$port")) {
        "jdbc:postgresql://${host}/$name"
    } else {
        "jdbc:postgresql://${host}:${port}/${name}"
    }
}
