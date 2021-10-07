package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling

class EventMetricsSession(val eventtype: Eventtype) {
    private val countAllEventsFromKafkaByProducer = HashMap<Producer, Int>()
    private val countProcessedEksternvarslingByProducer = HashMap<Producer, Int>()
    private val countFailedEksternvarslingByProducer = HashMap<Producer, Int>()
    private val countDuplicateKeyEksternvarslingByProducer = HashMap<Producer, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countAllEventsFromKafkaForProducer(namespace: String, appnavn: String) {
        val producer = Producer(namespace, appnavn)
        countAllEventsFromKafkaByProducer[producer] = countAllEventsFromKafkaByProducer.getOrDefault(producer, 0).inc()
    }

    fun countSuccessfulEksternVarslingForProducer(namespace: String, appnavn: String) {
        val producer = Producer(namespace, appnavn)
        countProcessedEksternvarslingByProducer[producer] = countProcessedEksternvarslingByProducer.getOrDefault(producer, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEksternvarslingForProducer(namespace: String, appnavn: String) {
        val producer = Producer(namespace, appnavn)
        countFailedEksternvarslingByProducer[producer] = countFailedEksternvarslingByProducer.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateVarselbestillingForProducer(namespace: String, appnavn: String) {
        val producer = Producer(namespace, appnavn)
        countDuplicateKeyEksternvarslingByProducer[producer] = countDuplicateKeyEksternvarslingByProducer.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateKeyEksternvarslingByProducer(result: ListPersistActionResult<Varselbestilling>) {
        result.getConflictingEntities()
                .groupingBy { varselbestilling -> Producer(varselbestilling.namespace, varselbestilling.appnavn) }
                .eachCount()
                .forEach { (producer, duplicates) ->
                    countDuplicateKeyEksternvarslingByProducer[producer] = countDuplicateKeyEksternvarslingByProducer.getOrDefault(producer, 0) + duplicates
                }

    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getAllEventsFromKafka(namespace: String, appnavn: String): Int {
        return countAllEventsFromKafkaByProducer.getOrDefault(Producer(namespace, appnavn), 0)
    }

    fun getEksternvarslingEventsSeen(namespace: String, appnavn: String): Int {
        return getEksternvarslingEventsProcessed(namespace, appnavn) + getEksternvarslingEventsFailed(namespace, appnavn)
    }

    fun getEksternvarslingEventsProcessed(namespace: String, appnavn: String): Int {
        return countProcessedEksternvarslingByProducer.getOrDefault(Producer(namespace, appnavn), 0)
    }

    fun getEksternvarslingEventsFailed(namespace: String, appnavn: String): Int {
        return countFailedEksternvarslingByProducer.getOrDefault(Producer(namespace, appnavn), 0)
    }

    fun getEksternvarslingDuplicateKeys(namespace: String, appnavn: String): Int {
        return countDuplicateKeyEksternvarslingByProducer.getOrDefault(Producer(namespace, appnavn), 0)
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

    fun getEksternvarslingDuplicateKeys(): HashMap<Producer, Int> {
        return countDuplicateKeyEksternvarslingByProducer
    }

    fun getNokkelWasNull(): Int {
        return countNokkelWasNull
    }

    fun getUniqueProducers(): List<Producer> {
        val producers = ArrayList<Producer>()
        producers.addAll(countAllEventsFromKafkaByProducer.keys)
        producers.addAll(countProcessedEksternvarslingByProducer.keys)
        producers.addAll(countFailedEksternvarslingByProducer.keys)
        return producers.distinct()
    }
}
