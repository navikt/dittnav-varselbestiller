package no.nav.personbruker.dittnav.varselbestiller.varsel

import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.personbruker.dittnav.varselbestiller.common.eventMdc
import no.nav.personbruker.dittnav.varselbestiller.common.logAvbestilling
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import observability.traceVarsel

class InaktivertSink(
    rapidsConnection: RapidsConnection,
    private val doknotifikasjonStoppProducer: DoknotifikasjonStoppProducer,
    private val varselbestillingRepository: VarselbestillingRepository
) :
    River.PacketListener {
    private val log = KotlinLogging.logger { }

    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "inaktivert")
                it.requireKey("varselId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["varselId"].textValue()
        val eventName = packet["@event_name"].textValue()
        traceVarsel(id = eventId, extra = mapOf("inaktivert".eventMdc)) {
            log.info { "Mottok inaktivertmelding" }
            runBlocking {
                varselbestillingRepository.getVarselbestillingIfExists(eventId)?.let { existingVarselbestilling ->
                    if (!existingVarselbestilling.avbestilt) {
                        doknotifikasjonStoppProducer.sendDoknotifikasjonStoppAndPersistCancellation(
                            createDoknotifikasjonStopp(existingVarselbestilling)
                        )
                        RapidMetrics.eksternVarslingStoppet(eventName)
                    }
                    log.logAvbestilling(existingVarselbestilling)
                } ?: log.info { "Fant ikke varsel" }
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.debug { problems.toString() }
    }
}

fun createDoknotifikasjonStopp(varselbestilling: Varselbestilling): DoknotifikasjonStopp {
    val doknotifikasjonStoppBuilder = DoknotifikasjonStopp.newBuilder()
        .setBestillingsId(varselbestilling.bestillingsId)
        .setBestillerId(varselbestilling.appnavn)
    return doknotifikasjonStoppBuilder.build()
}
