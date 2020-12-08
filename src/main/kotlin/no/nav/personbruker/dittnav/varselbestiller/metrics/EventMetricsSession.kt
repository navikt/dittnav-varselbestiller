package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling

class EventMetricsSession(val eventtype: Eventtype) {
    private val countAllEventsFromKafkaBySysUser = HashMap<String, Int>()
    private val countProcessedEksternvarslingBySysUser = HashMap<String, Int>()
    private val countFailedEksternvarslingBySysUser = HashMap<String, Int>()
    private val countDuplicateKeyEksternvarslingBySysUser = HashMap<String, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countAllEventsFromKafkaForSystemUser(systemUser: String) {
        countAllEventsFromKafkaBySysUser[systemUser] = countAllEventsFromKafkaBySysUser.getOrDefault(systemUser, 0).inc()
    }

    fun countSuccessfulEksternvarslingForSystemUser(systemUser: String) {
        countProcessedEksternvarslingBySysUser[systemUser] = countProcessedEksternvarslingBySysUser.getOrDefault(systemUser, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEksternvarslingForSystemUser(systemUser: String) {
        countFailedEksternvarslingBySysUser[systemUser] = countFailedEksternvarslingBySysUser.getOrDefault(systemUser, 0).inc()
    }

    fun countDuplicateKeyEksternvarslingBySystemUser(result: ListPersistActionResult<Varselbestilling>) {
        result.getConflictingEntities()
                .groupingBy { varselbestilling -> varselbestilling.systembruker }
                .eachCount()
                .forEach { (systembruker, duplicates) ->
                    countDuplicateKeyEksternvarslingBySysUser[systembruker] = countDuplicateKeyEksternvarslingBySysUser.getOrDefault(systembruker, 0) + duplicates
                }

    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getAllEventsFromKafka(systemUser: String): Int {
        return countAllEventsFromKafkaBySysUser.getOrDefault(systemUser, 0)
    }

    fun getEksternvarslingEventsSeen(systemUser: String): Int {
        return getEksternvarslingEventsProcessed(systemUser) + getEksternvarslingEventsFailed(systemUser)
    }

    fun getEksternvarslingEventsProcessed(systemUser: String): Int {
        return countProcessedEksternvarslingBySysUser.getOrDefault(systemUser, 0)
    }

    fun getEksternvarslingEventsFailed(systemUser: String): Int {
        return countFailedEksternvarslingBySysUser.getOrDefault(systemUser, 0)
    }

    fun getEksternvarslingDuplicateKeys(systemUser: String): Int {
        return countDuplicateKeyEksternvarslingBySysUser.getOrDefault(systemUser, 0)
    }

    fun getAllEventsFromKafka(): Int {
        return countAllEventsFromKafkaBySysUser.values.sum() + countNokkelWasNull
    }

    fun getEksternvarslingEventsSeen(): Int {
        return getEksternvarslingEventsProcessed() + getEksternvarslingEventsFailed()
    }

    fun getEksternvarslingEventsProcessed(): Int {
        return countProcessedEksternvarslingBySysUser.values.sum()
    }

    fun getEksternvarslingEventsFailed(): Int {
        return countFailedEksternvarslingBySysUser.values.sum()
    }

    fun getEksternvarslingDuplicateKeys(): HashMap<String, Int> {
        return countDuplicateKeyEksternvarslingBySysUser
    }

    fun getUniqueSystemUser(): List<String> {
        val systemUsers = ArrayList<String>()
        systemUsers.addAll(countAllEventsFromKafkaBySysUser.keys)
        systemUsers.addAll(countProcessedEksternvarslingBySysUser.keys)
        systemUsers.addAll(countFailedEksternvarslingBySysUser.keys)
        return systemUsers.distinct()
    }
}