package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object VarselbestillingObjectMother {

    fun createVarselbestilling(
        bestillingsId: String = "B-dummyAppnavn-123",
        eventId: String = "123",
        fodselsnummer: String = "12345678901",
        systembruker: String = "dummySystembruker",
        namespace: String = "dummyNamespace",
        appnavn: String = "dummyAppnavn",
        bestillingstidspunkt: LocalDateTime = LocalDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.MILLIS),
        prefererteKanaler: List<String> = listOf(PreferertKanal.SMS.toString(), PreferertKanal.EPOST.toString())
    ): Varselbestilling {
        return Varselbestilling(
                bestillingsId = bestillingsId,
                eventId = eventId,
                fodselsnummer = fodselsnummer,
                systembruker = systembruker,
                namespace = namespace,
                appnavn = appnavn,
                bestillingstidspunkt = bestillingstidspunkt,
                prefererteKanaler = prefererteKanaler
        )
    }
}
