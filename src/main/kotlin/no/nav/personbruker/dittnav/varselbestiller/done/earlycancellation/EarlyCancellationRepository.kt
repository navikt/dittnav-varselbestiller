package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import no.nav.personbruker.dittnav.varselbestiller.common.database.*
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types

class EarlyCancellationRepository(private val database: Database) {

    suspend fun persistInBatch(entities: List<EarlyCancellation>): ListPersistActionResult<EarlyCancellation> {
        return database.queryWithExceptionTranslation {
            idempotentlyPersistEntities(entities)
        }
    }

    suspend fun findByEventIds(eventIds: List<String>): List<EarlyCancellation> {
        return database.queryWithExceptionTranslation {
            getEarlyCancellationsByEventIds(eventIds)
        }
    }

    suspend fun deleteByEventIds(eventIds: List<String>) {
        database.queryWithExceptionTranslation {
            deleteByEventIds(eventIds)
        }
    }
}

private fun Connection.getEarlyCancellationsByEventIds(eventIds: List<String>): List<EarlyCancellation> {
    return prepareStatement("""SELECT * FROM early_cancellation WHERE eventid = ANY(?) """)
        .use {
            it.setArray(1, toVarcharArray(eventIds))
            it.executeQuery().mapList { toEarlyCancellation() }
        }

}

private fun Connection.deleteByEventIds(eventIds: List<String>) {
    prepareStatement("""DELETE FROM early_cancellation WHERE eventid = ANY(?) """)
        .use {
            it.setArray(1, toVarcharArray(eventIds))
            it.executeQuery()
        }

}

private fun Connection.idempotentlyPersistEntities(entities: List<EarlyCancellation>): ListPersistActionResult<EarlyCancellation> {
    return executeBatchPersistQuery(
        """
            INSERT INTO early_cancellation (eventid, appnavn, namespace, fodselsnummer, systembruker, tidspunkt) 
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

private fun ResultSet.toEarlyCancellation(): EarlyCancellation {
    return EarlyCancellation(
        eventId = getString("eventid"),
        appnavn = getString("appnavn"),
        namespace = getString("namespace"),
        fodselsnummer = getString("fodselsnummer"),
        systembruker = getString("systembruker"),
        tidspunkt = getUtcDateTime("tidspunkt")
    )
}
