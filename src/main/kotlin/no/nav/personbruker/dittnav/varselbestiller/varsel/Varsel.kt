package no.nav.personbruker.dittnav.varselbestiller.varsel

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import java.time.LocalDateTime
import java.time.ZoneId

data class Varsel(
    val type: VarselType,
    val varselId: String,
    val ident: String,
    val produsent: Produsent,
    val sensitivitet: Sensitivitet,
    val eksternVarslingBestilling: EksternVarslingBestilling,
) {

    fun toVarselBestilling() = Varselbestilling(
        bestillingsId = varselId,
        eventId = varselId,
        fodselsnummer = ident,
        systembruker = "",
        namespace = produsent.namespace,
        appnavn = produsent.appnavn,
        bestillingstidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
        prefererteKanaler = eksternVarslingBestilling.prefererteKanaler,
    )
}

data class Produsent(
    val namespace: String,
    val appnavn: String
)

enum class VarselType {
    Oppgave,
    Beskjed,
    Innboks;

    @JsonValue
    fun lowercaseName() = name.lowercase()
}

data class EksternVarslingBestilling(
    val prefererteKanaler: List<String> = emptyList(),
    val smsVarslingstekst: String? = null,
    val epostVarslingstekst: String? = null,
    val epostVarslingstittel: String? = null,
)

enum class Sensitivitet(val loginLevel: Int) {
    Substantial(3),
    High(4);

    @JsonValue
    fun toJson() = name.lowercase()
}
