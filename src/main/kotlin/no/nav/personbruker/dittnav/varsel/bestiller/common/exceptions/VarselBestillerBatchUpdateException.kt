package no.nav.personbruker.dittnav.varsel.bestiller.common.exceptions

class VarselBestillerBatchUpdateException(message: String, cause: Throwable?) : RetriableDatabaseException(message, cause) {
    constructor(message: String) : this(message, null)
}
