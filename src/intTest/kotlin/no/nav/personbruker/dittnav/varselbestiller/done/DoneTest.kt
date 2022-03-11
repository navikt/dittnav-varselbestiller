package no.nav.personbruker.dittnav.varselbestiller.done

import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStopp
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.varselbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.varselbestiller.common.kafka.delayUntilCommittedOffset
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjonStopp.DoknotifikasjonStoppProducer
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.createVarselbestillinger
import org.amshove.kluent.shouldBe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test

class DoneTest {
    private val database = LocalPostgresDatabase()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val doneTopicPartition = TopicPartition("done", 0)
    private val doneConsumerMock = MockConsumer<NokkelIntern, DoneIntern>(OffsetResetStrategy.EARLIEST).also {
        it.subscribe(listOf(doneTopicPartition.topic()))
        it.rebalance(listOf(doneTopicPartition))
        it.updateBeginningOffsets(mapOf(doneTopicPartition to 0))
    }

    private val doknotifikasjonStopProducerMock = MockProducer(
        false,
        { _: String, _: String -> ByteArray(0) }, //Dummy serializers
        { _: String, _: DoknotifikasjonStopp -> ByteArray(0) }
    )
    private val kafkaProducerWrapper =
        KafkaProducerWrapper(KafkaTestTopics.doknotifikasjonStopTopicName, doknotifikasjonStopProducerMock)

    private val doknotifikasjonRepository = VarselbestillingRepository(database)
    private val doknotifikasjonStopProducer = DoknotifikasjonStoppProducer(kafkaProducerWrapper, doknotifikasjonRepository)
    private val eventService = DoneEventService(doknotifikasjonStopProducer, doknotifikasjonRepository, metricsCollector)
    private val consumer = Consumer(KafkaTestTopics.doneTopicName, doneConsumerMock, eventService)

    private val varselbestillinger = listOf(
        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-1", eventId = "1"),
        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-2", eventId = "2"),
        VarselbestillingObjectMother.createVarselbestillingWithBestillingsIdAndEventId(bestillingsId = "B-test-3", eventId = "3"))

    @Test
    fun `Leser done og avbestiller varseler`() {
        doknotifikasjonStopProducerMock.initTransactions()

        createVarselbestillingerInDb()

        val doneEventer = (1..10).associate {
            AvroNokkelInternObjectMother.createNokkelInternWithEventId(it) to AvroDoneInternObjectMother.createDoneIntern()
        }

        doneEventer.entries.forEachIndexed { index, entry ->
            doneConsumerMock.addRecord(
                ConsumerRecord(
                    doneTopicPartition.topic(),
                    doneTopicPartition.partition(),
                    index.toLong(),
                    entry.key,
                    entry.value
                )
            )
        }

        runBlocking {
            consumer.startPolling()
            delayUntilCommittedOffset(doneConsumerMock, doneTopicPartition, doneEventer.size.toLong())
            consumer.stopPolling()
        }

        val doknotifikasjonStopEventer = doknotifikasjonStopProducerMock.history()
        doknotifikasjonStopEventer.size shouldBe varselbestillinger.size
    }

    private fun createVarselbestillingerInDb() {
        runBlocking {
            database.dbQuery {
                createVarselbestillinger(varselbestillinger)
            }
        }
    }
}