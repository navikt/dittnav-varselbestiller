package no.nav.personbruker.dittnav.varselbestiller.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.*
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.amshove.kluent.shouldBe
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test

class OppgaveTest {
    private val database = LocalPostgresDatabase()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val oppgaveTopicPartition = TopicPartition("oppgave", 0)
    private val oppgaveConsumerMock = MockConsumer<NokkelIntern, OppgaveIntern>(OffsetResetStrategy.EARLIEST).also {
        it.subscribe(listOf(oppgaveTopicPartition.topic()))
        it.rebalance(listOf(oppgaveTopicPartition))
        it.updateBeginningOffsets(mapOf(oppgaveTopicPartition to 0))
    }

    private val doknotifikasjonProducerMock =
        MockProducer(false, { _: String, _: String -> ByteArray(0) }, //Dummy serializers
            { _: String, _: Doknotifikasjon -> ByteArray(0) })
    private val kafkaProducerWrapper =
        KafkaProducerWrapper(KafkaTestTopics.doknotifikasjonTopicName, doknotifikasjonProducerMock)

    private val doknotifikasjonRepository = VarselbestillingRepository(database)
    private val doknotifikasjonProducer = DoknotifikasjonProducer(kafkaProducerWrapper, doknotifikasjonRepository)
    private val eventService = OppgaveEventService(doknotifikasjonProducer, doknotifikasjonRepository, metricsCollector)
    private val consumer = Consumer(KafkaTestTopics.oppgaveTopicName, oppgaveConsumerMock, eventService)

    @Test
    fun `Leser oppgaver og sender varsel`() {
        doknotifikasjonProducerMock.initTransactions()

        val oppgaver = createEventRecords(
            10, oppgaveTopicPartition, AvroOppgaveInternObjectMother::createOppgaveIntern
        )

        oppgaver.forEach { oppgaveConsumerMock.addRecord(it) }

        runBlocking {
            consumer.startPolling()
            delayUntilCommittedOffset(oppgaveConsumerMock, oppgaveTopicPartition, oppgaver.size.toLong())
            consumer.stopPolling()
        }

        val doknotifikasjoner = doknotifikasjonProducerMock.history()
        doknotifikasjoner.size shouldBe oppgaver.size
    }
}