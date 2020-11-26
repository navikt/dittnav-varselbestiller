package no.nav.personbruker.dittnav.varselbestiller.metrics

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.config.buildJsonSerializer
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.net.URL

internal class ProducerNameResolverTest {

    private val systembruker = "x-dittnav"
    private val produsent = "dittnav"

    @Test
    fun `Skal hente produsentnavn alias`() {
        val client = getClient {
            respond(
                    produsent,
                    headers = headersOf(HttpHeaders.ContentType,
                            ContentType.Application.Json.toString())
            )
        }

        val producerNameResolver = ProducerNameResolver(client, URL("http://event-handler"))

        runBlocking {
            val producerNameAlias = producerNameResolver.getProducerNameAlias(systembruker)
            producerNameAlias `should be equal to` produsent
        }
    }

    @Test
    fun `skal returnere tom String hvis produsentnavn ikke ble funnet i handler`() {
        val client = getClient {
            respond(
                    "",
                    headers = headersOf(HttpHeaders.ContentType,
                            ContentType.Application.Json.toString())
            )
        }

        val producerNameResolver = ProducerNameResolver(client, URL("http://event-handler"))

        runBlocking {
            val producerNameAlias = producerNameResolver.getProducerNameAlias(systembruker)
            producerNameAlias `should be equal to` ""
        }
    }

//TODO hvordan teste coVerify?
    /*
       @Test
       fun `skal returnere cachede produsentnavn hvis henting av nye feiler`() {
           val client = getClient {
               respond(
                       produsent,
                       headers = headersOf(HttpHeaders.ContentType,
                               ContentType.Application.Json.toString())
               )
           }

           val producerNameResolver = ProducerNameResolver(client, URL("http://event-handler"))

           runBlocking {
               producerNameResolver.getProducerNameAlias(systembruker)
               producerNameResolver.getProducerNameAlias(systembruker)
               coVerify(exactly = 1) { client.get(any(), any()) }

           }
       }

     */

    private fun getClient(respond: MockRequestHandleScope.() -> HttpResponseData): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond()
                }
            }
            install(JsonFeature) {
                serializer = buildJsonSerializer()
            }
        }
    }

}