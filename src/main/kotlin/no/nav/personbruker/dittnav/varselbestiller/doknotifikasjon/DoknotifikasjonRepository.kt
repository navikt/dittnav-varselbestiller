package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.database.Database

class DoknotifikasjonRepository(private val database: Database) {

    suspend fun createInOneBatch(entities: List<Doknotifikasjon>): ListPersistActionResult<Doknotifikasjon> {
        return database.queryWithExceptionTranslation {
            createDoknotifikasjoner(entities)
        }
    }
}
