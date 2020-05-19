package no.nav.personbruker.dittnav.eventaggregator.common.exceptions

class NokkelNullException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}
