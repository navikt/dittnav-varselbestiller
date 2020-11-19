package no.nav.personbruker.dittnav.varselbestiller.config

enum class Eventtype(val eventType: String) {
    OPPGAVE("oppgave"),
    BESKJED("beskjed"),
    DONE("done"),
    DOKNOTIFIKASJON("doknotifikasjon"),
    DOKNOTIFIKASJON_STOPP("doknotifikasjon-stopp")
}
