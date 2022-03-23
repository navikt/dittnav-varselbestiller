package no.nav.personbruker.dittnav.varselbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.doknotifikasjon.schemas.Doknotifikasjon
import no.nav.personbruker.dittnav.varselbestiller.common.AbstractVarselbestillerForInternalEvent
import no.nav.personbruker.dittnav.varselbestiller.config.Eventtype
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonProducer
import no.nav.personbruker.dittnav.varselbestiller.done.earlydone.EarlyDoneEventRepository
import no.nav.personbruker.dittnav.varselbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingRepository
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.VarselbestillingTransformer

class OppgaveEventService(
    doknotifikasjonProducer: DoknotifikasjonProducer,
    varselbestillingRepository: VarselbestillingRepository,
    earlyDoneEventRepository: EarlyDoneEventRepository,
    metricsCollector: MetricsCollector
) : AbstractVarselbestillerForInternalEvent<OppgaveIntern>(
    doknotifikasjonProducer,
    varselbestillingRepository,
    earlyDoneEventRepository,
    metricsCollector,
    Eventtype.OPPGAVE_INTERN
) {
    override fun createVarselbestilling(
        key: NokkelIntern, event: OppgaveIntern, doknotifikasjon: Doknotifikasjon
    ) = VarselbestillingTransformer.fromOppgave(key, event, doknotifikasjon)

    override fun createDoknotifikasjon(key: NokkelIntern, event: OppgaveIntern) =
        DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(key, event)

    override fun hasEksternVarsling(event: OppgaveIntern) = event.getEksternVarsling()

}
