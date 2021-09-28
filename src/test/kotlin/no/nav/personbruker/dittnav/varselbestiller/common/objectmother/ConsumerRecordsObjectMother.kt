package no.nav.personbruker.dittnav.varselbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.*
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varselbestiller.done.AvroDoneObjectMother
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    fun <T> giveMeConsumerRecordsWithThisConsumerRecord(concreteRecord: ConsumerRecord<Nokkel, T>): ConsumerRecords<Nokkel, T> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, T>>>()
        records[TopicPartition(concreteRecord.topic(), 1)] = listOf(concreteRecord)
        return ConsumerRecords(records)
    }

    fun <T> giveMeConsumerRecordsWithThisConsumerRecord(concreteRecords: List<ConsumerRecord<Nokkel, T>>): ConsumerRecords<Nokkel, T> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, T>>>()
        records[TopicPartition(concreteRecords[0].topic(), 1)] = concreteRecords
        return ConsumerRecords(records)
    }

    fun <T> createConsumerRecord(topicName: String, actualEvent: T): ConsumerRecord<Nokkel, T> {
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(1)
        return ConsumerRecord(topicName, 1, 0, nokkel, actualEvent)
    }

    fun <K, V> createConsumerRecordWithKey(topicName: String, actualKey: K?, actualEvent: V): ConsumerRecord<K, V> {
        return ConsumerRecord(topicName, 1, 0, actualKey, actualEvent)
    }

    fun giveMeANumberOfBeskjedRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<Nokkel, Beskjed> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, Beskjed>>>()
        val recordsForSingleTopic = createBeskjedRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfOppgaveRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<Nokkel, Oppgave> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, Oppgave>>>()
        val recordsForSingleTopic = createOppgaveRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfInnboksRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<Nokkel, Innboks> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, Innboks>>>()
        val recordsForSingleTopic = createInnboksRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfDoneRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<Nokkel, Done> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, Done>>>()
        val recordsForSingleTopic = createDoneRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<Nokkel, Beskjed>> {
        val allRecords = mutableListOf<ConsumerRecord<Nokkel, Beskjed>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroBeskjedObjectMother.createBeskjedWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(i)

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    private fun createOppgaveRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<Nokkel, Oppgave>> {
        val allRecords = mutableListOf<ConsumerRecord<Nokkel, Oppgave>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroOppgaveObjectMother.createOppgaveWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    private fun createInnboksRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<Nokkel, Innboks>> {
        val allRecords = mutableListOf<ConsumerRecord<Nokkel, Innboks>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroInnboksObjectMother.createInnboksWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    private fun createDoneRecords(topicName: String, totalNumber: Int): List<ConsumerRecord<Nokkel, Done>> {
        val allRecords = mutableListOf<ConsumerRecord<Nokkel, Done>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroDoneObjectMother.createDone("$i")
            val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }
}
