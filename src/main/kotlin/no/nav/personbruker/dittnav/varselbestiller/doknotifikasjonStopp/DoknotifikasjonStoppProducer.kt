package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class DoknotifikasjonStoppProducer(
        private val producer: KafkaProducerWrapper<String, DoknotifikasjonStopp>,
        private val varselbestillingRepository: VarselbestillingRepository
) {
    suspend fun sendDoknotifikasjonStoppAndPersistCancellation(doknotStop: DoknotifikasjonStopp) {
        val event = RecordKeyValueWrapper(doknotStop.getBestillingsId(), doknotStop)

        try {
            producer.sendEventsAndLeaveTransactionOpen(event)
            varselbestillingRepository.cancelVarselbestilling(event.key)
            producer.commitCurrentTransaction()
        } catch (e: Exception) {
            producer.abortCurrentTransaction()
            throw e
        }
    }

    fun flushAndClose() {
        producer.flushAndClose()
    }
}
