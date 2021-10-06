package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.brukernotifikasjon.schemas.internal.domain.PreferertKanal
import java.time.LocalDateTime
import java.time.ZoneId

object VarselbestillingObjectMother {

    private val defaultEventId = "123"
    private val defaultFodselsnr = "12345678901"
    private val defaultAppnavn = "dummyAppnavn"
    private val defaultBestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC"))
    private val defaultBestillingsId = "B-$defaultAppnavn-$defaultEventId"
    private val defaultPrefererteKanaler = listOf(PreferertKanal.SMS.toString(), PreferertKanal.EPOST.toString())

    fun giveMeANumberOfVarselbestilling(numberOfEvents: Int): List<Varselbestilling> {
        val varselbestillinger = mutableListOf<Varselbestilling>()

        for (i in 0 until numberOfEvents) {
            varselbestillinger.add(createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-${defaultAppnavn}-$i", eventId = i.toString()))
        }
        return varselbestillinger
    }

    fun createVarselbestillingWithBestillingsIdAndEventId(bestillingsId: String, eventId: String): Varselbestilling {
        return createVarselbestilling(bestillingsId, eventId, defaultFodselsnr, defaultPrefererteKanaler)
    }

    fun createVarselbestillingWithPrefererteKanaler(prefererteKanaler: List<String>): Varselbestilling {
        return createVarselbestilling(defaultBestillingsId, defaultEventId, defaultFodselsnr, prefererteKanaler)
    }

    private fun createVarselbestilling(bestillingsId: String, eventId: String, fodselsnummer: String, prefererteKanaler: List<String>): Varselbestilling {
        return Varselbestilling(
                bestillingsId = bestillingsId,
                eventId = eventId,
                fodselsnummer = fodselsnummer,
                appnavn = defaultAppnavn,
                bestillingstidspunkt = defaultBestillingstidspunkt,
                prefererteKanaler = prefererteKanaler
        )
    }
}
