package no.nav.personbruker.dittnav.varsel.bestiller.common

import no.nav.brukernotifikasjon.schemas.Nokkel

data class RecordKeyValueWrapper<T>(
        val key: Nokkel,
        val value: T
)