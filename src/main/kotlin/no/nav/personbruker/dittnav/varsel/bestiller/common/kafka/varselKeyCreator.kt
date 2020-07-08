package no.nav.personbruker.dittnav.varsel.bestiller.common.kafka

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength

fun createKeyForEvent(externalKey: Nokkel): Nokkel {
    return Nokkel.newBuilder()
            .setEventId(validateNonNullFieldMaxLength(externalKey.getEventId(), "eventId", 50))
            .setSystembruker(validateNonNullFieldMaxLength(externalKey.getSystembruker(), "systembruker", 100))
            .build()
}