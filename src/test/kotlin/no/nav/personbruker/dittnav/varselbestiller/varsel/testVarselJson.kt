package no.nav.personbruker.dittnav.varselbestiller.varsel


fun varselJsonWithNullableFields(
    type: VarselType,
    eventId: String,
    eksternVarsling: Boolean = true
) = varselJson(
    type = type,
    eventId = eventId,
    eksternVarsling = eksternVarsling,
    prefererteKanaler = null,
    smsVarslingstekst = null,
    epostVarslingstekst = null,
    epostVarslingstittel = null
)

fun varselJson(
    type: VarselType,
    eventId: String,
    eksternVarsling: Boolean = true,
    prefererteKanaler: String? = """["EPOST", "SMS"]""",
    smsVarslingstekst: String? = "smstekst",
    epostVarslingstekst: String? = "eposttekst",
    epostVarslingstittel: String? = "eposttittel"
) = """{
        "@event_name": "${type.name.lowercase()}",
        "namespace": "ns",
        "appnavn": "app",
        "eventId": "$eventId",
        "forstBehandlet": "2022-02-01T00:00:00",
        "fodselsnummer": "12345678910",
        "tekst": "Tekst",
        "link": "url",
        "sikkerhetsnivaa": 4,
        "synligFremTil": "2022-04-01T00:00:00",
        "aktiv": true,
        "eksternVarsling": $eksternVarsling,
        "prefererteKanaler": $prefererteKanaler,
        "smsVarslingstekst": ${smsVarslingstekst?.let { "\"$smsVarslingstekst\"" }},
        "epostVarslingstekst": ${epostVarslingstekst?.let { "\"$epostVarslingstekst\"" }},
        "epostVarslingstittel": ${epostVarslingstittel?.let { "\"$epostVarslingstittel\"" }}
    }""".trimIndent()