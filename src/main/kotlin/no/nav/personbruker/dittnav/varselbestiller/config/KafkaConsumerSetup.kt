package no.nav.personbruker.dittnav.varselbestiller.config

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.varselbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object KafkaConsumerSetup {

    private val log: Logger = LoggerFactory.getLogger(KafkaConsumerSetup::class.java)

    fun startAllKafkaPollers(appContext: ApplicationContext) {
        if(shouldPollBeskjedToDoknotifikasjon()) {
            appContext.beskjedConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av beskjed til doknotifikasjon.")
        }

        if(shouldPollOppgaveToDoknotifikasjon()) {
            appContext.oppgaveConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av oppgave til doknotifikasjon.")
        }

        if(shouldPollDoneToDoknotifikasjonStopp()) {
            appContext.doneConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av done til doknotifikasjon-stopp.")
        }
    }

    suspend fun stopAllKafkaConsumers(appContext: ApplicationContext) {
        log.info("Begynner å stoppe kafka-pollerne...")
        if(shouldPollBeskjedToDoknotifikasjon()) {
            appContext.beskjedConsumer.stopPolling()
        }

        if(shouldPollOppgaveToDoknotifikasjon()) {
            appContext.oppgaveConsumer.stopPolling()
        }

        if(shouldPollDoneToDoknotifikasjonStopp()) {
            appContext.doneConsumer.stopPolling()
        }
        log.info("...ferdig med å stoppe kafka-pollerne.")
    }

    fun <T> setupKafkaConsumer(topicName:String, kafkaProps: Properties, eventProcessor: EventBatchProcessorService<NokkelIntern, T>): Consumer<NokkelIntern, T> {
        val kafkaConsumer = KafkaConsumer<NokkelIntern, T>(kafkaProps)
        return Consumer(topicName, kafkaConsumer, eventProcessor)
    }

    suspend fun restartPolling(appContext: ApplicationContext) {
        stopAllKafkaConsumers(appContext)
        appContext.reinitializeConsumers()
        startAllKafkaPollers(appContext)
    }
}
