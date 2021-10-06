package no.nav.personbruker.dittnav.varselbestiller.common.kafka

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.varselbestiller.config.Environment
import org.apache.avro.generic.GenericRecord
import java.net.URL
import java.util.*

object KafkaTestUtil {

    val username = "srvkafkaclient"
    val password = "kafkaclient"

    fun createDefaultKafkaEmbeddedInstance(topics: List<String>): KafkaEnvironment {
        return KafkaEnvironment(
                topicNames = topics,
                withSecurity = true,
                withSchemaRegistry = true,
                users = listOf(JAASCredential(username, password)),
                brokerConfigOverrides = Properties().apply {
                    put("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                    put("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                }
        )
    }

    fun createEnvironmentForEmbeddedKafka(embeddedEnv: KafkaEnvironment): Environment {
        return Environment(
                bootstrapServers = embeddedEnv.brokersURL.substringAfterLast("/"),
                schemaRegistryUrl = embeddedEnv.schemaRegistry!!.url,
                username = username,
                password = password,
                groupId = "groupId-for-tests",
                dbHost = "dbHostIkkeIBrukHer",
                dbPort = "dbPortIkkeIBrukHer",
                dbName = "dbNameIkkeIBrukHer",
                dbUrl = "dbUrlIkkeIBrukHer",
                dbUser = "dbUserIkkeIBrukHer",
                dbPassword = "dbPWIkkeIBrukHer",
                clusterName = "clusterNameIkkeIBrukHer",
                namespace = "namespaceIkkeIBrukHer",
                influxdbHost = "",
                influxdbPort = 0,
                influxdbName = "",
                influxdbUser = "",
                influxdbPassword = "",
                influxdbRetentionPolicy = "",
                eventHandlerURL = URL("http://event-handler"),
                beskjedTopicName = KafkaTestTopics.beskjedTopicName,
                oppgaveTopicName = KafkaTestTopics.oppgaveTopicName,
                doneTopicName = KafkaTestTopics.doneTopicName,
                doknotifikasjonTopicName = KafkaTestTopics.doknotifikasjonTopicName,
                doknotifikasjonStopTopicName = KafkaTestTopics.doknotifikasjonStopTopicName
        )
    }

    suspend fun produceEvents(env: Environment, topicName: String, events: Map<NokkelIntern, GenericRecord>): Boolean {
        return KafkaProducerUtil.kafkaAvroProduce(
                env.bootstrapServers,
                env.schemaRegistryUrl,
                topicName,
                env.username,
                env.password,
                events)
    }

}
