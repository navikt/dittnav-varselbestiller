package no.nav.personbruker.dittnav.varsel.bestiller.common

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

interface EventBatchProcessorService<K, V> {

    suspend fun processEvents(events: ConsumerRecords<K, V>)

    val ConsumerRecord<Nokkel, V>.systembruker : String get() = key().getSystembruker()

    fun ConsumerRecords<K, V>.asWrapperList() : List<RecordKeyValueWrapper<K, V>> = map { record ->
        RecordKeyValueWrapper(record.key(), record.value())
    }
}
