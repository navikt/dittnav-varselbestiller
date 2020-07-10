package no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions

class UnretriableKafkaException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}