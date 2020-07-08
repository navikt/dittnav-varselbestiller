package no.nav.personbruker.dittnav.varsel.bestiller.common

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import no.nav.brukernotifikasjon.schemas.Nokkel
import org.apache.kafka.clients.consumer.ConsumerRecords

class CapturingEventProcessor<T> : EventBatchProcessorService<T> {

    private val lock = ReentrantLock()
    private val eventBuffer = ArrayList<RecordKeyValueWrapper<T>>()

    override suspend fun processEvents(events: ConsumerRecords<Nokkel, T>) {
        val eventList = events.asWrapperList()

        lock.withLock {
            eventBuffer.addAll(eventList)
        }
    }

    fun getEvents() = lock.withLock {
        eventBuffer.map { it }
    }
}
