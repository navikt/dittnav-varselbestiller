package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.common.util.database.fetching.mapList
import java.sql.Connection

fun Connection.deleteAllVarselbestilling() =
        prepareStatement("""DELETE FROM varselbestilling""")
                .use { it.execute() }

fun Connection.getAllVarselbestilling() =
        prepareStatement("""SELECT varselbestilling.* FROM varselbestilling""")
                .use {
                    it.executeQuery().mapList { toVarselbestilling() }
                }
