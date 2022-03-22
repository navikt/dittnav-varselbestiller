package no.nav.personbruker.dittnav.varselbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.AbstractInternalEventBatchProcessor
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation.EarlyCancellationRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer

class BeskjedEventService(
    doknotifikasjonProducer: DoknotifikasjonProducer,
    varselbestillingRepository: VarselbestillingRepository,
    earlyCancellationRepository: EarlyCancellationRepository,
    metricsCollector: MetricsCollector
) : AbstractInternalEventBatchProcessor<BeskjedIntern>(
    doknotifikasjonProducer,
    varselbestillingRepository,
    earlyCancellationRepository,
    metricsCollector,
    Eventtype.BESKJED_INTERN
) {

    override fun hasEksternVarsling(event: BeskjedIntern) = event.getEksternVarsling()

    override fun createDoknotifikasjon(key: NokkelIntern, event: BeskjedIntern) =
        DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(key, event)

    override fun createVarselbestilling(
        key: NokkelIntern, event: BeskjedIntern, doknotifikasjon: Doknotifikasjon
    ) = VarselbestillingTransformer.fromBeskjed(key, event, doknotifikasjon)
}
