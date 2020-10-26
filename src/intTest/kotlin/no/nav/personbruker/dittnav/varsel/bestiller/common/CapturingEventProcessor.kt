package no.nav.personbruker.dittnav.varsel.bestiller.common

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
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
