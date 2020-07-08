package no.nav.personbruker.dittnav.varsel.bestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateSikkerhetsnivaa

fun createBeskjedEksternVarslingForEvent(beskjedEksternVarsling: Beskjed): Beskjed {
    val build = Beskjed.newBuilder()
            .setTidspunkt(beskjedEksternVarsling.getTidspunkt())
            .setSynligFremTil(beskjedEksternVarsling.getSynligFremTil())
            .setFodselsnummer(validateNonNullFieldMaxLength(beskjedEksternVarsling.getFodselsnummer(), "fodselsnummer", 11))
            .setGrupperingsId(validateNonNullFieldMaxLength(beskjedEksternVarsling.getGrupperingsId(), "grupperingsId", 100))
            .setTekst(validateNonNullFieldMaxLength(beskjedEksternVarsling.getTekst(), "tekst", 300))
            .setLink(validateMaxLength(beskjedEksternVarsling.getLink(), "link", 200))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(beskjedEksternVarsling.getSikkerhetsnivaa()))
    return build.build()
}
