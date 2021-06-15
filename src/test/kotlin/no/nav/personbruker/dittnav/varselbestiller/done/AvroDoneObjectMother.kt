package no.nav.personbruker.dittnav.varselbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Instant

object AvroDoneObjectMother {

    fun createDone(eventId: String): Done {
        return Done(
                Instant.now().toEpochMilli(),
                "12345678901",
                "100${eventId}"
        )
    }

    fun createDone(eventId: Int): Done {
        return Done(
                Instant.now().toEpochMilli(),
                "12345678901",
                "100${eventId}"
        )
    }

}
