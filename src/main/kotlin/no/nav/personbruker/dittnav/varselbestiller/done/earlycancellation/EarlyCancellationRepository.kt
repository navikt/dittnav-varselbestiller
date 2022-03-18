package no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation

import no.nav.personbruker.dittnav.varselbestiller.common.database.*
import java.sql.Connection
import java.sql.ResultSet


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
}

private fun Connection.getEarlyCancellationsByEventIds(eventIds: List<String>): List<EarlyCancellation> {
    return prepareStatement("""SELECT * FROM early_cancellation WHERE eventid = ANY(?) """)
        .use {
            it.setArray(1, toVarcharArray(eventIds))
            it.executeQuery().mapList { toEarlyCancellation() }
        }

}

private fun Connection.idempotentlyPersistEntities(entities: List<EarlyCancellation>): ListPersistActionResult<EarlyCancellation> {
    return executeBatchPersistQuery(
        """
            INSERT INTO early_cancellation (eventid, appnavn) 
            VALUES (?, ?)
        """.trimIndent(),
        skipConflicting = true
    ) {
        entities.forEach { entity ->
            setString(1, entity.eventId)
            setString(2, entity.appnavn)
            addBatch()
        }
    }.toBatchPersistResult(entities)
}

private fun ResultSet.toEarlyCancellation(): EarlyCancellation {
    return EarlyCancellation(
        eventId = getString("eventid"),
        appnavn = getString("appnavn")
    )
}
