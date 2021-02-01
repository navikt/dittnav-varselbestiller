package no.nav.personbruker.dittnav.varselbestiller

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecords

class CapturingEventProcessor<K, V> : EventBatchProcessorService<K, V> {

    private val lock = ReentrantLock()
    private val eventBuffer = ArrayList<RecordKeyValueWrapper<K, V>>()

    override suspend fun processEvents(events: ConsumerRecords<K, V>) {
        val eventList = events.asWrapperList()

        lock.withLock {
            eventBuffer.addAll(eventList)
        }
    }

    fun getEvents() = lock.withLock {
        eventBuffer.map { it }
    }
}
