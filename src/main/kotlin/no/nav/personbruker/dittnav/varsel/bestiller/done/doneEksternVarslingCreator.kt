package no.nav.personbruker.dittnav.varsel.bestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateFodselsnummer
import no.nav.personbruker.dittnav.varsel.bestiller.common.validation.validateNonNullFieldMaxLength

fun createDoneEksternVarslingForEvent(done: Done): Done {
    val build = Done.newBuilder()
            .setFodselsnummer(validateFodselsnummer(done.getFodselsnummer()))
            .setTidspunkt(done.getTidspunkt())
            .setGrupperingsId(validateNonNullFieldMaxLength(done.getGrupperingsId(), "grupperingsId", 100))
    return build.build()
}