package no.nav.personbruker.dittnav.varsel.bestiller.common

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

interface EventBatchProcessorService<T> {

    suspend fun processEvents(events: ConsumerRecords<Nokkel, T>)

    val ConsumerRecord<Nokkel, T>.systembruker : String get() = key().getSystembruker()

    fun ConsumerRecords<Nokkel, T>.asWrapperList() : List<RecordKeyValueWrapper<Nokkel, T>> = map { record ->
        RecordKeyValueWrapper(record.key(), record.value())
    }
}
