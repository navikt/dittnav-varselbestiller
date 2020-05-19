package no.nav.personbruker.dittnav.varsel.bestiller.done

import java.sql.Connection

fun Connection.deleteAllDone() =
        prepareStatement("""DELETE FROM DONE""")
                .use {it.execute()}
