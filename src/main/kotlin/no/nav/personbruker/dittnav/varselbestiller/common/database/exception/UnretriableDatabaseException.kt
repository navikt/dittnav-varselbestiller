package no.nav.personbruker.dittnav.varselbestiller.common.database.exception

import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.AbstractPersonbrukerException

class UnretriableDatabaseException(message: String, cause: Throwable?) : AbstractPersonbrukerException(message, cause) {
    constructor(message: String) : this(message, null)
}
