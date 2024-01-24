package no.nav.tms.ekstern.varselbestiller.config

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.InaktivertSink
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselOpprettetSink
import org.apache.kafka.clients.producer.KafkaProducer

fun main() {
    val environment = Environment()

    val doknotifikasjonProducer = DoknotEventProducer(
            topicName = environment.doknotifikasjonTopicName,
            kafkaProducer = KafkaProducer<String, Doknotifikasjon>(Kafka.producerProps(environment))
    )

    val doknotifikasjonStoppProducer = DoknotEventProducer(
            topicName = environment.doknotifikasjonStopTopicName,
            kafkaProducer = KafkaProducer<String, DoknotifikasjonStopp>(Kafka.producerProps(environment))
    )

    startRapid(
        environment = environment,
        doknotifikasjonProducer = doknotifikasjonProducer,
        doknotifikasjonStoppProducer = doknotifikasjonStoppProducer
    )
}

private fun startRapid(
    environment: Environment,
    doknotifikasjonProducer: DoknotEventProducer<Doknotifikasjon>,
    doknotifikasjonStoppProducer: DoknotEventProducer<DoknotifikasjonStopp>
) {
    RapidApplication.create(environment.rapidConfig()).apply {
        VarselOpprettetSink(
            rapidsConnection = this,
            doknotifikasjonProducer = doknotifikasjonProducer,
        )
        InaktivertSink(
            rapidsConnection = this,
            stoppDoknotEventProducer = doknotifikasjonStoppProducer,
        )
    }.apply {
        register(object : RapidsConnection.StatusListener {

            override fun onShutdown(rapidsConnection: RapidsConnection) {
                doknotifikasjonProducer.flushAndClose()
                doknotifikasjonStoppProducer.flushAndClose()
            }
        })
    }.start()
}
