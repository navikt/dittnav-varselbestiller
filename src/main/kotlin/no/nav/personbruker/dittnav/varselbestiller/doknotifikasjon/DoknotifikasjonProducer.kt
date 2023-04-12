package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import mu.KotlinLogging
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class DoknotifikasjonProducer(
    private val producer: KafkaProducerWrapper<String, Doknotifikasjon>,
    private val varselbestillingRepository: VarselbestillingRepository
) {

    private val log = KotlinLogging.logger { }
    private val secureLog = KotlinLogging.logger("secureLog")
    suspend fun sendAndPersistBestilling(varselbestilling: Varselbestilling, doknotifikasjon: Doknotifikasjon) {
        val event = RecordKeyValueWrapper(doknotifikasjon.getBestillingsId(), doknotifikasjon)

        try {
            log.info { "Sender bestilling av eksternt varsel for ${varselbestilling.eventId}" }
            producer.sendEventsAndLeaveTransactionOpen(event)
            varselbestillingRepository.persistVarselbestilling(varselbestilling)
            producer.commitCurrentTransaction()
        } catch (e: Exception) {
            log.info { "Feil i eksternvarsel-bestilling for $varselbestilling" }
            secureLog.error { "Feil i eksternvarsel-bestilling for $varselbestilling fra ${varselbestilling.appnavn}: \n ${e.stackTraceToString()}" }
            producer.abortCurrentTransaction()
            throw e
        }
    }

    fun flushAndClose() {
        producer.flushAndClose()
    }
}
