package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper

class DoknotifikasjonProducer(private val doknotifikasjonKafkaProducer: KafkaProducerWrapper<String, Doknotifikasjon>) {

    fun produceDoknotifikasjon(events: List<RecordKeyValueWrapper<String, Doknotifikasjon>>) {
        doknotifikasjonKafkaProducer.sendEventsTransactionally(events)
    }
}
