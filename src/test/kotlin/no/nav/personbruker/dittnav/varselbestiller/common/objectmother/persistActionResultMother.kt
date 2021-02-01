package no.nav.personbruker.dittnav.varselbestiller.common.objectmother

import no.nav.personbruker.dittnav.varselbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.varselbestiller.common.database.PersistFailureReason


fun <T> successfulEvents(events: List<T>): ListPersistActionResult<T> {
    return events.map { event ->
        event to PersistFailureReason.NO_ERROR
    }.let { entryList ->
        ListPersistActionResult.mapListOfIndividualResults(entryList)
    }
}

fun <T> conflictingKeysEvents(events: List<T>): ListPersistActionResult<T> {
    return events.map { event ->
        event to PersistFailureReason.CONFLICTING_KEYS
    }.let { entryList ->
        ListPersistActionResult.mapListOfIndividualResults(entryList)
    }
}
