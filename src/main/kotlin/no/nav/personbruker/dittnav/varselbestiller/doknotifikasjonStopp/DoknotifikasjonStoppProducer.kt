package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository

class DoknotifikasjonStoppProducer(
    private val producer: KafkaProducerWrapper<String, DoknotifikasjonStopp>,
    private val varselbestillingRepository: VarselbestillingRepository
) {
    private val log = KotlinLogging.logger { }
    private val secureLog = KotlinLogging.logger("secureLog")


    fun sendDoknotifikasjonStoppAndPersistCancellation(doknotStop: DoknotifikasjonStopp) {
        val event = RecordKeyValueWrapper(doknotStop.getBestillingsId(), doknotStop)

        try {
            log.info { "Sender avbestilling av eksternt varsel" }
            producer.sendEventsAndLeaveTransactionOpen(event)
            varselbestillingRepository.cancelVarselbestilling(event.key)
            producer.commitCurrentTransaction()
        } catch (e: Exception) {
            log.warn { "Feil i avbestilling av ekstern varsel" }
            secureLog.warn(e) { "Feil i avbestilling av ekstern varsel" }

            producer.abortCurrentTransaction()
            throw e
        }
    }

    fun flushAndClose() {
        producer.flushAndClose()
    }
}
