package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper

class DoknotifikasjonStoppProducer(private val doknotifikasjonKafkaProducer: KafkaProducerWrapper<String, DoknotifikasjonStopp>) {

    fun produceDoknotifikasjonStop(events: List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>) {
        doknotifikasjonKafkaProducer.sendEventsTransactionally(events)
    }
}
