package no.nav.personbruker.dittnav.varselbestiller.common.database

import java.sql.*
import java.time.LocalDateTime

fun Connection.executeBatchPersistQuery(sql: String, skipConflicting: Boolean = true, paramInit: PreparedStatement.() -> Unit): IntArray {
    autoCommit = false

    val finalSqlString = appendSkipStatementIfRequested(sql, skipConflicting)

    val result = prepareStatement(finalSqlString).use { statement ->
        statement.paramInit()
        statement.executeBatch()
    }
    commit()
    return result
}

fun <T> IntArray.toBatchPersistResult(paramList: List<T>) = ListPersistActionResult.mapParamListToResultArray(paramList, this)

private fun appendSkipStatementIfRequested(baseSql: String, skipConflicting: Boolean) =
        if (skipConflicting) {
            """$baseSql ON CONFLICT DO NOTHING"""
        } else {
            baseSql
        }

fun <T> ResultSet.mapList(result: ResultSet.() -> T): List<T> =
        mutableListOf<T>().apply {
            while (next()) {
                add(result())
            }
        }

fun ResultSet.getUtcDateTime(columnLabel: String): LocalDateTime = getTimestamp(columnLabel).toLocalDateTime()

fun ResultSet.getListFromSeparatedString(columnLabel: String, separator: String): List<String> {
    var stringValue = getString(columnLabel)
    return if(stringValue.isNullOrEmpty()) {
        emptyList()
    }
    else {
        stringValue.split(separator)
    }
}
