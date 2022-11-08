package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import java.sql.Connection
import java.sql.ResultSet

fun Connection.deleteAllVarselbestilling() =
        prepareStatement("""DELETE FROM varselbestilling""")
                .use { it.execute() }

fun Connection.getAllVarselbestilling() =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling""")
                .use {
                    it.executeQuery().mapList { toVarselbestilling() }
                }

private fun <T> ResultSet.mapList(result: ResultSet.() -> T): List<T> =
        mutableListOf<T>().apply {
                while (next()) {
                        add(result())
                }
        }