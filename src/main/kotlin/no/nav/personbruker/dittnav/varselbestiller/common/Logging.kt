package no.nav.personbruker.dittnav.varselbestiller.common

import kotlinx.coroutines.runBlocking
import observability.traceVarsel

fun traceVarselAsync(id: String, extra: Map<String, String>, function: suspend () -> Any?) {
    traceVarsel(id = id, extra = extra) {
        runBlocking {
            function()
        }
    }
}
