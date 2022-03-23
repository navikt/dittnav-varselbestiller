package no.nav.personbruker.dittnav.varselbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object AvroNokkelInternObjectMother {

    private var eventIdCounter = 0
    private val defaultUlid = "dummyUlid"
    private val defaultGrupperingsid = "dummyGrupperingsid"
    private val defaultFodselsnummer = "12345678901"
    private val defaultNamespace = "dummyNamespace"
    private val defaultAppnavn = "dummyAppnavn"
    private val defaultSystembruker = "defaultSystembruker"

    fun createNokkelIntern(
        ulid: String = defaultUlid,
        eventId: String = (++eventIdCounter).toString(),
        grupperingsid: String = defaultGrupperingsid,
        fodselsnummer: String = defaultFodselsnummer,
        namespace: String = defaultNamespace,
        appnavn: String = defaultAppnavn,
        systembruker: String = defaultSystembruker
    ) = NokkelIntern(
        ulid,
        eventId,
        grupperingsid,
        fodselsnummer,
        namespace,
        appnavn,
        systembruker
    )

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
