package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateSikkerhetsnivaa

fun createBeskjedEksternVarslingForEvent(beskjed: Beskjed): Beskjed {
    val build = Beskjed.newBuilder()
            .setTidspunkt(beskjed.getTidspunkt())
            .setSynligFremTil(beskjed.getSynligFremTil())
            .setFodselsnummer(validateNonNullFieldMaxLength(beskjed.getFodselsnummer(), "fodselsnummer", 11))
            .setGrupperingsId(validateNonNullFieldMaxLength(beskjed.getGrupperingsId(), "grupperingsId", 100))
            .setTekst(validateNonNullFieldMaxLength(beskjed.getTekst(), "tekst", 300))
            .setLink(validateMaxLength(beskjed.getLink(), "link", 200))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(beskjed.getSikkerhetsnivaa()))
    return build.build()
}
