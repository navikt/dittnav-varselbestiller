package no.nav.personbruker.dittnav.varsel.bestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateSikkerhetsnivaa

fun createInnboksEksternVarslingForEvent(innboks: Innboks): Innboks {
    val build = Innboks.newBuilder()
            .setFodselsnummer(validateNonNullFieldMaxLength(innboks.getFodselsnummer(), "fodselsnummer", 11))
            .setGrupperingsId(validateNonNullFieldMaxLength(innboks.getGrupperingsId(), "grupperingsId", 100))
            .setLink(validateMaxLength(innboks.getLink(), "link", 200))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(innboks.getSikkerhetsnivaa()))
            .setTekst(validateNonNullFieldMaxLength(innboks.getTekst(), "tekst", 500))
            .setTidspunkt(innboks.getTidspunkt())
    return build.build()
}