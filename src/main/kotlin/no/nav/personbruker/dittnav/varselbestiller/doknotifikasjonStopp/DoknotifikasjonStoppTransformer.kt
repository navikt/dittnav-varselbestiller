package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.common.validation.validateNonNullFieldMaxLength

object DoknotifikasjonStoppTransformer {

    fun createDoknotifikasjonStopp(nokkel: Nokkel): DoknotifikasjonStopp {
        val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
                .setBestillingsId(validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", 50))
                .setBestillerId(validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", 100))
        return doknotifikasjonStoppBuilder.build()
    }
}
