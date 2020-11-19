package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.common.util.database.fetching.mapSingleResult
import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.common.util.database.persisting.executeBatchPersistQuery
import no.nav.personbruker.dittnav.common.util.database.persisting.toBatchPersistResult
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime

fun Connection.createDoknotifikasjoner(doknotifikasjoner: List<Doknotifikasjon>): ListPersistActionResult<Doknotifikasjon> =
        executeBatchPersistQuery("""INSERT INTO doknotifikasjon (bestillingsid, eventtype, systembruker, eventtidspunkt) 
                                    |VALUES (?, ?, ?, ?)""".trimMargin()) {
                doknotifikasjoner.forEach { doknotifikasjon ->
                        buildStatementForSingleRow(doknotifikasjon)
                        addBatch()
                }
        }.toBatchPersistResult(doknotifikasjoner)

fun Connection.getDoknotifikasjonForBestillingsid(bestillingsid: String): Doknotifikasjon =
        prepareStatement("""SELECT doknotifikasjon.* FROM doknotifikasjon WHERE bestillingsid = ?""")
                .use {
                    it.setString(1, bestillingsid)
                    it.executeQuery().mapSingleResult { toDoknotifikasjon() }
                }

fun ResultSet.toDoknotifikasjon(): Doknotifikasjon {
    return Doknotifikasjon(
            bestillingsid = getString("bestillingsid"),
            eventtype = Eventtype.valueOf(getString("eventtype").toUpperCase()),
            systembruker = getString("systembruker"),
            eventtidspunkt = getUtcDateTime("eventtidspunkt")
    )
}

private fun PreparedStatement.buildStatementForSingleRow(doknotifikasjon: Doknotifikasjon) {
        setString(1, doknotifikasjon.bestillingsid)
        setObject(2, doknotifikasjon.eventtype.eventtype)
        setString(3, doknotifikasjon.systembruker)
        setObject(4, doknotifikasjon.eventtidspunkt, Types.TIMESTAMP)
}

private fun ResultSet.getUtcDateTime(columnLabel: String): LocalDateTime = getTimestamp(columnLabel).toLocalDateTime()
