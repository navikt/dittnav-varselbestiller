package no.nav.personbruker.dittnav.varselbestiller.common.validation

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Oppgave
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.personbruker.dittnav.varselbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.varselbestiller.nokkel.AvroNokkelObjectMother
import no.nav.personbruker.dittnav.varselbestiller.oppgave.AvroOppgaveObjectMother
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test

internal class ValidationUtilKtTest {

    @Test
    fun `Skal kaste exception hvis link er ugyldig i beskjed-eventet`() {
        val invalidLink = "invalidLink"
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val beskjedWithInvalidLink = AvroBeskjedObjectMother.createBeskjedWithLink(invalidLink)

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjedWithInvalidLink)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis link er ugyldig i oppgave-eventet`() {
        val invalidLink = "invalidLink"
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val oppgaveWithInvalidLink = AvroOppgaveObjectMother.createOppgaveWithLink(invalidLink)

        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = nokkel, actualEvent = oppgaveWithInvalidLink)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfOppgaveOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal haandtere at link er en tom string i beskjed-eventet`() {
        val emptyLink = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val beskjedWithEmptyLink = AvroBeskjedObjectMother.createBeskjedWithLink(emptyLink)

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjedWithEmptyLink)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        }
    }

    @Test
    fun `Skal kaste exception hvis link er en tom String i oppgave-eventet`() {
        val emptyLink = ""
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val oppgaveWithEmptyLink = AvroOppgaveObjectMother.createOppgaveWithLink(emptyLink)

        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = nokkel, actualEvent = oppgaveWithEmptyLink)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfOppgaveOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis tekst er for lang i beskjed-eventet`() {
        val tooLongText = "t".repeat(MAX_LENGTH_TEXT_BESKJED + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithText(tooLongText)

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis tekst er for lang i oppgave-eventet`() {
        val tooLongText = "t".repeat(MAX_LENGTH_TEXT_OPPGAVE + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithText(tooLongText)

        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = nokkel, actualEvent = oppgave)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfOppgaveOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis grupperingsid er for lang i beskjed-eventet`() {
        val tooLongGrupperingsid = "g".repeat(MAX_LENGTH_GRUPPERINGSID + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithGrupperingsid(tooLongGrupperingsid)

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis grupperingsid er for lang i oppgave-eventet`() {
        val tooLongGrupperingsid = "g".repeat(MAX_LENGTH_GRUPPERINGSID + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithGrupperingsid(tooLongGrupperingsid)

        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = nokkel, actualEvent = oppgave)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfOppgaveOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis fodselsnummer er for lang i beskjed-eventet`() {
        val tooLongFnr = "f".repeat(MAX_LENGTH_FODSELSNUMMER + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithFodselsnummer(tooLongFnr)

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis fodselsnummer er for lang i oppgave-eventet`() {
        val tooLongFnr = "f".repeat(MAX_LENGTH_FODSELSNUMMER + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithFodselsnummer(fodselsnummer =  tooLongFnr)

        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = nokkel, actualEvent = oppgave)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfOppgaveOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis sikkerhetsnivaa er for lavt i beskjed-eventet`() {
        val invalidSikkerhetsnivaa = 2
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis sikkerhetsnivaa er for lavt i oppgave-eventet`() {
        val invalidSikkerhetsnivaa = 2
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = 1)
        val oppgave = AvroOppgaveObjectMother.createOppgaveWithSikkerhetsnivaa(invalidSikkerhetsnivaa)

        val cr: ConsumerRecord<Nokkel, Oppgave> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "oppgave", actualKey = nokkel, actualEvent = oppgave)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfOppgaveOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis systembruker er for lang i nokkel-eventet`() {
        val tooLongSystembruker = "s".repeat(MAX_LENGTH_SYSTEMBRUKER + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithSystembruker(systembruker = tooLongSystembruker)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithText("dummmyText")

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

    @Test
    fun `Skal kaste exception hvis eventid er for lang i nokkel-eventet`() {
        val tooLongEventId = "e".repeat(MAX_LENGTH_EVENTID + 1)
        val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(eventId = tooLongEventId)
        val beskjed = AvroBeskjedObjectMother.createBeskjedWithText("dummmyText")

        val cr: ConsumerRecord<Nokkel, Beskjed> = ConsumerRecordsObjectMother.createConsumerRecordWithKey(topicName = "beskjed", actualKey = nokkel, actualEvent = beskjed)
        val records = ConsumerRecordsObjectMother.giveMeConsumerRecordsWithThisConsumerRecord(cr)

        invoking {
            records.forEach { record ->
                throwExceptionIfBeskjedOrNokkelIsNotValid(record.key(), record.value())
            }
        } `should throw` FieldValidationException::class
    }

}
