package no.nav.personbruker.dittnav.varselbestiller.varsel


fun varselAktivertJsonWithNullableFields(
    type: VarselType,
    eventId: String,
    eksternVarsling: Boolean = true
) = varselOpprettetJson(
    type = type,
    varselId = eventId,
    eksternVarsling = eksternVarsling,
    prefererteKanaler = "[]",
    smsVarslingstekst = null,
    epostVarslingstekst = null,
    epostVarslingstittel = null
)

fun varselOpprettetJson(
    type: VarselType,
    varselId: String,
    eksternVarsling: Boolean = true,
    prefererteKanaler: String? = """["EPOST", "SMS"]""",
    smsVarslingstekst: String? = "smstekst",
    epostVarslingstekst: String? = "eposttekst",
    epostVarslingstittel: String? = "eposttittel"
) = """{
        "@event_name": "opprettet",
        "type": "${type.name.lowercase()}",
        "produsent": {
            "cluster": "cluster",
            "namespace": "ns",
            "appnavn": "app"
        },
        "varselId": "$varselId",
        "opprettet": "2022-02-01T00:00:00Z",
        "ident": "12345678910",
        "innhold": {
            "tekster": [
                {
                    "tekst": "Norsk tekst",
                    "spraakkode": "nb",
                    "default": false
                },
                {
                    "tekst": "Default text",
                    "spraakkode": "en",
                    "default": true
                }
            ],
            "link": "url"
        },
        "sensitivitet": "high",
        "aktivFremTil": "2022-04-01T00:00:00Z"
        ${
            if(eksternVarsling) {
                ""","eksternVarslingBestilling": {
                    "prefererteKanaler": $prefererteKanaler,
                    "smsVarslingstekst": ${smsVarslingstekst?.let { "\"$smsVarslingstekst\"" }},
                    "epostVarslingstekst": ${epostVarslingstekst?.let { "\"$epostVarslingstekst\"" }},
                    "epostVarslingstittel": ${epostVarslingstittel?.let { "\"$epostVarslingstittel\"" }}
                }
                """
            }
            else ""
        }
        
    }""".trimIndent()
