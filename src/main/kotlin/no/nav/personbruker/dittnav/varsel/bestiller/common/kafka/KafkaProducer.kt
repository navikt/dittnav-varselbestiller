package no.nav.personbruker.dittnav.varsel.bestiller.common.kafka

import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper

interface KafkaProducer<T> {
    fun sendEvents(events: List<RecordKeyValueWrapper<T>>)
}