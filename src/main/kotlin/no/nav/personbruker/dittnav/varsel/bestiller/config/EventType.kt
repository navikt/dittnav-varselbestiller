package no.nav.personbruker.dittnav.varsel.bestiller.config

enum class EventType(val eventType: String) {
    OPPGAVE("oppgave"),
    BESKJED("beskjed"),
    DONE("done"),
    DOKNOTIFIKASJON("doknotifikasjon"),
    DOKNOTIFIKASJON_STOPP("doknotifikasjon-stopp")
}
