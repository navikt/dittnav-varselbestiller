package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling

class EventMetricsSession(val eventtype: Eventtype) {
    private val countAllEventsFromKafkaByProducer = HashMap<String, Int>()
    private val countProcessedEksternvarslingByProducer = HashMap<String, Int>()
    private val countFailedEksternvarslingByProducer = HashMap<String, Int>()
    private val countDuplicateKeyEksternvarslingByProducer = HashMap<String, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countAllEventsFromKafkaForProducer(producer: String) {
        countAllEventsFromKafkaByProducer[producer] = countAllEventsFromKafkaByProducer.getOrDefault(producer, 0).inc()
    }

    fun countSuccessfulEksternVarslingForProducer(producer: String) {
        countProcessedEksternvarslingByProducer[producer] = countProcessedEksternvarslingByProducer.getOrDefault(producer, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEksternvarslingForProducer(producer: String) {
        countFailedEksternvarslingByProducer[producer] = countFailedEksternvarslingByProducer.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateVarselbestillingForProducer(producer: String) {
        countDuplicateKeyEksternvarslingByProducer[producer] = countDuplicateKeyEksternvarslingByProducer.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateKeyEksternvarslingByProducer(result: ListPersistActionResult<Varselbestilling>) {
        result.getConflictingEntities()
                .groupingBy { varselbestilling -> varselbestilling.appnavn }
                .eachCount()
                .forEach { (producer, duplicates) ->
                    countDuplicateKeyEksternvarslingByProducer[producer] = countDuplicateKeyEksternvarslingByProducer.getOrDefault(producer, 0) + duplicates
                }

    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getAllEventsFromKafka(producer: String): Int {
        return countAllEventsFromKafkaByProducer.getOrDefault(producer, 0)
    }

    fun getEksternvarslingEventsSeen(producer: String): Int {
        return getEksternvarslingEventsProcessed(producer) + getEksternvarslingEventsFailed(producer)
    }

    fun getEksternvarslingEventsProcessed(producer: String): Int {
        return countProcessedEksternvarslingByProducer.getOrDefault(producer, 0)
    }

    fun getEksternvarslingEventsFailed(producer: String): Int {
        return countFailedEksternvarslingByProducer.getOrDefault(producer, 0)
    }

    fun getEksternvarslingDuplicateKeys(producer: String): Int {
        return countDuplicateKeyEksternvarslingByProducer.getOrDefault(producer, 0)
    }

    fun getAllEventsFromKafka(): Int {
        return countAllEventsFromKafkaByProducer.values.sum() + countNokkelWasNull
    }

    fun getEksternvarslingEventsSeen(): Int {
        return getEksternvarslingEventsProcessed() + getEksternvarslingEventsFailed()
    }

    fun getEksternvarslingEventsProcessed(): Int {
        return countProcessedEksternvarslingByProducer.values.sum()
    }

    fun getEksternvarslingEventsFailed(): Int {
        return countFailedEksternvarslingByProducer.values.sum()
    }

    fun getEksternvarslingDuplicateKeys(): HashMap<String, Int> {
        return countDuplicateKeyEksternvarslingByProducer
    }

    fun getNokkelWasNull(): Int {
        return countNokkelWasNull
    }

    fun getUniqueProducers(): List<String> {
        val producers = ArrayList<String>()
        producers.addAll(countAllEventsFromKafkaByProducer.keys)
        producers.addAll(countProcessedEksternvarslingByProducer.keys)
        producers.addAll(countFailedEksternvarslingByProducer.keys)
        return producers.distinct()
    }
}
