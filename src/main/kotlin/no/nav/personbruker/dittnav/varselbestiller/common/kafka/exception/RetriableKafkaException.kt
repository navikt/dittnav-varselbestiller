package no.nav.personbruker.dittnav.varselbestiller.common.kafka.exception

import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.AbstractPersonbrukerException

class RetriableKafkaException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}
