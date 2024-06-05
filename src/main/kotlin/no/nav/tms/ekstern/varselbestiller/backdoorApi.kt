package no.nav.tms.ekstern.varselbestiller

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.tms.common.observability.traceVarsel
import no.nav.tms.ekstern.varselbestiller.config.MetricsCollector
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.Varsel

fun Route.backdoorApi(
    doknotifikasjonProducer: DoknotEventProducer<Doknotifikasjon>
) {
    val objectMapper = jacksonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    post("backdoor") {
        val event = call.receiveText().let { objectMapper.readTree(it) }

        val varsel: Varsel = objectMapper.treeToValue(event)

        traceVarsel(varsel.varselId, mapOf("action" to "opprettet", "initiated_by" to "admin")) {
            doknotifikasjonProducer.sendEvent(varsel.varselId, DoknotifikasjonCreator.createDoknotifikasjonFromVarsel(varsel))
            MetricsCollector.eksternVarslingBestilt(varsel.type, varsel.eksternVarslingBestilling.prefererteKanaler)
        }
    }
}
