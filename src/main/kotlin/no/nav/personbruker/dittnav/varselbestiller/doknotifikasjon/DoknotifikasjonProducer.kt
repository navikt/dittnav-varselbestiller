package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class DoknotifikasjonProducer(
        private val producer: KafkaProducerWrapper<String, Doknotifikasjon>,
        private val varselbestillingRepository: VarselbestillingRepository
) {

    suspend fun sendAndPersistEvents(
            successfullyValidatedEvents: Map<String, Doknotifikasjon>,
            varselbestillinger: List<Varselbestilling>
    ): ListPersistActionResult<Varselbestilling> {
        val events = successfullyValidatedEvents.map { RecordKeyValueWrapper(it.key, it.value) }
        return try {
            producer.sendEventsAndLeaveTransactionOpen(events)
            val result = varselbestillingRepository.persistInOneBatch(varselbestillinger)
            producer.commitCurrentTransaction()
            result
        } catch (e: Exception) {
            producer.abortCurrentTransaction()
            throw e
        }
    }

    fun flushAndClose() {
        producer.flushAndClose()
    }
}
