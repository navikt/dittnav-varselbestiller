package no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon

import no.nav.personbruker.dittnav.varselbestiller.config.EventType
import java.time.LocalDateTime

data class Doknotifikasjon(
        val bestillingsid: String,
        val eventType: EventType,
        val systembruker: String,
        val eventtidspunkt: LocalDateTime
)
