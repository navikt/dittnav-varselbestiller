package no.nav.personbruker.dittnav.varselbestiller.common.kafka.polling

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import no.nav.personbruker.dittnav.varselbestiller.config.*
import no.nav.personbruker.dittnav.varselbestiller.health.HealthStatus
import no.nav.personbruker.dittnav.varselbestiller.health.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class PeriodicConsumerPollingCheck(
        private val appContext: ApplicationContext,
        private val job: Job = Job()) : CoroutineScope {

    private val log: Logger = LoggerFactory.getLogger(PeriodicConsumerPollingCheck::class.java)
    private val minutesToWait = Duration.ofMinutes(30)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun start() {
        log.info("Periodisk sjekking av at konsumerne kjører har blitt aktivert, første sjekk skjer om $minutesToWait minutter.")
        launch {
            while (job.isActive) {
                delay(minutesToWait)
                checkIfConsumersAreRunningAndRestartIfTheyShouldRun()
            }
        }
    }

    suspend fun checkIfConsumersAreRunningAndRestartIfTheyShouldRun() {
        val consumersThatShouldBeRestarted = getConsumersThatShouldBeRestarted()
        if (consumersThatShouldBeRestarted.isNotEmpty()) {
            restartPolling(consumersThatShouldBeRestarted)
        }
    }

    fun getConsumersThatShouldBeRestarted(): MutableList<Eventtype> {
        val consumersThatShouldBeRestarted = mutableListOf<Eventtype>()

        if (shouldPollBeskjedToDoknotifikasjon() and appContext.beskjedConsumer.isStopped()) {
            consumersThatShouldBeRestarted.add(Eventtype.BESKJED)
        }
        if (shouldPollDoneToDoknotifikasjonStopp() and appContext.doneConsumer.isStopped()) {
            consumersThatShouldBeRestarted.add(Eventtype.DONE)
        }
        if (shouldPollOppgaveToDoknotifikasjon() and appContext.oppgaveConsumer.isStopped()) {
            consumersThatShouldBeRestarted.add(Eventtype.OPPGAVE)
        }
        return consumersThatShouldBeRestarted
    }

    suspend fun restartPolling(consumers: MutableList<Eventtype>) {
        log.warn("Følgende konsumere hadde stoppet ${consumers}, de(n) vil bli restartet.")
        KafkaConsumerSetup.restartPolling(appContext)
        log.info("$consumers konsumern(e) har blitt restartet.")
    }

    suspend fun stop() {
        log.info("Stopper periodisk sjekking av at konsumerne kjører.")
        job.cancelAndJoin()
    }

    fun isCompleted(): Boolean {
        return job.isCompleted
    }

    fun status(): HealthStatus {
        return when (job.isActive) {
            true -> HealthStatus("PeriodicConsumerPollingCheck", Status.OK, "Checker is running", false)
            false -> HealthStatus("PeriodicConsumerPollingCheck", Status.ERROR, "Checker is not running", false)
        }
    }

}