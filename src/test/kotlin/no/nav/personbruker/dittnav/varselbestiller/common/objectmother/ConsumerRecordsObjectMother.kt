package no.nav.personbruker.dittnav.varselbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.done.AvroDoneInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveInternObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    fun <T> giveMeConsumerRecordsWithThisConsumerRecord(concreteRecord: ConsumerRecord<NokkelIntern, T>): ConsumerRecords<NokkelIntern, T> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, T>>>()
        records[TopicPartition(concreteRecord.topic(), 1)] = listOf(concreteRecord)
        return ConsumerRecords(records)
    }

    fun <T> giveMeConsumerRecordsWithThisConsumerRecord(concreteRecords: List<ConsumerRecord<NokkelIntern, T>>): ConsumerRecords<NokkelIntern, T> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, T>>>()
        records[TopicPartition(concreteRecords[0].topic(), 1)] = concreteRecords
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfBeskjedRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<NokkelIntern, BeskjedIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, BeskjedIntern>>>()
        val recordsForSingleTopic = createBeskjedRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<NokkelIntern, BeskjedIntern>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelIntern, BeskjedIntern>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroBeskjedInternObjectMother.createBeskjedWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(i)

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    fun <T> createConsumerRecord(topicName: String, actualEvent: T): ConsumerRecord<NokkelIntern, T> {
        val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(1)
        return ConsumerRecord(topicName, 1, 0, nokkel, actualEvent)
    }

    fun <K, V> createConsumerRecordWithKey(topicName: String, actualKey: K?, actualEvent: V): ConsumerRecord<K, V> {
        return ConsumerRecord(topicName, 1, 0, actualKey, actualEvent)
    }

    fun giveMeANumberOfDoneRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<NokkelIntern, DoneIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, DoneIntern>>>()
        val recordsForSingleTopic = createDoneRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createDoneRecords(topicName: String, totalNumber: Int): List<ConsumerRecord<NokkelIntern, DoneIntern>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelIntern, DoneIntern>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroDoneInternObjectMother.createDoneIntern()
            val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    fun giveMeANumberOfOppgaveRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<NokkelIntern, OppgaveIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, OppgaveIntern>>>()
        val recordsForSingleTopic = createOppgaveRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createOppgaveRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<NokkelIntern, OppgaveIntern>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelIntern, OppgaveIntern>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroOppgaveInternObjectMother.createOppgaveWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }
}
