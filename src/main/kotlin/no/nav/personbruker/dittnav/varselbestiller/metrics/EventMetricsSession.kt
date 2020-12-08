package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling

class EventMetricsSession(val eventtype: Eventtype) {
    private val numberOfAllEvents = HashMap<String, Int>()
    private val numberProcessedBySystemUser = HashMap<String, Int>()
    private val numberFailedBySystemUser = HashMap<String, Int>()
    private val numberDuplicateKeysBySystemUser = HashMap<String, Int>()
    private val startTime = System.nanoTime()
    private val nokkelIsNull = "NokkelIsNullNoProducerSpecified"

    fun countAllEventsFromKafkaForSystemUser(systemUser: String) {
        numberOfAllEvents[systemUser] = numberOfAllEvents.getOrDefault(systemUser, 0).inc()
    }

    fun countSuccessfulEventForSystemUser(systemUser: String) {
        numberProcessedBySystemUser[systemUser] = numberProcessedBySystemUser.getOrDefault(systemUser, 0).inc()
    }

    fun countFailedEventForSystemUser(systemUser: String) {
        if (systemUser == nokkelIsNull) {
            countAllEventsFromKafkaForSystemUser(systemUser)
        }
        numberFailedBySystemUser[systemUser] = numberFailedBySystemUser.getOrDefault(systemUser, 0).inc()
    }

    fun countDuplicateEventKeysBySystemUser(result: ListPersistActionResult<Varselbestilling>) {
        result.getConflictingEntities()
                .groupingBy { varselbestilling -> varselbestilling.systembruker }
                .eachCount()
                .forEach { (systembruker, duplicates) ->
                    numberDuplicateKeysBySystemUser[systembruker] = numberDuplicateKeysBySystemUser.getOrDefault(systembruker, 0) + duplicates
                }

    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getAllEvents(systemUser: String): Int {
        return numberOfAllEvents.getOrDefault(systemUser, 0)
    }

    fun getEventsSeen(systemUser: String): Int {
        return getEventsProcessed(systemUser) + getEventsFailed(systemUser)
    }

    fun getEventsProcessed(systemUser: String): Int {
        return numberProcessedBySystemUser.getOrDefault(systemUser, 0)
    }

    fun getEventsFailed(systemUser: String): Int {
        return numberFailedBySystemUser.getOrDefault(systemUser, 0)
    }

    fun getDuplicateKeyEvents(systemUser: String): Int {
        return numberDuplicateKeysBySystemUser.getOrDefault(systemUser, 0)
    }

    fun getAllEvents(): Int {
        return numberOfAllEvents.values.sum()
    }

    fun getEventsSeen(): Int {
        return getEventsProcessed() + getEventsFailed()
    }

    fun getEventsProcessed(): Int {
        return numberProcessedBySystemUser.values.sum()
    }

    fun getEventsFailed(): Int {
        return numberFailedBySystemUser.values.sum()
    }

    fun getNumberDuplicateKeysBySystemUser(): HashMap<String, Int> {
        return numberDuplicateKeysBySystemUser
    }

    fun getUniqueSystemUser(): List<String> {
        val systemUsers = ArrayList<String>()
        systemUsers.addAll(numberOfAllEvents.keys)
        systemUsers.addAll(numberProcessedBySystemUser.keys)
        systemUsers.addAll(numberFailedBySystemUser.keys)
        return systemUsers.distinct()
    }
}