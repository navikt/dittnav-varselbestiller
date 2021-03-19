package no.nav.personbruker.dittnav.varselbestiller.common

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*

fun getClient(producerNameAlias: String): HttpClient {
    return HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                if (request.url.encodedPath.contains("/producer/alias") && request.url.host.contains("event-handler")) {
                    respond(producerNameAlias, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
                } else {
                    respondError(HttpStatusCode.BadRequest)
                }
            }
        }
        install(JsonFeature)
    }
}
