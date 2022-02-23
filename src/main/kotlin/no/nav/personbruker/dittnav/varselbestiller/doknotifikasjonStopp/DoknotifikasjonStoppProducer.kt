package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class DoknotifikasjonStoppProducer(
        private val producer: KafkaProducerWrapper<String, DoknotifikasjonStopp>,
        private val varselbestillingRepository: VarselbestillingRepository
) {
    suspend fun sendEventsAndPersistCancellation(successfullyValidatedEvents: Map<String, DoknotifikasjonStopp>) {

        val events = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
        val keys = events.map { it.key }

        try {
            //producer.sendEventsAndLeaveTransactionOpen(events)
            varselbestillingRepository.cancelVarselbestilling(keys)
            //producer.commitCurrentTransaction()
        } catch (e: Exception) {
            producer.abortCurrentTransaction()
            throw e
        }
    }

    fun flushAndClose() {
        producer.flushAndClose()
    }
}
