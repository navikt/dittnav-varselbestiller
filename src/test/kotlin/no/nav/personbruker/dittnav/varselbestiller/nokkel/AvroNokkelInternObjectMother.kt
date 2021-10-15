package no.nav.personbruker.dittnav.varselbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object AvroNokkelInternObjectMother {

    private val defaultUlid = "dummyUlid"
    private val defaultGrupperingsid = "dummyGrupperingsid"
    private val defaultFodselsnummer = "12345678901"
    private val defaultNamespace = "dummyNamespace"
    private val defaultAppnavn = "dummyAppnavn"
    private val defaultSystembruker = "defaultSystembruker"

    fun createNokkelInternWithEventId(eventId: Int): NokkelIntern = NokkelIntern(
        defaultUlid,
        eventId.toString(),
        defaultGrupperingsid,
        defaultFodselsnummer,
        defaultNamespace,
        defaultAppnavn,
        defaultSystembruker
    )

    fun createNokkelInternWithEventId(eventId: String): NokkelIntern = NokkelIntern(
        defaultUlid,
        eventId,
        defaultGrupperingsid,
        defaultFodselsnummer,
        defaultNamespace,
        defaultAppnavn,
        defaultSystembruker
    )

}
