package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

open class DoknotEventProducer<T>(
    private val topicName: String,
    private val kafkaProducer: Producer<String, T>
) {

    private val log = KotlinLogging.logger { }

    fun sendEvent(key:String, value: T) {
        kafkaProducer.send(ProducerRecord(topicName, key, value))
    }

    fun flushAndClose() {
        try {
            kafkaProducer.flush()
            kafkaProducer.close()
            log.info { "Produsent for kafka-eventer er flushet og lukket." }
        } catch (e: Exception) {
            log.warn { "Klarte ikke å flushe og lukke produsent. Det kan være eventer som ikke ble produsert." }
        }
    }
}
