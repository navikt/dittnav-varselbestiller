package no.nav.personbruker.dittnav.varselbestiller.config

enum class Eventtype(val eventtype: String) {
    BESKJED("beskjed"),
    OPPGAVE("oppgave"),
    INNBOKS("innboks"),
    DONE("done"),
    DOKNOTIFIKASJON("doknotifikasjon"),
    DOKNOTIFIKASJON_STOPP("doknotifikasjon-stopp")
}
