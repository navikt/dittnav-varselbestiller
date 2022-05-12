package no.nav.personbruker.dittnav.varselbestiller.metrics

import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.conflictingKeysEvents
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother.giveMeANumberOfVarselbestilling
import org.junit.jupiter.api.Test

internal class EventMetricsSessionTest {

    val producer = Producer("dummyNamespace", "dummyAppnavn")

    @Test
    fun `Skal returnere antall sendte duplikat`() {
        val numberOfDuplicates = 2
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)
        val conflictingKeysResult = conflictingKeysEvents(giveMeANumberOfVarselbestilling(numberOfDuplicates))

        session.countDuplicateKeyEksternvarslingByProducer(conflictingKeysResult)

        session.getEksternvarslingDuplicateKeys(producer) shouldBe numberOfDuplicates
    }

    @Test
    fun `Skal returnere 0 hvis ingen duplikat er sendt`() {
        val numberOfEvents = 2
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)
        val result = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents))

        session.countDuplicateKeyEksternvarslingByProducer(result)

        session.getEksternvarslingDuplicateKeys(producer) shouldBe 0
    }

    @Test
    fun `Skal fortsatt telle event hvis nokkel er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)

        session.countNokkelWasNull()

        session.getAllEventsFromKafka() shouldBe 1
        session.getNokkelWasNull() shouldBe 1
    }

    @Test
    fun `Skal telle rett antall totale events fra Kafka`() {
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)

        session.countNokkelWasNull()
        session.countAllEventsFromKafkaForProducer(producer)

        session.getAllEventsFromKafka() shouldBe 2
        session.getAllEventsFromKafka(producer) shouldBe 1
    }
}
