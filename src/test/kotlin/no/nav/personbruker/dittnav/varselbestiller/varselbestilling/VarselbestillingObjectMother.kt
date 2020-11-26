package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import java.time.LocalDateTime
import java.time.ZoneId

object VarselbestillingObjectMother {

    fun createVarselbestilling(bestillingsId: String, eventId: String, fodselsnummer: String): Varselbestilling {
        return Varselbestilling(
                bestillingsId = bestillingsId,
                eventId = eventId,
                fodselsnummer = fodselsnummer,
                systembruker = "dummySystembruker",
                bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC"))
        )
    }
}
