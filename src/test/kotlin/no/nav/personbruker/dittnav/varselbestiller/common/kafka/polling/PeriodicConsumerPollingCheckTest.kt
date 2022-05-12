package no.nav.personbruker.dittnav.varselbestiller.common.kafka.polling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.varselbestiller.config.ApplicationContext
import no.nav.personbruker.dittnav.varselbestiller.config.KafkaConsumerSetup
import no.nav.personbruker.dittnav.varselbestiller.config.shouldPollBeskjedToDoknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.config.shouldPollDoneToDoknotifikasjonStopp
import no.nav.personbruker.dittnav.varselbestiller.config.shouldPollInnboksToDoknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.config.shouldPollOppgaveToDoknotifikasjon
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
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = false, isInnboksConsumerStopped = false)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true, shouldInnboksPoll = true)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size shouldBe 2
        }
    }

    @Test
    fun `Skal returnere en tom liste hvis alle konsumenter kjorer som normalt`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = false, isDoneConsumerStopped = false, isOppgaveConsumerStopped = false, isInnboksConsumerStopped = false)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true, shouldInnboksPoll = true)


        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().shouldBeEmpty()
        }
    }

    @Test
    fun `Skal kalle paa restartPolling hvis en eller flere konsumere har sluttet aa kjore`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = false, isOppgaveConsumerStopped = true, isInnboksConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true, shouldInnboksPoll = true)


        runBlocking {
            periodicConsumerPollingCheck.checkIfConsumersAreRunningAndRestartIfTheyShouldRun()
        }

        coVerify(exactly = 1) { KafkaConsumerSetup.restartPolling(appContext) }
        confirmVerified(KafkaConsumerSetup)
    }

    @Test
    fun `Skal ikke restarte polling hvis alle konsumere kjorer`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = false, isDoneConsumerStopped = false, isOppgaveConsumerStopped = false, isInnboksConsumerStopped = false)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = true, shouldOppgavePoll = true, shouldInnboksPoll = true)

        runBlocking {
            periodicConsumerPollingCheck.checkIfConsumersAreRunningAndRestartIfTheyShouldRun()
        }

        coVerify(exactly = 0) { KafkaConsumerSetup.restartPolling(appContext) }
        confirmVerified(KafkaConsumerSetup)
    }

    @Test
    fun `Skal kun restarte polling av beskjed-consumer hvis kun polling paa beskjed er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true, isInnboksConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = true, shouldDonePoll = false, shouldOppgavePoll = false, shouldInnboksPoll = false)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size shouldBe 1
        }
    }

    @Test
    fun `Skal kun restarte polling av done-consumer hvis kun polling paa Done er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true, isInnboksConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = false, shouldDonePoll = true, shouldOppgavePoll = false, shouldInnboksPoll = false)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size shouldBe 1
        }
    }

    @Test
    fun `Skal kun restarte polling av oppgave-consumer hvis kun polling paa Oppgave er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true, isInnboksConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = false, shouldDonePoll = false, shouldOppgavePoll = true, shouldInnboksPoll = false)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size shouldBe 1
        }
    }

    @Test
    fun `Skal kun restarte polling av innboks-consumer hvis kun polling paa Innboks er satt til true`() {
        createResponsConsumerIsStopped(isBeskjedConsumerStopped = true, isDoneConsumerStopped = true, isOppgaveConsumerStopped = true, isInnboksConsumerStopped = true)
        createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll = false, shouldDonePoll = false, shouldOppgavePoll = false, shouldInnboksPoll = true)

        runBlocking {
            periodicConsumerPollingCheck.getConsumersThatShouldBeRestarted().size shouldBe 1
        }
    }

    private fun createResponsConsumerIsStopped(isBeskjedConsumerStopped: Boolean,
                                               isOppgaveConsumerStopped: Boolean,
                                               isInnboksConsumerStopped: Boolean,
                                               isDoneConsumerStopped: Boolean) {
        coEvery { appContext.beskjedConsumer.isStopped() } returns isBeskjedConsumerStopped
        coEvery { appContext.oppgaveConsumer.isStopped() } returns isOppgaveConsumerStopped
        coEvery { appContext.innboksConsumer.isStopped() } returns isInnboksConsumerStopped
        coEvery { appContext.doneConsumer.isStopped() } returns isDoneConsumerStopped

    }

    private fun createResponseShouldConsumerPollToDoknotifikasjon(shouldBeskjedPoll: Boolean,
                                                                  shouldOppgavePoll: Boolean,
                                                                  shouldInnboksPoll: Boolean,
                                                                  shouldDonePoll: Boolean) {
        coEvery { shouldPollBeskjedToDoknotifikasjon() } returns shouldBeskjedPoll
        coEvery { shouldPollOppgaveToDoknotifikasjon() } returns shouldOppgavePoll
        coEvery { shouldPollInnboksToDoknotifikasjon() } returns shouldInnboksPoll
        coEvery { shouldPollDoneToDoknotifikasjonStopp() } returns shouldDonePoll
    }
}
