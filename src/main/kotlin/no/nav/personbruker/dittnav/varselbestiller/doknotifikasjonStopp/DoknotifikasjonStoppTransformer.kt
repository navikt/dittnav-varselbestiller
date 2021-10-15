package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling

object DoknotifikasjonStoppTransformer {

    fun createDoknotifikasjonStopp(varselbestilling: Varselbestilling): DoknotifikasjonStopp {
        val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
                .setBestillingsId(varselbestilling.bestillingsId)
                .setBestillerId(varselbestilling.appnavn)
        return doknotifikasjonStoppBuilder.build()
    }
}
