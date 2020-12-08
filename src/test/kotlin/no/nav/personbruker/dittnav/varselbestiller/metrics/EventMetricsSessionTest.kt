package no.nav.personbruker.dittnav.varselbestiller.metrics

import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.conflictingKeysEvents
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.giveMeANumberOfVarselbestilling
import no.nav.personbruker.dittnav.varselbestiller.common.objectmother.successfulEvents
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

internal class EventMetricsSessionTest {

    private val systembruker = "x-dittnav"

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
    fun `Skal plusse paa én numberOfAllEvents når nokkel er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUserThatIsNull = "NokkelIsNullNoProducerSpecified"

        session.countFailedEksternvarslingForSystemUser(systemUserThatIsNull)

        session.getAllEventsFromKafka() `should be` 1
        session.getEksternvarslingEventsFailed() `should be` 1
    }

    @Test
    fun `Skal ikke plusse paa numberOfAllEvents hvis nokkel ikke er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countFailedEksternvarslingForSystemUser(systemUser)

        session.getAllEventsFromKafka() `should be` 0
        session.getEksternvarslingEventsFailed() `should be` 1
    }
}