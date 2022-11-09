package no.nav.personbruker.dittnav.varselbestiller.config

import no.nav.personbruker.dittnav.common.util.config.IntEnvVar.getEnvVarAsInt
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar
import no.nav.personbruker.dittnav.varselbestiller.config.ConfigUtil.isCurrentlyRunningOnNais

data class Environment(
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
    val beskjedTopicName: String = getEnvVar("INTERN_BESKJED_TOPIC"),
    val oppgaveTopicName: String = getEnvVar("INTERN_OPPGAVE_TOPIC"),
    val innboksTopicName: String = getEnvVar("INTERN_INNBOKS_TOPIC"),
    val doneTopicName: String = getEnvVar("INTERN_DONE_TOPIC"),
    val doknotifikasjonTopicName: String = getEnvVar("DOKNOTIFIKASJON_TOPIC"),
    val doknotifikasjonStopTopicName: String = getEnvVar("DOKNOTIFIKASJON_STOP_TOPIC"),
    val aivenBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val aivenSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val securityConfig: SecurityConfig = SecurityConfig(isCurrentlyRunningOnNais()),
    val rapidTopic: String = getEnvVar("RAPID_TOPIC")
    ) {
    fun rapidConfig(): Map<String, String> = mapOf(
        "KAFKA_BROKERS" to aivenBrokers,
        "KAFKA_CONSUMER_GROUP_ID" to "dittnav-varselbestiller-v1",
        "KAFKA_RAPID_TOPIC" to rapidTopic,
        "KAFKA_KEYSTORE_PATH" to securityConfig.variables!!.aivenKeystorePath,
        "KAFKA_CREDSTORE_PASSWORD" to securityConfig.variables.aivenCredstorePassword,
        "KAFKA_TRUSTSTORE_PATH" to securityConfig.variables.aivenTruststorePath,
        "KAFKA_RESET_POLICY" to "earliest",
        "HTTP_PORT" to "8080"
    )
}

data class SecurityConfig(
    val enabled: Boolean,

    val variables: SecurityVars? = if (enabled) {
        SecurityVars()
    } else {
        null
    }
)

data class SecurityVars(
    val aivenTruststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    val aivenKeystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
    val aivenCredstorePassword: String = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
    val aivenSchemaRegistryUser: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
    val aivenSchemaRegistryPassword: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD")
)

fun getDbUrl(host: String, port: String, name: String): String {
    return if (host.endsWith(":$port")) {
        "jdbc:postgresql://${host}/$name"
    } else {
        "jdbc:postgresql://${host}:${port}/${name}"
    }
}
