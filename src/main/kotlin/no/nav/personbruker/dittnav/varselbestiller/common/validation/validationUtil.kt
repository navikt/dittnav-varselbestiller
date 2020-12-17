package no.nav.personbruker.dittnav.varselbestiller.common.validation

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil

fun throwExceptionIfBeskjedOrNokkelIsNotValid(nokkel: Nokkel, beskjed: Beskjed) {
    throwExceptionIfNokkelIsNotValid(nokkel)
    ValidationUtil.validateNonNullFieldMaxLength(beskjed.getFodselsnummer(), "fodselsnummer", ValidationUtil.MAX_LENGTH_FODSELSNUMMER)
    ValidationUtil.validateNonNullFieldMaxLength(beskjed.getGrupperingsId(), "grupperingsId", ValidationUtil.MAX_LENGTH_GRUPPERINGSID)
    ValidationUtil.validateNonNullFieldMaxLength(beskjed.getTekst(), "tekst", ValidationUtil.MAX_LENGTH_TEXT_BESKJED)
    ValidationUtil.validateLinkAndConvertToString(ValidationUtil.validateLinkAndConvertToURL(beskjed.getLink()), "link", ValidationUtil.MAX_LENGTH_LINK, ValidationUtil.isLinkRequired(Eventtype.BESKJED))
    ValidationUtil.validateSikkerhetsnivaa(beskjed.getSikkerhetsnivaa())
}

fun throwExceptionIfOppgaveOrNokkelIsNotValid(nokkel: Nokkel, oppgave: Oppgave) {
    throwExceptionIfNokkelIsNotValid(nokkel)
    ValidationUtil.validateNonNullFieldMaxLength(oppgave.getFodselsnummer(), "fodselsnummer", ValidationUtil.MAX_LENGTH_FODSELSNUMMER)
    ValidationUtil.validateNonNullFieldMaxLength(oppgave.getGrupperingsId(), "grupperingsId", ValidationUtil.MAX_LENGTH_GRUPPERINGSID)
    ValidationUtil.validateNonNullFieldMaxLength(oppgave.getTekst(), "tekst", ValidationUtil.MAX_LENGTH_TEXT_OPPGAVE)
    ValidationUtil.validateLinkAndConvertToString(ValidationUtil.validateLinkAndConvertToURL(oppgave.getLink()), "link", ValidationUtil.MAX_LENGTH_LINK, ValidationUtil.isLinkRequired(Eventtype.OPPGAVE))
    ValidationUtil.validateSikkerhetsnivaa(oppgave.getSikkerhetsnivaa())
}

private fun throwExceptionIfNokkelIsNotValid(nokkel: Nokkel) {
    ValidationUtil.validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", ValidationUtil.MAX_LENGTH_SYSTEMBRUKER)
    ValidationUtil.validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", ValidationUtil.MAX_LENGTH_EVENTID)
}

