package no.nav.personbruker.dittnav.varsel.bestiller.common.database.util

import java.sql.ResultSet

fun <T> ResultSet.list(result: ResultSet.() -> T): List<T> =
        mutableListOf<T>().apply {
            while (next()) {
                add(result())
            }
        }
