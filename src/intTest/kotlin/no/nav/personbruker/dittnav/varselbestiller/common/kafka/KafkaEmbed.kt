package no.nav.personbruker.dittnav.varselbestiller.common.kafka

import no.nav.personbruker.dittnav.varselbestiller.config.Environment
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.config.Kafka
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

object KafkaEmbed {

    fun consumerProps(env: Environment, eventtypeToConsume: Eventtype): Properties {
        return Kafka.consumerProps(env, eventtypeToConsume).apply {
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
    }
}
