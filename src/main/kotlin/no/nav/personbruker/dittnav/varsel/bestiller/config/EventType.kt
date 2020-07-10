package no.nav.personbruker.dittnav.varsel.bestiller.config

enum class EventType(val eventType: String) {
    OPPGAVE("oppgave"),
    BESKJED("beskjed"),
    INNBOKS("innboks"),
    DONE("done"),
    BESKJED_EKSTERN_VARSLING("beskjed_ekstern_varsling"),
    OPPGAVE_EKSTERN_VARSLING("oppgave_ekstern_varsling"),
    INNBOKS_EKSTERN_VARSLING("innboks_ekstern_varsling"),
    INNBOKS_EKSTERN_DONE("done_ekstern_varsling"),
}
