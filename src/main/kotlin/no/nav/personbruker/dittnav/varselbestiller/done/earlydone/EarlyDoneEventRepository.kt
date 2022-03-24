package no.nav.personbruker.dittnav.varselbestiller.done.earlydone

import no.nav.personbruker.dittnav.varselbestiller.common.database.*
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types

class EarlyDoneEventRepository(private val database: Database) {

    suspend fun persistInBatch(entities: List<EarlyDoneEvent>): ListPersistActionResult<EarlyDoneEvent> {
        return database.queryWithExceptionTranslation {
            idempotentlyPersistEntities(entities)
        }
    }

    suspend fun findByEventIds(eventIds: List<String>): List<EarlyDoneEvent> {
        return database.queryWithExceptionTranslation {
            getEarlyDoneByEventIds(eventIds)
        }
    }

    suspend fun deleteByEventIds(eventIds: List<String>) {
        database.queryWithExceptionTranslation {
            deleteByEventIds(eventIds)
        }
    }
}

private fun Connection.getEarlyDoneByEventIds(eventIds: List<String>): List<EarlyDoneEvent> {
    return prepareStatement("""SELECT * FROM early_done_event WHERE eventid = ANY(?) """)
        .use {
            it.setArray(1, toVarcharArray(eventIds))
            it.executeQuery().mapList { toEarlyDoneEvent() }
        }

}

private fun Connection.deleteByEventIds(eventIds: List<String>) {
    prepareStatement("""DELETE FROM early_done_event WHERE eventid = ANY(?) """)
        .use {
            it.setArray(1, toVarcharArray(eventIds))
            it.executeQuery()
        }

}

private fun Connection.idempotentlyPersistEntities(entities: List<EarlyDoneEvent>): ListPersistActionResult<EarlyDoneEvent> {
    return executeBatchPersistQuery(
        """
            INSERT INTO early_done_event (eventid, appnavn, namespace, fodselsnummer, systembruker, tidspunkt) 
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        skipConflicting = true
    ) {
        entities.forEach { entity ->
            setString(1, entity.eventId)
            setString(2, entity.appnavn)
            setString(3, entity.namespace)
            setString(4, entity.fodselsnummer)
            setString(5, entity.systembruker)
            setObject(6, entity.tidspunkt, Types.TIMESTAMP)
            addBatch()
        }
    }.toBatchPersistResult(entities)
}

private fun ResultSet.toEarlyDoneEvent(): EarlyDoneEvent {
    return EarlyDoneEvent(
        eventId = getString("eventid"),
        appnavn = getString("appnavn"),
        namespace = getString("namespace"),
        fodselsnummer = getString("fodselsnummer"),
        systembruker = getString("systembruker"),
        tidspunkt = getUtcDateTime("tidspunkt")
    )
}
