package no.nav.personbruker.dittnav.varselbestiller.common.kafka

import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception.UnretriableKafkaException
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.slf4j.LoggerFactory

class KafkaProducerWrapper<K, V>(
    private val topicName: String,
    private val kafkaProducer: KafkaProducer<K, V>
) {

    fun sendEventsTransactionally(events: List<RecordKeyValueWrapper<K, V>>) {
        try {
            kafkaProducer.beginTransaction()
            events.forEach { event ->
                sendSingleEvent(event)
            }
            kafkaProducer.commitTransaction()
        } catch (e: KafkaException) {
            kafkaProducer.abortTransaction()
            throw RetriableKafkaException("Et eller flere eventer feilet med en periodisk feil ved sending til kafka", e)
        } catch (e: Exception) {
            kafkaProducer.close()
            throw UnretriableKafkaException("Fant en uventet feil ved sending av eventer til kafka", e)
        }
    }

    private fun sendSingleEvent(event: RecordKeyValueWrapper<K, V>) {
        val producerRecord = ProducerRecord(topicName, event.key, event.value)
        kafkaProducer.send(producerRecord)
    }
}
