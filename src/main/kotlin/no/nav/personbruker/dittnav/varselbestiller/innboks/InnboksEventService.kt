package no.nav.personbruker.dittnav.varselbestiller.innboks


import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.AbstractVarselbestillerForInternalEvent
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.done.earlycancellation.EarlyCancellationRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer

class InnboksEventService(
    doknotifikasjonProducer: DoknotifikasjonProducer,
    varselbestillingRepository: VarselbestillingRepository,
    earlyCancellationRepository: EarlyCancellationRepository,
    metricsCollector: MetricsCollector
) : AbstractVarselbestillerForInternalEvent<InnboksIntern>(
    doknotifikasjonProducer,
    varselbestillingRepository,
    earlyCancellationRepository,
    metricsCollector,
    Eventtype.INNBOKS_INTERN
) {
    override fun createVarselbestilling(
        key: NokkelIntern,
        event: InnboksIntern,
        doknotifikasjon: Doknotifikasjon
    ) = VarselbestillingTransformer.fromInnboks(key, event, doknotifikasjon)

    override fun createDoknotifikasjon(key: NokkelIntern, event: InnboksIntern) =
        DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(key, event)

    override fun hasEksternVarsling(event: InnboksIntern) = event.getEksternVarsling()
}
