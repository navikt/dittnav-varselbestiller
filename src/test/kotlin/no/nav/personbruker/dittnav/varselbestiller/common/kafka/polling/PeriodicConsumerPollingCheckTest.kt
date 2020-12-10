package no.nav.personbruker.dittnav.varselbestiller.common.kafka.polling

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.config.*
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PeriodicConsumerPollingCheckTest {

    private val appContext = mockk<ApplicationContext>(relaxed = true)
    private val periodicConsumerPollingCheck = PeriodicConsumerPollingCheck(appContext)

    @BeforeEach
    fun resetMocks() {
        mockkObject(KafkaConsumerSetup)
        coEvery { KafkaConsumerSetup.restartPolling(appContext) } returns Unit
        coEvery { KafkaConsumerSetup.stopAllKafkaConsumers(appContext) } returns Unit
        coEvery { appContext.reinitializeConsumers() } returns Unit
        coEvery { KafkaConsumerSetup.startAllKafkaPollers(appContext) } returns Unit
        staticMocks()
    }

    fun staticMocks() {
        mockkStatic("no.nav.personbruker.dittnav.varselbestiller.config.EnvironmentKt")
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `Skal returnere en liste med konsumenter som har stoppet aa polle`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = false)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size `should be equal to` 2
        }
    }

    @Test
    fun `Skal returnere en tom liste hvis alle konsumenter kjorer som normalt`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = false, isDoneConsumerStopped = false, isOppgaveConsumerStopped = false)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true)


        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().`should be empty`()
        }
    }

    @Test
    fun `Skal kalle paa restartPolling hvis en eller flere konsumere har sluttet aa kjore`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = false, isOppgaveConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true)


        runBlocking {
            periodicConsumerPollingCheck.checkIfConsumersAreRunningAndRestartIfTheyShouldRun()
        }

        coVerify(exactly = 1) { KafkaConsumerSetup.restartPolling(appContext) }
        confirmVerified(KafkaConsumerSetup)
    }

    @Test
    fun `Skal ikke restarte polling hvis alle konsumere kjorer`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = false, isDoneConsumerStopped = false, isOppgaveConsumerStopped = false)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true)

        runBlocking {
            periodicConsumerPollingCheck.checkIfConsumersAreRunningAndRestartIfTheyShouldRun()
        }

        coVerify(exactly = 0) { KafkaConsumerSetup.restartPolling(appContext) }
        confirmVerified(KafkaConsumerSetup)
    }

    @Test
    fun `Skal kun restarte polling av beskjed-consumer hvis kun polling paa beskjed er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = false, shouldOppgavePoll = false)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size `should be equal to` 1
        }
    }

    @Test
    fun `Skal kun restarte polling av done-consumer hvis kun polling paa Done er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = false, shouldDonePoll = true, shouldOppgavePoll = false)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size `should be equal to` 1
        }
    }

    @Test
    fun `Skal kun restarte polling av oppgave-consumer hvis kun polling paa Oppgave er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = false, shouldDonePoll = false, shouldOppgavePoll = true)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size `should be equal to` 1
        }
    }

    private fun createResponsConsumerIsStopped(isBeskjedConsumerStopped: Boolean,
                                               isOppgaveConsumerStopped: Boolean,
                                               isDoneConsumerStopped: Boolean) {
        coEvery { appContext.beskjedConsumer.isStopped() } returns isBeskjedConsumerStopped
        coEvery { appContext.doneConsumer.isStopped() } returns isDoneConsumerStopped
        coEvery { appContext.oppgaveConsumer.isStopped() } returns isOppgaveConsumerStopped
    }

    private fun createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll: Boolean,
                                                                  shouldDonePoll: Boolean,
                                                                  shouldOppgavePoll: Boolean) {
        coEvery { shouldPollBeskjedToDoknotifikasjon() } returns shouldBeskjedPoll
        coEvery { shouldPollDoneToDoknotifikasjonStopp() } returns shouldDonePoll
        coEvery { shouldPollOppgaveToDoknotifikasjon() } returns shouldOppgavePoll
    }

}