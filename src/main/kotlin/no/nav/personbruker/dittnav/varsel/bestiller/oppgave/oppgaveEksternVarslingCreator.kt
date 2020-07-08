package no.nav.personbruker.dittnav.varsel.bestiller.oppgave

import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateSikkerhetsnivaa

fun createOppgaveEksternVarslingForEvent(oppgave: Oppgave): Oppgave {
    val build = Oppgave.newBuilder()
            .setFodselsnummer(validateNonNullFieldMaxLength(oppgave.getFodselsnummer(), "fodselsnummer", 11))
            .setGrupperingsId(validateNonNullFieldMaxLength(oppgave.getGrupperingsId(), "grupperingsId", 100))
            .setLink(validateMaxLength(oppgave.getLink(), "link", 200))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(oppgave.getSikkerhetsnivaa()))
            .setTekst(validateNonNullFieldMaxLength(oppgave.getTekst(), "tekst", 500))
            .setTidspunkt(oppgave.getTidspunkt())
    return build.build()
}