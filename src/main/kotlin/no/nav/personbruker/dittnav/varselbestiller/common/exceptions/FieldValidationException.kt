package no.nav.personbruker.dittnav.varselbestiller.common.exceptions

class FieldValidationException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}
