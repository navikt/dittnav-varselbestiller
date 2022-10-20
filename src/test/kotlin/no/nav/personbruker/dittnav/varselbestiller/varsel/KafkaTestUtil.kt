package no.nav.personbruker.dittnav.varselbestiller.varsel

import org.apache.kafka.clients.producer.MockProducer

object KafkaTestUtil {

    fun <K, V> createMockProducer(): MockProducer<K, V> {
        return MockProducer(
            false,
            { _: String, _: K -> ByteArray(0) }, //Dummy serializers
            { _: String, _: V -> ByteArray(0) }
        )
    }
}
