package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.Database
import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult

class VarselbestillingRepository(private val database: Database) {

    suspend fun persistInOneBatch(entities: List<Varselbestilling>): ListPersistActionResult<Varselbestilling> {
        return database.queryWithExceptionTranslation {
            createVarselbestillinger(entities)
        }
    }

    suspend fun fetchVarselbestillingerForBestillingIds(bestillingsIds: List<String>): List<Varselbestilling> {
        var resultat = emptyList<Varselbestilling>()
        database.queryWithExceptionTranslation {
            resultat = getVarselbestillingerForBestillingsIds(bestillingsIds)
        }
        return resultat
    }

    suspend fun fetchVarselbestillingerForEventIds(eventIds: List<String>): List<Varselbestilling> {
        var resultat = emptyList<Varselbestilling>()
        database.queryWithExceptionTranslation {
            resultat = getVarselbestillingerForEventIds(eventIds)
        }
        return resultat
    }

    suspend fun cancelVarselbestilling(bestillingsIds: List<String>) {
        database.queryWithExceptionTranslation {
            setVarselbestillingAvbestiltFlag(bestillingsIds, true)
        }
    }
}
