package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.conflictingKeysEvents
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother.giveMeANumberOfVarselbestilling
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

internal class EventMetricsSessionTest {

    private val namespace = "dummyNamespace"
    private val appnavn = "dummyAppnavn"

    @Test
    fun `Skal returnere antall sendte duplikat`() {
        val numberOfDuplicates = 2
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)
        val conflictingKeysResult = conflictingKeysEvents(giveMeANumberOfVarselbestilling(numberOfDuplicates))

        session.countDuplicateKeyEksternvarslingByProducer(conflictingKeysResult)

        session.getEksternvarslingDuplicateKeys(namespace, appnavn) `should be` numberOfDuplicates
    }

    @Test
    fun `Skal returnere 0 hvis ingen duplikat er sendt`() {
        val numberOfEvents = 2
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)
        val result = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents))

        session.countDuplicateKeyEksternvarslingByProducer(result)

        session.getEksternvarslingDuplicateKeys(namespace, appnavn) `should be` 0
    }

    @Test
    fun `Skal fortsatt telle event hvis nokkel er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)

        session.countNokkelWasNull()

        session.getAllEventsFromKafka() `should be` 1
        session.getNokkelWasNull() `should be` 1
    }

    @Test
    fun `Skal telle rett antall totale events fra Kafka`() {
        val session = EventMetricsSession(Eventtype.BESKJED_INTERN)

        session.countNokkelWasNull()
        session.countAllEventsFromKafkaForProducer(namespace, appnavn)

        session.getAllEventsFromKafka() `should be` 2
        session.getAllEventsFromKafka(namespace, appnavn) `should be` 1
    }
}
