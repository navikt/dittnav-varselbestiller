package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper

class DoknotifikasjonProducer(private val doknotifikasjonKafkaProducer: KafkaProducerWrapper<String, Doknotifikasjon>) {

    fun produceDoknotifikasjon(events: List<RecordKeyValueWrapper<String, Doknotifikasjon>>) {
        doknotifikasjonKafkaProducer.sendEventsTransactionally(events)
    }

    fun flushAndClose() {
        doknotifikasjonKafkaProducer.flushAndClose()
    }
}
