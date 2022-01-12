package no.nav.personbruker.dittnav.varselbestiller.config

enum class Eventtype(val eventtype: String) {
    OPPGAVE_INTERN("oppgave"),
    BESKJED_INTERN("beskjed"),
    INNBOKS_INTERN("innboks"),
    DONE_INTERN("done"),
    DOKNOTIFIKASJON("doknotifikasjon"),
    DOKNOTIFIKASJON_STOPP("doknotifikasjon-stopp")
}
