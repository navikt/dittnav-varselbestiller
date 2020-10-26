package no.nav.personbruker.dittnav.varsel.bestiller.config

data class Environment(val bootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
                       val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMAREGISTRY_SERVERS"),
                       val username: String = getEnvVar("SERVICEUSER_USERNAME"),
                       val password: String = getEnvVar("SERVICEUSER_PASSWORD"),
                       val groupId: String = getEnvVar("GROUP_ID"),
                       val dbHost: String = getEnvVar("DB_HOST"),
                       val dbName: String = getEnvVar("DB_NAME"),
                       val dbReadOnlyUser: String = getEnvVar("DB_NAME") + "-readonly",
                       val dbUser: String = getEnvVar("DB_NAME") + "-user",
                       val dbUrl: String = "jdbc:postgresql://$dbHost/$dbName",
                       val dbMountPath: String = getEnvVar("DB_MOUNT_PATH"),
                       val clusterName: String = getEnvVar("NAIS_CLUSTER_NAME"),
                       val namespace: String = getEnvVar("NAIS_NAMESPACE"),
                       val sensuHost: String = getEnvVar("SENSU_HOST"),
                       val sensuPort: String = getEnvVar("SENSU_PORT"),
                       val applicationName: String = "dittnav-varselbestiller",
                       val sensuBatchingEnabled: Boolean = getEnvVar("SENSU_BATCHING_ENABLED", "true").toBoolean(),
                       val sensuBatchesPerSecond: Int = getEnvVar("SENSU_BATCHING_ENABLED", "3").toInt(),
)

fun getEnvVar(varName: String, default: String? = null): String {
    return System.getenv(varName)
            ?: default
            ?: throw IllegalArgumentException("Appen kan ikke starte uten at miljøvariabelen $varName er satt.")
}

fun isOtherEnvironmentThanProd() = System.getenv("NAIS_CLUSTER_NAME") != "prod-sbs"
