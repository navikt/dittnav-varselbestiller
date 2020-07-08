package no.nav.personbruker.dittnav.varsel.bestiller.metrics.influx

import org.influxdb.dto.Point

data class SensuEvent(
        val dataPoint: Point,
        val name: String = "dittnav-varsel-bestiller-kafka-events"
) {
    fun toJson(): String {
        return """
            {
                "name": "$name",
                "type": "metric",
                "handlers": [ "events_nano" ],
                "status": 0,
                "output": "${dataPoint.lineProtocol()}"
            }
        """.trimIndent()
    }

    fun toCompactJson(): String {
        return toJson().replace("\r", " ").replace("\n", " ").replace("\\s+".toRegex(), " ")
    }
}