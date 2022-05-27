package no.nav.personbruker.dittnav.varselbestiller.varselbestilling

import no.nav.doknotifikasjon.schemas.Doknotifikasjon

data class BestillingWrapper(
    val varselbestilling: Varselbestilling,
    val doknotifikasjon: Doknotifikasjon
)
