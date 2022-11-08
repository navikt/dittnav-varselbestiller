package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.Database

class VarselbestillingRepository(private val database: Database) {

    suspend fun persistVarselbestilling(bestilling: Varselbestilling) =
        database.queryWithExceptionTranslation { createVarselbestilling(bestilling) }

    suspend fun getVarselbestillingIfExists(eventId: String) =
        database.queryWithExceptionTranslation { getVarselbestillingIfExists(eventId) }

    suspend fun cancelVarselbestilling(bestillingsId: String) =
        database.queryWithExceptionTranslation { cancelVarselbestilling(bestillingsId) }
}
