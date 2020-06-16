package no.nav.personbruker.dittnav.varsel.bestiller.done

import io.mockk.mockk
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsProbe
import no.nav.personbruker.dittnav.varsel.bestiller.metrics.EventMetricsSession

class DoneEventServiceTest {

    private val metricsProbe = mockk<EventMetricsProbe>(relaxed = true)
    private val metricsSession = mockk<EventMetricsSession>(relaxed = true)

}
