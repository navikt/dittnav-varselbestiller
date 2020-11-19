package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class Doknotifikasjon(
        val bestillingsid: String,
        val eventtype: Eventtype,
        val systembruker: String,
        val eventtidspunkt: ZonedDateTime
)
