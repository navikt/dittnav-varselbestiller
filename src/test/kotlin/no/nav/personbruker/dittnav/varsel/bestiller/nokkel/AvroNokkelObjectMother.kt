package no.nav.personbruker.dittnav.varsel.bestiller.nokkel

import no.nav.brukernotifikasjon.schemas.Nokkel

object AvroNokkelObjectMother {

    fun createNokkelWithEventId(eventId: Int): Nokkel = Nokkel("dummySystembruker", eventId.toString())

    fun createNokkelWithEventId(eventId: String): Nokkel = Nokkel("dummySystembruker", eventId)

    fun createNokkelWithSystembruker(systembruker: String): Nokkel = Nokkel(systembruker, "dummyEventID")

}
