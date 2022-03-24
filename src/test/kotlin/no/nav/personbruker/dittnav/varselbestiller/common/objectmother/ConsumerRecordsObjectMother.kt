package no.nav.personbruker.dittnav.varselbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.done.AvroDoneInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.innboks.AvroInnboksInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelInternObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveInternObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    fun <T> giveMeConsumerRecordsWithThisConsumerRecord(concreteRecords: List<ConsumerRecord<NokkelIntern, T>>): ConsumerRecords<NokkelIntern, T> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, T>>>()
        records[TopicPartition(concreteRecords[0].topic(), 1)] = concreteRecords
        return ConsumerRecords(records)
    }

    fun <K, V> createConsumerRecordWithKey(topicName: String, actualKey: K?, actualEvent: V): ConsumerRecord<K, V> {
        return ConsumerRecord(topicName, 1, 0, actualKey, actualEvent)
    }

    fun giveMeANumberOfBeskjedRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<NokkelIntern, BeskjedIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, BeskjedIntern>>>()
        val recordsForSingleTopic = createBeskjedRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfOppgaveRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<NokkelIntern, OppgaveIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, OppgaveIntern>>>()
        val recordsForSingleTopic = createOppgaveRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfInnboksRecords(numberOfRecords: Int, topicName: String, withEksternVarsling: Boolean = false): ConsumerRecords<NokkelIntern, InnboksIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, InnboksIntern>>>()
        val recordsForSingleTopic = createInnboksRecords(topicName, numberOfRecords, withEksternVarsling)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    fun giveMeANumberOfDoneRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<NokkelIntern, DoneIntern> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelIntern, DoneIntern>>>()
        val recordsForSingleTopic = createDoneRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<NokkelIntern, BeskjedIntern>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelIntern, BeskjedIntern>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroBeskjedInternObjectMother.createBeskjedInternWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(i)

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    private fun createOppgaveRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<NokkelIntern, OppgaveIntern>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelIntern, OppgaveIntern>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroOppgaveInternObjectMother.createOppgaveInternWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    private fun createInnboksRecords(topicName: String, totalNumber: Int, withEksternVarsling: Boolean): List<ConsumerRecord<NokkelIntern, InnboksIntern>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelIntern, InnboksIntern>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroInnboksInternObjectMother.createInnboksInternWithEksternVarsling(withEksternVarsling)
            val nokkel = AvroNokkelInternObjectMother.createNokkelInternWithEventId(i)
            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
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
}
