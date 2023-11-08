package no.nav.personbruker.dittnav.varselbestiller.common
import io.github.oshai.kotlinlogging.KLogger
import no.nav.personbruker.dittnav.varselbestiller.varsel.VarselType
import no.nav.personbruker.dittnav.varselbestiller.varselbestilling.Varselbestilling
import org.slf4j.MDC

private const val varseltypeMdcName = "type"
val VarselType.typeMDC
    get() = varseltypeMdcName to this.name

val String.eventMdc
    get() = "event" to this
val String.kildeMdc: Pair<String, String>
    get() = "kilde" to this
fun String?.addKildeToMDC() {
    if (this != null) MDC.put("kilde", this)
}

fun KLogger.logAvbestilling(existingVarselbestilling: Varselbestilling) {
    if (!existingVarselbestilling.avbestilt) {
        info { "Avbestiller varsel" }
    } else {
        info { "Varsel er allerede avbestilt" }
    }
}