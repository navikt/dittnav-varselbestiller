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

enum class VarselType(val emailTitle: String, val smsText: String, emailTextFile: String) {
    Oppgave(
        emailTitle = "Du har fått en oppgave fra NAV",
        smsText = "Hei! Du har fått en ny oppgave fra NAV. Logg inn på NAV for å se hva oppgaven gjelder. Vennlig hilsen NAV",
        emailTextFile = "epost_oppgave.txt",
    ),
    Beskjed(
        emailTitle = "Beskjed fra NAV",
        smsText = "Hei! Du har fått en ny beskjed fra NAV. Logg inn på NAV for å se hva beskjeden gjelder. Vennlig hilsen NAV",
        emailTextFile = "epost_beskjed.txt",
    ),
    Innboks(
        emailTitle = "Du har fått en melding fra NAV",
        smsText = "Hei! Du har fått en ny melding fra NAV. Logg inn på NAV for å lese meldingen. Vennlig hilsen NAV",
        emailTextFile = "epost_innboks.txt",
    );

    val emailText = this::class.java.getResource("/texts/$emailTextFile")!!.readText(Charsets.UTF_8)

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
