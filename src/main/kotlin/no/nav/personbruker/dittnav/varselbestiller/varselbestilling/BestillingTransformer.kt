package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.personbruker.dittnav.varselbestiller.doknotifikasjon.DoknotifikasjonCreator

object BestillingTransformer {
    fun transformAndWrapEvent(nokkel: NokkelIntern, beskjed: BeskjedIntern): BestillingWrapper {
        val varselbestilling = VarselbestillingTransformer.fromBeskjed(nokkel, beskjed)
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromBeskjed(nokkel, beskjed)

        return BestillingWrapper(varselbestilling, doknotifikasjon)
    }

    fun transformAndWrapEvent(nokkel: NokkelIntern, oppgave: OppgaveIntern): BestillingWrapper {
        val varselbestilling = VarselbestillingTransformer.fromOppgave(nokkel, oppgave)
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromOppgave(nokkel, oppgave)

        return BestillingWrapper(varselbestilling, doknotifikasjon)
    }

    fun transformAndWrapEvent(nokkel: NokkelIntern, innboks: InnboksIntern): BestillingWrapper {
        val varselbestilling = VarselbestillingTransformer.fromInnboks(nokkel, innboks)
        val doknotifikasjon = DoknotifikasjonCreator.createDoknotifikasjonFromInnboks(nokkel, innboks)

        return BestillingWrapper(varselbestilling, doknotifikasjon)
    }
}
