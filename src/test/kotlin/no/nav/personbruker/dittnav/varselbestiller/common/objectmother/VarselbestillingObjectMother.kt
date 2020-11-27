package no.nav.personbruker.dittnav.varselbestiller.common.objectmother

import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import java.time.LocalDateTime
import java.time.ZoneId

private val defaultEventId = "bestillerId"
private val defaultFodselsnr = "1234"
private val defaultSystembruker = "x-dittnav"
private val defaultBestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC"))

fun giveMeANumberOfVarselbestilling(numberOfEvents: Int): List<Varselbestilling> {
    val varselbestillinger = mutableListOf<Varselbestilling>()

    for (i in 0 until numberOfEvents) {
        varselbestillinger.add(createVarselbestilling(i.toString()))
    }
    return varselbestillinger
}

private fun createVarselbestilling(bestillingsId: String): Varselbestilling {
    return Varselbestilling(
            bestillingsId,
            defaultEventId,
            defaultFodselsnr,
            defaultSystembruker,
            defaultBestillingstidspunkt)
}




