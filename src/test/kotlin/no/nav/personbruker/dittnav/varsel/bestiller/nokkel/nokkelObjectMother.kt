package no.nav.personbruker.dittnav.varsel.bestiller.nokkel

import no.nav.brukernotifikasjon.schemas.Nokkel

fun createNokkel(eventId: Int): Nokkel = Nokkel("dummySystembruker", eventId.toString())

fun createNokkelWithSystembruker(systembruker: String): Nokkel = Nokkel(systembruker, "dummyEventID")
