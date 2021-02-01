package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper

class DoknotifikasjonStoppProducer(private val doknotifikasjonStoppKafkaProducer: KafkaProducerWrapper<String, DoknotifikasjonStopp>) {

    fun produceDoknotifikasjonStop(events: List<RecordKeyValueWrapper<String, DoknotifikasjonStopp>>) {
        doknotifikasjonStoppKafkaProducer.sendEventsTransactionally(events)
    }
}
