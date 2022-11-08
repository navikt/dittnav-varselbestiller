package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.getListFromSeparatedString
import no.nav.personbruker.dittnav.varselbestiller.common.database.getUtcDateTime
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

fun Connection.createVarselbestilling(varselbestilling: Varselbestilling) =
    prepareStatement(
        """INSERT INTO varselbestilling (bestillingsid, eventid, fodselsnummer, systembruker, eventtidspunkt, avbestilt, prefererteKanaler, namespace, appnavn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"""
    ).use {
        it.buildStatementForSingleRow(varselbestilling)
        it.executeUpdate()
    }

fun Connection.getVarselbestillingIfExists(eventId: String): Varselbestilling? =
    prepareStatement("""SELECT varselbestilling.* FROM varselbestilling WHERE eventid = ? """)
        .use {
            it.setString(1, eventId)
            it.executeQuery().use {
                    resultSet -> if(resultSet.next()) resultSet.toVarselbestilling() else null
            }
        }

fun Connection.cancelVarselbestilling(bestillingsId: String) {
    prepareStatement("""UPDATE varselbestilling SET avbestilt = ? WHERE bestillingsid = ?""")
        .use {
            it.setBoolean(1, true)
            it.setString(2, bestillingsId)
            it.executeUpdate()
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




