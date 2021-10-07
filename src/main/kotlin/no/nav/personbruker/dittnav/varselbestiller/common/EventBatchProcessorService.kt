package no.nav.personbruker.dittnav.varselbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

interface EventBatchProcessorService<K, V> {

    suspend fun processEvents(events: ConsumerRecords<K, V>)

    val ConsumerRecord<NokkelIntern, V>.appnavn: String get() = key().getAppnavn()

    val ConsumerRecord<NokkelIntern, V>.namespace: String get() = key().getNamespace()

    val ConsumerRecord<NokkelIntern, V>.eventId : String? get() = key().getEventId()

    fun ConsumerRecords<K, V>.asWrapperList() : List<RecordKeyValueWrapper<K, V>> = map { record ->
        RecordKeyValueWrapper(record.key(), record.value())
    }
}
