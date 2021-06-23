package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.conflictingKeysEvents
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingObjectMother.giveMeANumberOfVarselbestilling
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

internal class EventMetricsSessionTest {

    private val systembruker = "dummySystembruker"

    @Test
    fun `Skal returnere antall sendte duplikat`() {
        val numberOfDuplicates = 2
        val session = EventMetricsSession(Eventtype.BESKJED)
        val conflictingKeysResult = conflictingKeysEvents(giveMeANumberOfVarselbestilling(numberOfDuplicates))

        session.countDuplicateKeyEksternvarslingBySystemUser(conflictingKeysResult)

        session.getEksternvarslingDuplicateKeys(systembruker) `should be` numberOfDuplicates
    }

    @Test
    fun `Skal returnere 0 hvis ingen duplikat er sendt`() {
        val numberOfEvents = 2
        val session = EventMetricsSession(Eventtype.BESKJED)
        val result = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents))

        session.countDuplicateKeyEksternvarslingBySystemUser(result)

        session.getEksternvarslingDuplicateKeys(systembruker) `should be` 0
    }

    @Test
    fun `Skal fortsatt telle event hvis nokkel er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED)

        session.countNokkelWasNull()

        session.getAllEventsFromKafka() `should be` 1
        session.getNokkelWasNull() `should be` 1
    }

    @Test
    fun `Skal telle rett antall totale events fra Kafka`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countNokkelWasNull()
        session.countAllEventsFromKafkaForSystemUser(systemUser)

        session.getAllEventsFromKafka() `should be` 2
        session.getAllEventsFromKafka(systemUser) `should be` 1
    }
}
