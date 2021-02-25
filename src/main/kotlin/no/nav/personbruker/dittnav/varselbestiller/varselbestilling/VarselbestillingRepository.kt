package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.Database
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult

class VarselbestillingRepository(private val database: Database) {

    suspend fun persistInOneBatch(entities: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        return database.queryWithExceptionTranslation {
            createVarselbestillinger(entities)
        }
    }

    suspend fun fetchVarselbestilling(eventId: String, systembruker: String, fodselsnummer: String): Varselbestilling? {
        var resultat: Varselbestilling? = null
        database.queryWithExceptionTranslation {
            resultat = getVarselbestillingForEvents(eventId = eventId, systembruker = systembruker, fodselsnummer = fodselsnummer)
        }
        return resultat
    }

    suspend fun fetchVarselbestilling(bestillingsId: String): Varselbestilling? {
        var resultat: Varselbestilling? = null
        database.queryWithExceptionTranslation {
            resultat = getVarselbestillingForBestillingsId(bestillingsId = bestillingsId)
        }
        return resultat
    }

    suspend fun fetchVarselbestillingerForBestillingIds(bestillingsIds: List<String>): List<Varselbestilling> {
        var resultat = emptyList<Varselbestilling>()
        database.queryWithExceptionTranslation {
            resultat = getVarselbestillingerForBestillingsIds(bestillingsIds)
        }
        return resultat
    }

    suspend fun cancelVarselbestilling(entities: List<Varselbestilling>) {
        database.queryWithExceptionTranslation {
            setVarselbestillingAvbestiltFlag(entities, true)
        }
    }
}
