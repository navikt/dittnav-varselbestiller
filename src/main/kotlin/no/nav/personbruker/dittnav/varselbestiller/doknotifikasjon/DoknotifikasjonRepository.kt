package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import kotlinx.html.Entities
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.database.Database


class DoknotifikasjonRepository(private val database: Database) {

    suspend fun saveDoknotifikasjon(entities: List<Doknotifikasjon>) {
        return database.queryWithExceptionTranslation {

        }
    }
}
