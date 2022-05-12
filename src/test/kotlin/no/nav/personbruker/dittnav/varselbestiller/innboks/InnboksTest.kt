package no.nav.personbruker.dittnav.varselbestiller.innboks

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
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

class InnboksTest {
    private val database = LocalPostgresDatabase.cleanDb()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val innboksTopicPartition = TopicPartition("innboks", 0)
    private val innboksConsumerMock = MockConsumer<NokkelIntern, InnboksIntern>(OffsetResetStrategy.EARLIEST).also {
        it.subscribe(listOf(innboksTopicPartition.topic()))
        it.rebalance(listOf(innboksTopicPartition))
        it.updateBeginningOffsets(mapOf(innboksTopicPartition to 0))
    }

    private val doknotifikasjonProducerMock = MockProducer(
        false,
        { _: String, _: String -> ByteArray(0) }, //Dummy serializers
        { _: String, _: Doknotifikasjon -> ByteArray(0) }
    )
    private val kafkaProducerWrapper =
        KafkaProducerWrapper(KafkaTestTopics.doknotifikasjonTopicName, doknotifikasjonProducerMock)

    private val doknotifikasjonRepository = VarselbestillingRepository(database)
    private val doknotifikasjonProducer = DoknotifikasjonProducer(kafkaProducerWrapper, doknotifikasjonRepository)
    private val eventService = InnboksEventService(doknotifikasjonProducer, doknotifikasjonRepository, metricsCollector)
    private val consumer = Consumer(KafkaTestTopics.innboksTopicName, innboksConsumerMock, eventService)

    @Test
    fun `Leser innbokser og sender varsel`() {
        doknotifikasjonProducerMock.initTransactions()

        val innbokser = createEventRecords(
            10,
            innboksTopicPartition,
            AvroInnboksInternObjectMother::createInnboksIntern
        )

        innbokser.forEach { innboksConsumerMock.addRecord(it) }

        runBlocking {
            consumer.startPolling()
            delayUntilCommittedOffset(innboksConsumerMock, innboksTopicPartition, innbokser.size.toLong())
            consumer.stopPolling()
        }

        val doknotifikasjoner = doknotifikasjonProducerMock.history()
        doknotifikasjoner.size shouldBe innbokser.size
    }
}