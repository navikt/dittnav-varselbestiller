package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.common.util.database.persisting.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.database.Database

class VarselbestillingRepository(private val database: Database) {

    suspend fun persistInOneBatch(entities: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        return database.queryWithExceptionTranslation {
            createVarselbestillinger(entities)
        }
    }

    suspend fun fetchVarselbestilling(eventId: String, systembruker: String, fodselsnummer: String): Varselbestilling? {
        var resultat: Varselbestilling? = null
        database.queryWithExceptionTranslation {
            resultat = getVarselbestillingForEvent(eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        }
        return resultat
    }
}
