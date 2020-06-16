package no.nav.personbruker.dittnav.varsel.bestiller.common.database

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BrukernotifikasjonProducer<T>() {

    private val log: Logger = LoggerFactory.getLogger(BrukernotifikasjonProducer::class.java)

    fun sendToKafka(list: List<T>) {

    }

}
