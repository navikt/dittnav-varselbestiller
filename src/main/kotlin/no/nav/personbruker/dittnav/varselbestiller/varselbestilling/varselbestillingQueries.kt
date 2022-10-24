package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.*
import java.sql.*
import java.sql.Array

fun Connection.createVarselbestillinger(varselbestillinger: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> =
    executeBatchPersistQuery(
        """INSERT INTO varselbestilling (bestillingsid, eventid, fodselsnummer, systembruker, eventtidspunkt, avbestilt, prefererteKanaler, namespace, appnavn) 
                                    |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimMargin()
    ) {
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

fun Connection.getVarselbestillingForEventId(eventId: String): Varselbestilling? =
    prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE eventid = ? """)
        .use {
            it.setString(1, eventId)
            it.executeQuery().mapList { toVarselbestilling() }.let { result ->
                if (result.size > 1) {
                    throw DupilcateEventIdExcpetion(eventId = eventId, antall = result.size)
                }
                result.firstOrNull()
            }
        }


fun Connection.setVarselbestillingAvbestiltFlag(bestillingsIds: List<String>, avbestilt: Boolean) {
    executeBatchPersistQuery(
        """UPDATE varselbestilling SET avbestilt = ? WHERE bestillingsid = ?""",
        skipConflicting = false
    ) {
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
        namespace = getString("namespace"),
        appnavn = getString("appnavn"),
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
    setString(8, varselbestilling.namespace)
    setString(9, varselbestilling.appnavn)
}

private fun Connection.toVarcharArray(stringList: List<String>): Array {
    return createArrayOf("VARCHAR", stringList.toTypedArray())
}


internal class DupilcateEventIdExcpetion(eventId: String, antall: Int) :
    Exception("Fant mer enn én ($antall) varselbestilling for $eventId")
