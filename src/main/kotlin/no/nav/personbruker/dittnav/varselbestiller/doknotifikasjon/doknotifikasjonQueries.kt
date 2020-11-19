package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.common.util.database.fetching.mapSingleResult
import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.common.util.database.persisting.executeBatchPersistQuery
import no.nav.personbruker.dittnav.common.util.database.persisting.toBatchPersistResult
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import java.sql.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

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

private fun PreparedStatement.buildStatementForSingleRow(doknotifikasjon: Doknotifikasjon) {
        setString(1, doknotifikasjon.bestillingsid)
        setString(2, doknotifikasjon.eventtype.eventType)
        setString(3, doknotifikasjon.systembruker)
        setObject(4, doknotifikasjon.eventtidspunkt, Types.TIMESTAMP)
}

private fun ResultSet.toDoknotifikasjon(): Doknotifikasjon {
    return Doknotifikasjon(
            bestillingsid = getString("bestillingsid"),
            eventtype = Eventtype.valueOf(getString("eventtype")),
            systembruker = getString("systembruker"),
            eventtidspunkt = ZonedDateTime.ofInstant(getUtcTimeStamp("eventTidspunkt").toInstant(), ZoneId.of("Europe/Oslo"))
    )
}

private fun ResultSet.getUtcTimeStamp(label: String): Timestamp = getTimestamp(label, Calendar.getInstance(TimeZone.getTimeZone("UTC")))
