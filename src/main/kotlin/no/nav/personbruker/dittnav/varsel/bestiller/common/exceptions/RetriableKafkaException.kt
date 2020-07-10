package no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions

class RetriableKafkaException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}