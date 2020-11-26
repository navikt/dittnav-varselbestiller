package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype

class EventMetricsSession(val eventType: Eventtype) {
    private val numberProcessedBySystemUser = HashMap<String, Int>()
    private val numberFailedBySystemUser = HashMap<String, Int>()
    private val numberDuplicateKeysBySystemUser = HashMap<String, Int>()


    fun countSuccessfulEventForProducer(systemUser: String) {
        numberProcessedBySystemUser[systemUser] = numberProcessedBySystemUser.getOrDefault(systemUser, 0).inc()
    }

    fun countFailedEventForProducer(systemUser: String) {
        numberFailedBySystemUser[systemUser] = numberFailedBySystemUser.getOrDefault(systemUser, 0).inc()
    }

    fun countDuplicateEventKeysByProducer(systemUser: String, number: Int = 1) {
        numberDuplicateKeysBySystemUser[systemUser] = numberDuplicateKeysBySystemUser.getOrDefault(systemUser, 0) + number
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

    fun getDuplicateKeyEvents(producer: String): Int {
        return numberDuplicateKeysBySystemUser.getOrDefault(producer, 0)
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

    fun getDuplicateKeyEvents(): Int {
        return numberDuplicateKeysBySystemUser.values.sum()
    }

    fun getUniqueSystemUser(): List<String> {
        val systemUsers = ArrayList<String>()
        systemUsers.addAll(numberProcessedBySystemUser.keys)
        systemUsers.addAll(numberFailedBySystemUser.keys)
        return systemUsers.distinct()
    }
}