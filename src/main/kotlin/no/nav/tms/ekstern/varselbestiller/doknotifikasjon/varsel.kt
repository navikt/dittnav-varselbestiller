package no.nav.tms.ekstern.varselbestiller.doknotifikasjon

import com.fasterxml.jackson.annotation.JsonValue

data class Varsel(
    val type: VarselType,
    val varselId: String,
    val ident: String,
    val produsent: Produsent,
    val sensitivitet: Sensitivitet,
    val eksternVarslingBestilling: EksternVarslingBestilling,
)

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
