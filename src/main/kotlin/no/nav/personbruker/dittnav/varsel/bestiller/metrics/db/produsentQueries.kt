package no.nav.personbruker.dittnav.varsel.bestiller.metrics.db

import no.nav.personbruker.dittnav.common.metrics.masking.NameAndPublicAlias
import no.nav.personbruker.dittnav.varsel.bestiller.common.database.util.list
import java.sql.Connection
import java.sql.ResultSet

fun Connection.getProdusentnavn(): List<NameAndPublicAlias> =
        prepareStatement("""SELECT * FROM systembrukere""")
                .use {
                    it.executeQuery().list {
                        toProdusent()
                    }
                }

private fun ResultSet.toProdusent(): NameAndPublicAlias {
    return NameAndPublicAlias(
            name = getString("systembruker"),
            publicAlias = getString("produsentnavn")
    )
}
