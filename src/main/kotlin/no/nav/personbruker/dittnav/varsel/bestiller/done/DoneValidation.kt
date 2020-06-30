package no.nav.personbruker.dittnav.varsel.bestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateFodselsnummer
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength

object DoneValidation {

    fun validateEvent(externalNokkel: Nokkel, externalValue: Done) {
        validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", 100)
        validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", 50)
        validateFodselsnummer(externalValue.getFodselsnummer())
    }
}
