package no.nav.personbruker.dittnav.varselbestiller.beskjed

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.createEventRecords
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.delayUntilCommittedOffset
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test

class BeskjedTest {
    private val database = LocalPostgresDatabase.cleanDb()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val beskjedTopicPartition = TopicPartition("beskjed", 0)
    private val beskjedConsumerMock = MockConsumer<NokkelIntern, BeskjedIntern>(OffsetResetStrategy.EARLIEST).also {
        it.subscribe(listOf(beskjedTopicPartition.topic()))
        it.rebalance(listOf(beskjedTopicPartition))
        it.updateBeginningOffsets(mapOf(beskjedTopicPartition to 0))
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
    private val eventService = BeskjedEventService(doknotifikasjonProducer, doknotifikasjonRepository, metricsCollector)
    private val consumer = Consumer(KafkaTestTopics.beskjedTopicName, beskjedConsumerMock, eventService)

    @Test
    fun `Leser beskjeder og sender varsel`() {
        doknotifikasjonProducerMock.initTransactions()

        val beskjeder = createEventRecords(
            10,
            beskjedTopicPartition,
            AvroBeskjedInternObjectMother::createBeskjedIntern
        )

        beskjeder.forEach { beskjedConsumerMock.addRecord(it) }

        runBlocking {
            consumer.startPolling()
            delayUntilCommittedOffset(beskjedConsumerMock, beskjedTopicPartition, beskjeder.size.toLong())
            consumer.stopPolling()
        }

        val doknotifikasjoner = doknotifikasjonProducerMock.history()
        doknotifikasjoner.size shouldBe beskjeder.size
    }
}