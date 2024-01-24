package no.nav.tms.ekstern.varselbestiller.config

import no.nav.tms.common.util.config.IntEnvVar.getEnvVarAsInt
import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val groupId: String = getEnvVar("GROUP_ID"),
    val doknotifikasjonTopicName: String = getEnvVar("DOKNOTIFIKASJON_TOPIC"),
    val doknotifikasjonStopTopicName: String = getEnvVar("DOKNOTIFIKASJON_STOP_TOPIC"),
    val kafkaBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val kafkaSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val securityVars: SecurityVars = SecurityVars(),
    val rapidTopic: String = getEnvVar("VARSEL_TOPIC"),
    ) {
    fun rapidConfig(): Map<String, String> = mapOf(
        "KAFKA_BROKERS" to kafkaBrokers,
        "KAFKA_CONSUMER_GROUP_ID" to groupId,
        "KAFKA_RAPID_TOPIC" to rapidTopic,
        "KAFKA_KEYSTORE_PATH" to securityVars.kafkaKeystorePath,
        "KAFKA_CREDSTORE_PASSWORD" to securityVars.kafkaCredstorePassword,
        "KAFKA_TRUSTSTORE_PATH" to securityVars.kafkaTruststorePath,
        "KAFKA_RESET_POLICY" to "earliest",
        "HTTP_PORT" to "8080"
    )
}

data class SecurityVars(
    val kafkaTruststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    val kafkaKeystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
    val kafkaCredstorePassword: String = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
    val kafkaSchemaRegistryUser: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
    val kafkaSchemaRegistryPassword: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD")
)
