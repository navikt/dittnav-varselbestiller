package no.nav.personbruker.dittnav.varselbestiller.common.kafka


class Producer<K, V>(private val kafkaProducer: KafkaProducerWrapper<K, V>) {

    fun produceEvents(events: List<RecordKeyValueWrapper<K, V>>) {
        kafkaProducer.sendEventsTransactionally(events)
    }

    fun flushAndClose() {
        kafkaProducer.flushAndClose()
    }
}
