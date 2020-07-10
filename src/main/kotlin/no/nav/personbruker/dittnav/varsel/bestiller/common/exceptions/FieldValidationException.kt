package no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions

open class FieldValidationException (message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}