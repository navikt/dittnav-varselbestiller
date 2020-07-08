package no.nav.personbruker.dittnav.varsel.bestiller.common.kafka

import no.nav.personbruker.dittnav.varsel.bestiller.common.RecordKeyValueWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

//Så lenge varsel-bestiller-topic ikke er opprettet logger vi bare eventet
class LogKafkaProducer<T>(
) : no.nav.personbruker.dittnav.varsel.bestiller.common.kafka.KafkaProducer<T> {

    private val log: Logger = LoggerFactory.getLogger(KafkaProducerWrapper::class.java)

    override fun sendEvents(events: List<RecordKeyValueWrapper<T>>) {
        try {
            events.forEach { event ->
                logEvent(event)
            }
        } catch (e: Exception) {
            throw Exception("Fant en uventet feil ved logging av event.", e)
        }
    }

    private fun logEvent(event: RecordKeyValueWrapper<T>) {
        log.info("EventId: ${event.key.getEventId()}, " +
                "Systembruker: ${event.key.getSystembruker()}," +
                "Tekst: ***")
    }
}