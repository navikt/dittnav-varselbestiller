package no.nav.personbruker.dittnav.varselbestiller.common.validation

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil

fun throwExceptionIfBeskjedOrNokkelIsInvalid(nokkel: Nokkel, beskjed: Beskjed) {
    throwExceptionIfNokkelIsInvalid(nokkel)
    ValidationUtil.validateNonNullFieldMaxLength(beskjed.getFodselsnummer(), "fodselsnummer", ValidationUtil.MAX_LENGTH_FODSELSNUMMER)
    ValidationUtil.validateNonNullFieldMaxLength(beskjed.getGrupperingsId(), "grupperingsId", ValidationUtil.MAX_LENGTH_GRUPPERINGSID)
    ValidationUtil.validateNonNullFieldMaxLength(beskjed.getTekst(), "tekst", ValidationUtil.MAX_LENGTH_TEXT_BESKJED)
    ValidationUtil.validateLinkAndConvertToString(ValidationUtil.validateLinkAndConvertToURL(beskjed.getLink()), "link", ValidationUtil.MAX_LENGTH_LINK, ValidationUtil.isLinkRequired(Eventtype.BESKJED))
    ValidationUtil.validateSikkerhetsnivaa(beskjed.getSikkerhetsnivaa())
}

fun throwExceptionIfOppgaveOrNokkelIsInvalid(nokkel: Nokkel, oppgave: Oppgave) {
    throwExceptionIfNokkelIsInvalid(nokkel)
    ValidationUtil.validateNonNullFieldMaxLength(oppgave.getFodselsnummer(), "fodselsnummer", ValidationUtil.MAX_LENGTH_FODSELSNUMMER)
    ValidationUtil.validateNonNullFieldMaxLength(oppgave.getGrupperingsId(), "grupperingsId", ValidationUtil.MAX_LENGTH_GRUPPERINGSID)
    ValidationUtil.validateNonNullFieldMaxLength(oppgave.getTekst(), "tekst", ValidationUtil.MAX_LENGTH_TEXT_OPPGAVE)
    ValidationUtil.validateLinkAndConvertToString(ValidationUtil.validateLinkAndConvertToURL(oppgave.getLink()), "link", ValidationUtil.MAX_LENGTH_LINK, ValidationUtil.isLinkRequired(Eventtype.OPPGAVE))
    ValidationUtil.validateSikkerhetsnivaa(oppgave.getSikkerhetsnivaa())
}

fun throwExceptionIfInnboksOrNokkelIsInvalid(nokkel: Nokkel, innboks: Innboks) {
    throwExceptionIfNokkelIsInvalid(nokkel)
    ValidationUtil.validateNonNullFieldMaxLength(innboks.getFodselsnummer(), "fodselsnummer", ValidationUtil.MAX_LENGTH_FODSELSNUMMER)
    ValidationUtil.validateNonNullFieldMaxLength(innboks.getGrupperingsId(), "grupperingsId", ValidationUtil.MAX_LENGTH_GRUPPERINGSID)
    ValidationUtil.validateNonNullFieldMaxLength(innboks.getTekst(), "tekst", ValidationUtil.MAX_LENGTH_TEXT_OPPGAVE)
    ValidationUtil.validateLinkAndConvertToString(ValidationUtil.validateLinkAndConvertToURL(innboks.getLink()), "link", ValidationUtil.MAX_LENGTH_LINK, ValidationUtil.isLinkRequired(Eventtype.INNBOKS))
    ValidationUtil.validateSikkerhetsnivaa(innboks.getSikkerhetsnivaa())
}

private fun throwExceptionIfNokkelIsInvalid(nokkel: Nokkel) {
    ValidationUtil.validateNonNullFieldMaxLength(nokkel.getSystembruker(), "systembruker", ValidationUtil.MAX_LENGTH_SYSTEMBRUKER)
    ValidationUtil.validateNonNullFieldMaxLength(nokkel.getEventId(), "eventId", ValidationUtil.MAX_LENGTH_EVENTID)
}
