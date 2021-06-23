package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.*
import java.sql.*
import java.sql.Array

fun Connection.createVarselbestillinger(varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> =
        executeBatchPersistQuery("""INSERT INTO varselbestilling (bestillingsid, eventid, fodselsnummer, systembruker, eventtidspunkt, avbestilt, prefererteKanaler) 
                                    |VALUES (?, ?, ?, ?, ?, ?, ?)""".trimMargin()) {
                varselbestillinger.forEach { varselbestilling ->
                        buildStatementForSingleRow(varselbestilling)
                        addBatch()
                }
        }.toBatchPersistResult(varselbestillinger)


fun Connection.getVarselbestillingerForBestillingsIds(bestillingsIds: List<String>): List<Varselbestilling> =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE bestillingsid = ANY(?)""")
                .use {
                    it.setArray(1, toVarcharArray(bestillingsIds))
                    it.executeQuery().mapList { toVarselbestilling() }
                }

fun Connection.getVarselbestillingerForEventIds(eventIds: List<String>): List<Varselbestilling> =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE eventid = ANY(?) """)
                .use {
                    it.setArray(1, toVarcharArray(eventIds))
                    it.executeQuery().mapList { toVarselbestilling() }
                }

fun Connection.setVarselbestillingAvbestiltFlag(bestillingsIds: List<String>, avbestilt: Boolean) {
    executeBatchPersistQuery("""UPDATE varselbestilling SET avbestilt = ? WHERE bestillingsid = ?""", skipConflicting = false) {
        bestillingsIds.forEach { bestillingsId ->
            setBoolean(1, avbestilt)
            setString(2, bestillingsId)
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
            prefererteKanaler = getListFromSeparatedString("prefererteKanaler", ","),
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
        setObject(7, varselbestilling.prefererteKanaler.joinToString(","))
}

private fun Connection.toVarcharArray(stringList: List<String>): Array {
    return createArrayOf("VARCHAR", stringList.toTypedArray())
}




