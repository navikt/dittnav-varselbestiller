package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.personbruker.dittnav.varselbestiller.common.database.Database

class VarselbestillingRepository(private val database: Database) {

    fun persistVarselbestilling(bestilling: Varselbestilling) =
        database.queryWithExceptionTranslation(bestilling.eventId) { createVarselbestilling(bestilling) }

    fun getVarselbestillingIfExists(eventId: String) =
        database.queryWithExceptionTranslation(eventId) { getVarselbestillingIfExists(eventId) }

    fun cancelVarselbestilling(bestillingsId: String) =
        database.queryWithExceptionTranslation(bestillingsId) { cancelVarselbestilling(bestillingsId) }
}
