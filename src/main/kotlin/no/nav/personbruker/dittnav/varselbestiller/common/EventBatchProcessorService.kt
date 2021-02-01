package no.nav.personbruker.dittnav.varselbestiller.common

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

interface EventBatchProcessorService<K, V> {

    suspend fun processEvents(events: ConsumerRecords<K, V>)

    val ConsumerRecord<Nokkel, V>.systembruker : String? get() = key().getSystembruker()

    val ConsumerRecord<Nokkel, V>.eventId : String? get() = key().getEventId()

    fun ConsumerRecords<K, V>.asWrapperList() : List<RecordKeyValueWrapper<K, V>> = map { record ->
        RecordKeyValueWrapper(record.key(), record.value())
    }
}
