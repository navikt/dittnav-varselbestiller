package no.nav.tms.ekstern.varselbestiller

import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.tms.ekstern.varselbestiller.config.Environment
import no.nav.tms.ekstern.varselbestiller.config.Kafka
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.DoknotEventProducer
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselInaktivertSink
import no.nav.tms.ekstern.varselbestiller.doknotifikasjon.VarselOpprettetSubscriber
import no.nav.tms.kafka.application.KafkaApplication
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

    startKafkaApplication(
        environment = environment,
        doknotifikasjonProducer = doknotifikasjonProducer,
        doknotifikasjonStoppProducer = doknotifikasjonStoppProducer
    )
}

fun startKafkaApplication(
    environment: Environment,
    doknotifikasjonProducer: DoknotEventProducer<Doknotifikasjon>,
    doknotifikasjonStoppProducer: DoknotEventProducer<DoknotifikasjonStopp>
) = KafkaApplication.build {
    kafkaConfig {
        groupId=environment.groupId
        readTopic(environment.kafkaTopic)
    }
    subscribers(VarselOpprettetSubscriber(doknotifikasjonProducer),VarselInaktivertSink(doknotifikasjonStoppProducer))
    onShutdown {
        doknotifikasjonProducer.flushAndClose()
        doknotifikasjonStoppProducer.flushAndClose()
    }
}