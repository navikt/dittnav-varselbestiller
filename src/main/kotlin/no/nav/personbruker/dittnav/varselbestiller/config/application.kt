package no.nav.personbruker.dittnav.varselbestiller.config

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.personbruker.dittnav.varselbestiller.varsel.DoneSink
import no.nav.personbruker.dittnav.varselbestiller.varsel.RapidMetricsProbe
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselSink

fun main() {
    val appContext = ApplicationContext()

    if(appContext.environment.rapidOnly) {
        startRapid(appContext)
    } else {
        embeddedServer(Netty, port = 8080) {
            varselbestillerApi(
                appContext
            )
        }.start(wait = true)
    }
}

private fun startRapid(appContext: ApplicationContext) {
    val rapidMetricsProbe = RapidMetricsProbe(appContext.resolveMetricsReporter(appContext.environment))
    RapidApplication.create(appContext.environment.rapidConfig() + mapOf("HTTP_PORT" to "8080")).apply {
        VarselSink(
            rapidsConnection = this,
            doknotifikasjonProducer = appContext.doknotifikasjonProducer,
            varselbestillingRepository = appContext.doknotifikasjonRepository,
            rapidMetricsProbe = rapidMetricsProbe,
            writeToDb = true
        )
        DoneSink(
            rapidsConnection = this,
            doknotifikasjonStoppProducer = appContext.doknotifikasjonStopProducer,
            varselbestillingRepository = appContext.doknotifikasjonRepository,
            rapidMetricsProbe = rapidMetricsProbe,
            writeToDb = true
        )
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                Flyway.runFlywayMigrations(appContext.environment)
            }
        })
    }.start()
}