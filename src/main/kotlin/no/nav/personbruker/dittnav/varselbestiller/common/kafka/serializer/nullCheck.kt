package no.nav.personbruker.dittnav.varselbestiller.common.kafka.serializer

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.varselbestiller.common.exceptions.NokkelNullException
import org.apache.kafka.clients.consumer.ConsumerRecord

fun <T> ConsumerRecord<Nokkel, T>.getNonNullKey(): Nokkel {
    return key() ?: throw NokkelNullException("Produsenten har ikke spesifisert en kafka-key for sitt event")
}
