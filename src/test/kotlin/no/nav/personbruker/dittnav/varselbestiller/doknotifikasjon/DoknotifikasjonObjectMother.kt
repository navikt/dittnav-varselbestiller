package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneId

object DoknotifikasjonObjectMother {

    fun giveMeDoknotifikasjon(bestillingsid: String, eventtype: Eventtype): Doknotifikasjon {
        return Doknotifikasjon(
                bestillingsid = bestillingsid,
                eventtype = eventtype,
                systembruker = "dummySystembruker",
                eventtidspunkt = LocalDateTime.now(ZoneId.of("UTC"))
        )
    }
}
