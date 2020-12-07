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

        session.countDuplicateEventKeysBySystemUser(conflictingKeysResult)

        session.getDuplicateKeyEvents(systembruker) `should be` numberOfDuplicates
    }

    @Test
    fun `Skal returnere 0 hvis ingen duplikat er sendt`() {
        val numberOfEvents = 2
        val session = EventMetricsSession(Eventtype.BESKJED)
        val result = successfulEvents(giveMeANumberOfVarselbestilling(numberOfEvents))

        session.countDuplicateEventKeysBySystemUser(result)

        session.getDuplicateKeyEvents(systembruker) `should be` 0
    }

    @Test
    fun `Skal plusse paa én numberOfAllEvents når nokkel er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUserThatIsNull = "NokkelIsNullNoProducerSpecified"

        session.countFailedEventForSystemUser(systemUserThatIsNull)

        session.getAllEvents() `should be` 1
        session.getEventsFailed() `should be` 1
    }

    @Test
    fun `Skal ikke plusse paa numberOfAllEvents hvis nokkel ikke er null`() {
        val session = EventMetricsSession(Eventtype.BESKJED)
        val systemUser = "dummySystemUser"

        session.countFailedEventForSystemUser(systemUser)

        session.getAllEvents() `should be` 0
        session.getEventsFailed() `should be` 1
    }
}