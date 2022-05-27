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

    suspend fun sendAndPersistBestillingBatch(
            varselbestillingList: List<Varselbestilling>,
            doknotifikasjonList: List<Doknotifikasjon>,
    ) {

        val kafkaEvents = doknotifikasjonList.map { doknotifikasjon ->
            RecordKeyValueWrapper(doknotifikasjon.getBestillingsId(), doknotifikasjon)
        }

        try {
            producer.sendEventsAndLeaveTransactionOpen(kafkaEvents)
            varselbestillingRepository.persistInOneBatch(varselbestillingList)
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
