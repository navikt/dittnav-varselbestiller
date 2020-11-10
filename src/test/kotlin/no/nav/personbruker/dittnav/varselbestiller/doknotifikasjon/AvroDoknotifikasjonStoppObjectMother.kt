package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp

object AvroDoknotifikasjonStoppObjectMother {

    fun giveMeANumberOfDoknotifikasjonStopp(numberOfEvents: Int): List<DoknotifikasjonStopp> {
        val doknotifikasjoner = mutableListOf<DoknotifikasjonStopp>()
        for(i in 0 until numberOfEvents) {
            doknotifikasjoner.add(createDoknotifikasjonStopp(i.toString(), i.toString()))
        }
        return doknotifikasjoner
    }

    fun createDoknotifikasjonStopp(bestillingsId: String, bestillerId: String): DoknotifikasjonStopp {
        return DoknotifikasjonStopp(bestillingsId, bestillerId)
    }
}
