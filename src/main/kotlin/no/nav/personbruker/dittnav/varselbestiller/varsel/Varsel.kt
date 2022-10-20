package no.nav.personbruker.dittnav.varselbestiller.varsel

import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import java.time.LocalDateTime
import java.time.ZoneId

data class Varsel(
    val varselType: VarselType,
    val namespace: String,
    val appnavn: String,
    val eventId: String,
    val fodselsnummer: String,
    val sikkerhetsnivaa: Int,
    val eksternVarsling: Boolean,
    val prefererteKanaler: List<String> = emptyList(),
    val smsVarslingstekst: String?,
    val epostVarslingstekst: String?,
    val epostVarslingstittel: String?
) {

    fun toVarselBestilling() = Varselbestilling(
        bestillingsId = eventId,
        eventId = eventId,
        fodselsnummer = fodselsnummer,
        systembruker = "",
        namespace = namespace,
        appnavn = appnavn,
        bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
        prefererteKanaler = prefererteKanaler,
    )
}