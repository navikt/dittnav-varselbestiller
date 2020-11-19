package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.common.util.database.fetching.mapList
import java.sql.Connection

fun Connection.deleteAllDoknotifikasjon() =
        prepareStatement("""DELETE FROM doknotifikasjon""")
                .use { it.execute() }

fun Connection.getAllDoknotifikasjon() =
        prepareStatement("""SELECT doknotifikasjon.* FROM doknotifikasjon""")
                .use {
                    it.executeQuery().mapList { toDoknotifikasjon() }
                }


