package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.common.util.database.fetching.mapSingleResult
import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.common.util.database.persisting.executeBatchPersistQuery
import no.nav.personbruker.dittnav.common.util.database.persisting.toBatchPersistResult
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime

fun Connection.createVarselbestillinger(doknotifikasjoner: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> =
        executeBatchPersistQuery("""INSERT INTO varselbestilling (bestillingsid, eventid, fodselsnummer, systembruker, eventtidspunkt) 
                                    |VALUES (?, ?, ?, ?, ?)""".trimMargin()) {
                doknotifikasjoner.forEach { doknotifikasjon ->
                        buildStatementForSingleRow(doknotifikasjon)
                        addBatch()
                }
        }.toBatchPersistResult(doknotifikasjoner)

fun Connection.getVarselbestillingForBestillingsId(bestillingsId: String): Varselbestilling =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE bestillingsid = ?""")
                .use {
                    it.setString(1, bestillingsId)
                    it.executeQuery().mapSingleResult { toVarselbestilling() }
                }

fun Connection.getVarselbestillingForEvent(eventId: String, systembruker: String, fodselsnummer: String) =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE eventid = ? AND systembruker = ? AND fodselsnummer = ?""")
                .use {
                    it.setString(1, eventId)
                    it.setString(2, systembruker)
                    it.setString(3, fodselsnummer)
                    it.executeQuery().mapSingleResult { toVarselbestilling() }
                }

fun ResultSet.toVarselbestilling(): Varselbestilling {
    return Varselbestilling(
            bestillingsId = getString("bestillingsid"),
            eventId = getString("eventid"),
            fodselsnummer = getString("fodselsnummer"),
            systembruker = getString("systembruker"),
            bestillingstidspunkt = getUtcDateTime("eventtidspunkt")
    )
}

private fun PreparedStatement.buildStatementForSingleRow(varselbestilling: Varselbestilling) {
        setString(1, varselbestilling.bestillingsId)
        setString(2, varselbestilling.eventId)
        setString(3, varselbestilling.fodselsnummer)
        setString(4, varselbestilling.systembruker)
        setObject(5, varselbestilling.bestillingstidspunkt, Types.TIMESTAMP)
}

private fun ResultSet.getUtcDateTime(columnLabel: String): LocalDateTime = getTimestamp(columnLabel).toLocalDateTime()
