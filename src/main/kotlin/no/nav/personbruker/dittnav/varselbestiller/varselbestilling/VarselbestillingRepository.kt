package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.Database

class VarselbestillingRepository(private val database: Database) {

    suspend fun persistVarselbestilling(bestilling: Varselbestilling) =
        database.queryWithExceptionTranslation(bestilling.eventId) { createVarselbestilling(bestilling) }

    suspend fun getVarselbestillingIfExists(eventId: String) =
        database.queryWithExceptionTranslation(eventId) { getVarselbestillingIfExists(eventId) }

    suspend fun cancelVarselbestilling(bestillingsId: String) =
        database.queryWithExceptionTranslation(bestillingsId) { cancelVarselbestilling(bestillingsId) }
}
