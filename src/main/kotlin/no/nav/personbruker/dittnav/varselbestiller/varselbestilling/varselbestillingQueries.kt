package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.database.executeBatchPersistQuery
import no.nav.personbruker.dittnav.varselbestiller.common.database.toBatchPersistResult
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime

fun Connection.createVarselbestillinger(varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> =
        executeBatchPersistQuery("""INSERT INTO varselbestilling (bestillingsid, eventid, fodselsnummer, systembruker, eventtidspunkt, avbestilt) 
                                    |VALUES (?, ?, ?, ?, ?, ?)""".trimMargin()) {
                varselbestillinger.forEach { varselbestilling ->
                        buildStatementForSingleRow(varselbestilling)
                        addBatch()
                }
        }.toBatchPersistResult(varselbestillinger)

fun Connection.getVarselbestillingForBestillingsId(bestillingsId: String): Varselbestilling? =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE bestillingsid = ?""")
                .use {
                    it.setString(1, bestillingsId)
                    it.executeQuery().mapSingleResultNullable { toVarselbestilling() }
                }

fun Connection.getVarselbestillingForEvent(eventId: String, systembruker: String, fodselsnummer: String): Varselbestilling? =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE eventid = ? AND systembruker = ? AND fodselsnummer = ?""")
                .use {
                    it.setString(1, eventId)
                    it.setString(2, systembruker)
                    it.setString(3, fodselsnummer)
                    it.executeQuery().mapSingleResultNullable { toVarselbestilling() }
                }

fun Connection.setVarselbestillingAvbestiltFlag(varselbestillinger: List<Varselbestilling>, avbestilt: Boolean) {
    executeBatchPersistQuery("""UPDATE varselbestilling SET avbestilt = ? WHERE bestillingsid = ?""", skipConflicting = false) {
        varselbestillinger.forEach { varselbestilling ->
            setBoolean(1, avbestilt)
            setString(2, varselbestilling.bestillingsId)
            addBatch()
        }
    }
}

fun ResultSet.toVarselbestilling(): Varselbestilling {
    return Varselbestilling(
            bestillingsId = getString("bestillingsid"),
            eventId = getString("eventid"),
            fodselsnummer = getString("fodselsnummer"),
            systembruker = getString("systembruker"),
            bestillingstidspunkt = getUtcDateTime("eventtidspunkt"),
            avbestilt = getBoolean("avbestilt")
    )
}

private fun PreparedStatement.buildStatementForSingleRow(varselbestilling: Varselbestilling) {
        setString(1, varselbestilling.bestillingsId)
        setString(2, varselbestilling.eventId)
        setString(3, varselbestilling.fodselsnummer)
        setString(4, varselbestilling.systembruker)
        setObject(5, varselbestilling.bestillingstidspunkt, Types.TIMESTAMP)
        setObject(6, varselbestilling.avbestilt)
}

private fun ResultSet.getUtcDateTime(columnLabel: String): LocalDateTime = getTimestamp(columnLabel).toLocalDateTime()

private fun <T> ResultSet.mapSingleResultNullable(result: ResultSet.() -> T): T? =
        if (next()) {
            result()
        } else {
            null
        }
